/*
 * Copyright 2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spinnaker.kork.jedis.recipe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.kork.jedis.RedisClientDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static com.netflix.spinnaker.kork.jedis.recipe.RedisDistributedLock.LockStatus.*;

/**
 * Simple distributed lock recipe that supports both Redis & Dynomite.
 *
 * When a lock is acquired, it's expected that the owner of the lock will continue to
 * send heartbeats before the end of its lease duration to continue extending the lock
 * as long as it needs. Upon completing work, a client can either stop sending heartbeats,
 * or explicitly release the lock. If a release is not called, the lock will expire at
 * the end of lease.
 *
 * Each operation will take one or two version numbers: The Previous Record Version (PRV)
 * and the New Record Version (NRV). If a PRV is required, the action will only succeed
 * if the PRV matches the version number stored with the lock in Redis. When an action
 * succeeds, the version number inside of Redis will be updated to the NRV.
 *
 * If a client has a lock and heartbeats, but the heartbeat fails due to a transfer
 * of ownership, the client must stop performing work against the locked resource(s).
 * This behavior is not enforced by this lock class.
 *
 * TODO rz - total lock duration timer metric (somehow? lock expiration is db-side)
 */
public class RedisDistributedLock {

  /**
   * Returns the active lock, whether or not the desired lock was acquired.
   *
   * ARGS
   * 1: leaseDurationMillis
   * 2: owner
   * 3: ownerSystemTimestamp
   * 4: newRecordVersion
   */
  private static String ACQUIRE_SCRIPT = "" +
    "local payload = cjson.encode({" +
    "  ['leaseDurationMillis']=ARGS[1]," +
    "  ['owner']=ARGS[2]," +
    "  ['ownerSystemTimestamp']=ARGS[3]," +
    "  ['version']=ARGS[4]," +
    "})" +
    "if redis.call('SET', KEYS[1], payload, 'NX', 'EX', ARGS[1]) == 'OK' then" +
    "  return payload" +
    "end" +
    "return redis.call('GET', KEYS[1])";

  /**
   * Returns 1 if heartbeat was successful, -1 if the lock no longer exists,
   * 0 if the lock is now owned by someone else or is a different version.
   *
   * If the heartbeat is successful, update the lock with the NRV and updated
   * owner system timestamp.
   *
   * ARGS
   * 1: owner
   * 2: previousRecordVersion
   * 3: newRecordVersion
   * 4: updatedOwnerSystemTimestamp
   */
  private static String HEARTBEAT_SCRIPT = "" +
    "if redis.call('EXISTS', KEYS[1]) == 1 then" +
    "  local payload = redis.call('GET', KEYS[1])" +
    "  local lock = cjson.decode(payload)" +
    "  if lock['owner'] == ARGS[1] and lock['version'] == ARGS[2] then" +
    "    lock['version'] = ARGS[3]" +
    "    lock['ownerSystemTimestamp'] = ARGS[4]" +
    "    redis.call('PSETEX', KEYS[1], lock['leaseDurationMillis'], cjson.encode(lock))" +
    "    return 1" +
    "  end" +
    "  return 0" +
    "end" +
    "return -1";

  /**
   * Returns 1 if the release is successful, 0 if the release could not be
   * completed (no longer the owner, different version), 2 if the lock no
   * longer exists.
   *
   * ARGS
   * 1: owner
   * 2: previousRecordVersion
   * 3: newRecordVersion
   */
  private static String RELEASE_SCRIPT = "" +
    "if redis.call('EXISTS', KEYS[1]) == 1 then" +
    "  local payload = redis.call('GET', KEYS[1])" +
    "  local lock = cjson.decode(record)" +
    "  if payload['owner'] == ARGS[1] and payload['version'] == ARGS[2] then" +
    "    redis.call('DEL', KEYS[1])" +
    "    return 1" +
    "  end" +
    "  return 0" +
    "end" +
    "return 2";

