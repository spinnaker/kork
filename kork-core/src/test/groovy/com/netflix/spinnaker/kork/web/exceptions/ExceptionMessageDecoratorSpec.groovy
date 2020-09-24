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
import com.netflix.spinnaker.kork.api.exceptions.ExceptionMessage
import com.netflix.spinnaker.kork.exceptions.SpinnakerException
import org.springframework.beans.BeansException
import org.springframework.beans.factory.ObjectProvider
import spock.lang.Specification

import javax.annotation.Nullable

class ExceptionMessageDecoratorSpec extends Specification {

  String messageToBeAppended = "Message to be appended."
  AccessDeniedExceptionMessage accessDeniedExceptionMessage = new AccessDeniedExceptionMessage(messageToBeAppended)
  ExceptionMessageProvider exceptionMessageProvider = new ExceptionMessageProvider([accessDeniedExceptionMessage])
  ExceptionMessageDecorator exceptionMessageDecorator = new ExceptionMessageDecorator(exceptionMessageProvider)

  def "Returns a message that is the original exception message and the message provided from the appender"() {
    given:
    LocalAccessDeniedException accessDeniedException = new LocalAccessDeniedException("Access is denied.")

    when:
    String userMessage = exceptionMessageDecorator.decorate(accessDeniedException, accessDeniedException.getMessage())

    then:
    userMessage == accessDeniedException.getMessage() + "\n" + messageToBeAppended
  }

  def "Does not return an appended message when the exception type is unsupported"() {
    given:
    RuntimeException runtimeException = new RuntimeException("Runtime exception.")

    when:
    String userMessage = exceptionMessageDecorator.decorate(runtimeException, runtimeException.getMessage())

    then:
    userMessage == runtimeException.getMessage()
  }
}

class ExceptionMessageProvider implements ObjectProvider<List<ExceptionMessage>> {

  List<ExceptionMessage> exceptionMessages

  ExceptionMessageProvider(List<ExceptionMessage> exceptionMessages) {
    this.exceptionMessages = exceptionMessages
  }

  @Override
  List<ExceptionMessage> getObject(Object... args) throws BeansException {
    return exceptionMessages
  }

  @Override
  List<ExceptionMessage> getIfAvailable() throws BeansException {
    return exceptionMessages
  }

  @Override
  List<ExceptionMessage> getIfUnique() throws BeansException {
    return exceptionMessages
  }

  @Override
  List<ExceptionMessage> getObject() throws BeansException {
    return exceptionMessages
  }
}

class AccessDeniedExceptionMessage implements ExceptionMessage {

  private String messageToBeAppended

  AccessDeniedExceptionMessage(String messageToBeAppended) {
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
