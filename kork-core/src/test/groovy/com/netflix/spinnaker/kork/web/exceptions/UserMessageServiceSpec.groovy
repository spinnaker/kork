/*
 * Copyright 2020 Netflix, Inc.
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

package com.netflix.spinnaker.kork.web.exceptions

import com.netflix.spinnaker.kork.api.exceptions.ExceptionDetails
import com.netflix.spinnaker.kork.api.exceptions.UserMessage
import com.netflix.spinnaker.kork.exceptions.SpinnakerException
import org.springframework.beans.BeansException
import org.springframework.beans.factory.ObjectProvider
import spock.lang.Specification

import javax.annotation.Nullable

class UserMessageServiceSpec extends Specification {

  String messageToBeAppended = "Message to be appended."
  AccessDeniedUserMessage accessDeniedUserMessageAppender = new AccessDeniedUserMessage(messageToBeAppended)
  UserMessageAppenderProvider userMessageAppenderProvider = new UserMessageAppenderProvider([accessDeniedUserMessageAppender])
  UserMessageService userMessageService = new UserMessageService(userMessageAppenderProvider)

  def "Returns a message that is the original exception message and the message provided from the appender"() {
    given:
    LocalAccessDeniedException accessDeniedException = new LocalAccessDeniedException("Access is denied.")

    when:
    String userMessage = userMessageService.userMessage(accessDeniedException, accessDeniedException.getMessage())

    then:
    userMessage == accessDeniedException.getMessage() + "\n" + messageToBeAppended
  }

  def "Does not return an appended message when the exception type is unsupported"() {
    given:
    RuntimeException runtimeException = new RuntimeException("Runtime exception.")

    when:
    String userMessage = userMessageService.userMessage(runtimeException, runtimeException.getMessage())

    then:
    userMessage == runtimeException.getMessage()
  }
}

class UserMessageAppenderProvider implements ObjectProvider<List<UserMessage>> {

  List<UserMessage> userMessageAppenders

  UserMessageAppenderProvider(List<UserMessage> userMessageAppenders) {
    this.userMessageAppenders = userMessageAppenders
  }

  @Override
  List<UserMessage> getObject(Object... args) throws BeansException {
    return userMessageAppenders
  }

  @Override
  List<UserMessage> getIfAvailable() throws BeansException {
    return userMessageAppenders
  }

  @Override
  List<UserMessage> getIfUnique() throws BeansException {
    return userMessageAppenders
  }

  @Override
  List<UserMessage> getObject() throws BeansException {
    return userMessageAppenders
  }
}

class AccessDeniedUserMessage implements UserMessage {

  private String messageToBeAppended

  AccessDeniedUserMessage(String messageToBeAppended) {
    this.messageToBeAppended = messageToBeAppended
  }

  @Override
  boolean supports(Class<? extends Throwable> throwable) {
    return throwable == LocalAccessDeniedException.class
  }

  @Override
  String message(Throwable throwable, @Nullable ExceptionDetails exceptionDetails) {
    return messageToBeAppended
  }
}

class LocalAccessDeniedException extends SpinnakerException {
  LocalAccessDeniedException(String message) {
    super(message)
  }
}
