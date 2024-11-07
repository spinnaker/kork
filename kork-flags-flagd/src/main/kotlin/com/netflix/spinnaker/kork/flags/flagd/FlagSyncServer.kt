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

import io.grpc.Server
import io.grpc.netty.NettyServerBuilder
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerDomainSocketChannel
import io.netty.channel.unix.DomainSocketAddress
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Bean to handle the lifecycle of the flag sync service in a gRPC server.
 */
class FlagSyncServer(properties: FlagdProperties, service: FlagSyncService) : InitializingBean, DisposableBean {
  private val server: Server

  init {
    val builder = properties.sync.socketPath?.let {
      NettyServerBuilder.forAddress(DomainSocketAddress(it.toString()))
        .channelType(EpollServerDomainSocketChannel::class.java)
        .bossEventLoopGroup(EpollEventLoopGroup())
        .workerEventLoopGroup(EpollEventLoopGroup())
    } ?: NettyServerBuilder.forAddress(InetSocketAddress(InetAddress.getLoopbackAddress(), properties.sync.port))
    server = builder.addService(service).build()
  }

  override fun afterPropertiesSet() {
    server.start()
  }

  override fun destroy() {
    server.shutdown()
  }
}