  private final Logger log = LoggerFactory.getLogger(RedisDistributedLock.class);

  private final String ownerName;

  private final RedisClientDelegate client;
  private final ObjectMapper objectMapper;
  private final Registry registry;
  private final Clock clock;

  private final Id acquireId;
  private final Id heartbeatId;
  private final Id releaseId;

  public RedisDistributedLock(RedisClientDelegate client, ObjectMapper objectMapper, Registry registry, Clock clock) {
    this(getOwnerName(), client, objectMapper, registry, clock);
  }

  public RedisDistributedLock(String ownerName,
                              RedisClientDelegate client,
                              ObjectMapper objectMapper,
                              Registry registry,
                              Clock clock) {
    this.ownerName = ownerName;
    this.client = client;
    this.objectMapper = objectMapper;
    this.registry = registry;
    this.clock = clock;

    acquireId = registry.createId("redis.distributedLock.acquire");
    heartbeatId = registry.createId("redis.distributedLock.heartbeat");
    releaseId = registry.createId("redis.distributedLock.release");
  }

  /**
   * Does not need to use a PVN, as the lock will only be acquired if the key does
   * not exist in Redis. Only the new version is necessary.
   */
  AcquireLockResponse acquire(String name, Duration lease, String version) {
    String normalizedName = name.toLowerCase();

    Lock lock = new Lock(normalizedName, lease.toMillis(), ownerName, clock.millis(), version);

    AcquireLockResponse response = client.withScriptingClient(c -> {
      Object payload = c.eval(
        ACQUIRE_SCRIPT,
        Collections.singletonList(lockKey(normalizedName)),
        Arrays.asList(
          Long.toString(lock.leaseDurationMillis),
          lock.ownerName,
          Long.toString(lock.ownerSystemTimestamp),
          lock.recordVersion
        )
      );

      Lock authoritativeLock;
      try {
        authoritativeLock = objectMapper.readValue(payload.toString(), Lock.class);
      } catch (IOException e) {
        // In this case, client implementations should assume that the lock
        // is taken, not theirs, and to retry at the maximum lease duration.
        return new AcquireLockResponse(ERROR, unknownLockPlaceholder(lock), e);
      }
      return new AcquireLockResponse(getAcquireStatus(authoritativeLock, lock), authoritativeLock);
    });

    switch (response.status) {
      case ACQUIRED:
        log.info("Acquired lock " + lockVersion(response.lock.name, response.lock.recordVersion));
        break;
      case TAKEN:
        log.info(
          "Could not acquire lock {} (owner: {}, version: {}, durationMillis: {})",
          response.lock.name,
          response.lock.ownerName,
          response.lock.recordVersion,
          response.lock.leaseDurationMillis
        );
        break;
      default:
      case ERROR:
        log.error("An unexpected error occurred acquiring lock " + normalizedName, response.reason);
    }

    registry.counter(
      acquireId
        .withTag("lockName", normalizedName)
        .withTag("status", response.status.toString())
    ).increment();

    return response;
  }

  /**
   * A heartbeat will only be accepted if the provided previousVersion matches the
   * version stored in Redis. If a heartbeat is accepted, the newVersion value will
   * be stored with the lock.
   */
  boolean heartbeat(String name, String previousVersion, String newVersion) {
    String normalizedName = name.toLowerCase();

    return client.withScriptingClient(c -> {
      Integer result = Integer.valueOf(c.eval(
        HEARTBEAT_SCRIPT,
        Collections.singletonList(lockKey(normalizedName)),
        Arrays.asList(
          ownerName,
          previousVersion,
          newVersion,
          Long.toString(clock.millis())
        )
      ).toString());

      Id lockHeartbeat = heartbeatId.withTag("lockName", normalizedName);

      switch (result) {
        case 1:
          log.info("Refreshed lease for lock " + lockVersion(normalizedName, newVersion));
          registry.counter(lockHeartbeat.withTag("status", "SUCCESS")).increment();
          return true;
        case 0:
          log.warn("Failed refreshing lock " + normalizedName + ": Not owner");
          registry.counter(lockHeartbeat.withTag("status", "FAILED_NOT_OWNER")).increment();
          return false;
        case -1:
          log.warn("Failed refreshing lock " + normalizedName + ": Lock is gone");
          registry.counter(lockHeartbeat.withTag("status", "FAILED_GONE")).increment();
          return false;
        default:
          log.error("Unknown heartbeat response code " + result + " for lock " + normalizedName);
          registry.counter(lockHeartbeat.withTag("status", "ERROR")).increment();
          return false;
      }
    });
  }

