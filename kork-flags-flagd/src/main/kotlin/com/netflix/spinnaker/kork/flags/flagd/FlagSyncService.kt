/*
 * Copyright 2024 Apple, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.spinnaker.kork.flags.flagd

import com.google.protobuf.Struct
import dev.openfeature.flagd.grpc.sync.FlagSyncServiceGrpc.FlagSyncServiceImplBase
import dev.openfeature.flagd.grpc.sync.Sync.FetchAllFlagsRequest
import dev.openfeature.flagd.grpc.sync.Sync.FetchAllFlagsResponse
import dev.openfeature.flagd.grpc.sync.Sync.GetMetadataRequest
import dev.openfeature.flagd.grpc.sync.Sync.GetMetadataResponse
import dev.openfeature.flagd.grpc.sync.Sync.SyncFlagsRequest
import dev.openfeature.flagd.grpc.sync.Sync.SyncFlagsResponse
import io.grpc.stub.StreamObserver
import org.springframework.beans.factory.DisposableBean
import org.springframework.context.ApplicationListener
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * In-process implementation of the flag sync service. Flagd clients connect to this to keep track of
 * flag data updates. Flag synchronization modules should publish a [SyncFlagsEvent] to notify observers
 * of a flag configuration sync.
 */
class FlagSyncService : FlagSyncServiceImplBase(), DisposableBean, ApplicationListener<SyncFlagsEvent> {
  private val syncFlagsObservers = CopyOnWriteArrayList<StreamObserver<SyncFlagsResponse>>()
  private val currentFlagConfiguration = AtomicReference<String>()

  override fun onApplicationEvent(event: SyncFlagsEvent) {
    val flagConfiguration = event.flagConfiguration
    currentFlagConfiguration.set(flagConfiguration)
    val response = SyncFlagsResponse.newBuilder().setFlagConfiguration(flagConfiguration).build()
    syncFlagsObservers.forEach { it.onNext(response) }
  }

  override fun syncFlags(request: SyncFlagsRequest, responseObserver: StreamObserver<SyncFlagsResponse>) {
    val flagConfiguration = currentFlagConfiguration.get()
    if (flagConfiguration != null) {
      // start with cached config
      val response = SyncFlagsResponse.newBuilder().setFlagConfiguration(flagConfiguration).build()
      responseObserver.onNext(response)
    }
    syncFlagsObservers += responseObserver
  }

  override fun fetchAllFlags(
    request: FetchAllFlagsRequest,
    responseObserver: StreamObserver<FetchAllFlagsResponse>
  ) {
    val response = FetchAllFlagsResponse.newBuilder()
      .setFlagConfiguration(currentFlagConfiguration.get())
      .build()
    responseObserver.onNext(response)
    responseObserver.onCompleted()
  }

  override fun getMetadata(
    request: GetMetadataRequest,
    responseObserver: StreamObserver<GetMetadataResponse>
  ) {
    // no relevant metadata at the moment
    val metadata = Struct.getDefaultInstance()
    val response = GetMetadataResponse.newBuilder()
      .setMetadata(metadata)
      .build()
    responseObserver.onNext(response)
    responseObserver.onCompleted()
  }

  override fun destroy() {
    syncFlagsObservers.forEach { it.onCompleted() }
    syncFlagsObservers.clear()
  }
}
