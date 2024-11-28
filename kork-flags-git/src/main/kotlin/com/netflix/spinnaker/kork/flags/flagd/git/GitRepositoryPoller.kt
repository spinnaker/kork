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

package com.netflix.spinnaker.kork.flags.flagd.git

import com.netflix.spinnaker.kork.flags.flagd.SyncFlagsEvent
import org.apache.logging.log4j.kotlin.Logging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import java.io.BufferedReader
import java.nio.file.Files
import kotlin.io.path.bufferedReader
import kotlin.io.path.div

open class GitRepositoryPoller(
  private val properties: GitFlagsProperties,
  private val applicationEventPublisher: ApplicationEventPublisher,
) : InitializingBean, DisposableBean {
  private val directory = Files.createTempDirectory("GitRepositoryPoller")
  private lateinit var git: Git

  override fun afterPropertiesSet() {
    git = Git.cloneRepository()
      .setURI(properties.repositoryUri)
      .setBranch(properties.branch)
      .setDepth(1)
      .setDirectory(directory.toFile())
      .setCredentialsProvider(UsernamePasswordCredentialsProvider("token", properties.apiToken))
      .call()
    syncFlags()
  }

  @Async
  @Scheduled(fixedRateString = "\${flags.providers.flagd.git.polling-interval:PT5M}")
  open fun checkForUpdates() {
    val result = git.pull().setFastForward(MergeCommand.FastForwardMode.FF_ONLY).call()
    if (result.isSuccessful) {
      when (result.mergeResult.mergeStatus) {
        MergeResult.MergeStatus.ALREADY_UP_TO_DATE -> return
        MergeResult.MergeStatus.FAST_FORWARD -> syncFlags()
        else -> logger.warn { "Unsuspected merge status: ${result.mergeResult.mergeStatus}" }
      }
    } else {
      // TODO: may want an option to re-clone on merge errors?
      logger.warn { "Unsuccessful attempt to pull from git: $result" }
    }
  }

  private fun syncFlags() {
    val file = directory / properties.flagsSource
    val flagConfiguration = file.bufferedReader().use(BufferedReader::readText)
    val event = SyncFlagsEvent(this, flagConfiguration)
    applicationEventPublisher.publishEvent(event)
  }

  override fun destroy() {
    git.close()
  }

  companion object : Logging
}