  boolean release(String name, String previousVersion) {
    String normalizedName = name.toLowerCase();

    return client.withScriptingClient(c -> {
      Integer result = Integer.valueOf(c.eval(
        RELEASE_SCRIPT,
        Collections.singletonList(lockKey(normalizedName)),
        Arrays.asList(
          ownerName,
          previousVersion
        )
      ).toString());

      Id lockRelease = releaseId.withTag("lockName", normalizedName);

      switch (result) {
        case 1:
          log.info("Released lock " + lockVersion(normalizedName, previousVersion));
          registry.counter(lockRelease.withTag("status", "SUCCESS")).increment();
          return true;
        case 2:
          log.info("Released lock " + lockVersion(normalizedName, previousVersion));
          registry.counter(lockRelease.withTag("status", "SUCCESS_GONE")).increment();
          return true;
        case 0:
          log.warn("Failed releasing lock " + normalizedName + ": Not owner");
          registry.counter(lockRelease.withTag("status", "FAILED_NOT_OWNER")).increment();
          return false;
        default:
          log.error("Unknown release response code " + result + " for lock " + normalizedName);
          registry.counter(lockRelease.withTag("status", "ERROR")).increment();
          return false;
      }
    });
  }

  private LockStatus getAcquireStatus(Lock authoritative, Lock attempted) {
    return (authoritative.ownerName.equals(attempted.ownerName) &&
      authoritative.recordVersion.equals(attempted.recordVersion)) ? ACQUIRED : TAKEN;
  }

  private Lock unknownLockPlaceholder(Lock lock) {
    return new Lock(lock.name, lock.leaseDurationMillis, "unknown", clock.millis(), "unknown");
  }

  private static String lockKey(String normalizedName) {
    return "{korkLock:" + normalizedName + "}";
  }

  private static String lockVersion(String normalizedName, String version) {
    return normalizedName + "@" + version;
  }

  public enum LockStatus {
    ACQUIRED,
    TAKEN,
    ERROR
  }

  public static class Lock {
    public final String name;
    public final long leaseDurationMillis;
    public final String ownerName;
    public final long ownerSystemTimestamp;
    public final String recordVersion;

    public Lock(String name,
                long leaseDurationMillis,
                String ownerName,
                long ownerSystemTimestamp,
                String versionNumber) {
      this.name = name.toLowerCase();
      this.leaseDurationMillis = leaseDurationMillis;
      this.ownerName = ownerName;
      this.ownerSystemTimestamp = ownerSystemTimestamp;
      this.recordVersion = versionNumber;
    }
  }

  public static class AcquireLockResponse {
    public final LockStatus status;
    public final Lock lock;
    public final Exception reason;

    public AcquireLockResponse(LockStatus status, Lock lock) {
      this(status, lock, null);
    }

    public AcquireLockResponse(LockStatus status, Lock lock, Exception reason) {
      this.status = status;
      this.lock = lock;
      this.reason = reason;
    }

    public long nextRetry() {
      return lock.ownerSystemTimestamp + lock.leaseDurationMillis;
    }
  }

  /**
   * Used only if an ownerName is not provided in the constructor.
   */
  private static String getOwnerName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return NAME_FALLBACK;
    }
  }

  private static final String NAME_FALLBACK = UUID.randomUUID().toString();
}
