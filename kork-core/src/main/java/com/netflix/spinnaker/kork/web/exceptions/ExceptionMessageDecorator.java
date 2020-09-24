package com.netflix.spinnaker.kork.web.exceptions;

import com.netflix.spinnaker.kork.api.exceptions.ExceptionDetails;
import com.netflix.spinnaker.kork.api.exceptions.UserMessage;
import java.util.List;
import javax.annotation.Nullable;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Used to add additional information to an exception message. Messages on exceptions are immutable,
 * so this is mostly used when a message is pulled from an exception prior to being sent to an
 * end-user.
 */
public class ExceptionMessageDecorator {

  private final ObjectProvider<List<UserMessage>> userMessagesProvider;

  public ExceptionMessageDecorator(ObjectProvider<List<UserMessage>> userMessagesProvider) {
    this.userMessagesProvider = userMessagesProvider;
  }

  /**
   * Provided an exception type, original message, and optional exception details, return a message
   * for the end-user.
   *
   * @param throwable {@link Throwable}
   * @param message The exception message (which can be different from the message on the thrown
   *     exception).
   * @param exceptionDetails Additional {@link ExceptionDetails} about the exception.
   * @return The final exception message for the end-user.
   */
  public String decorate(
      Throwable throwable, String message, @Nullable ExceptionDetails exceptionDetails) {
    List<UserMessage> userMessages = userMessagesProvider.getIfAvailable();

    StringBuilder sb = new StringBuilder();
    sb.append(message);

    if (userMessages != null && !userMessages.isEmpty()) {
      for (UserMessage userMessage : userMessages) {
        if (userMessage.supports(throwable.getClass())) {
          String messageToAppend = userMessage.message(throwable, exceptionDetails);
          if (messageToAppend != null && !messageToAppend.isEmpty()) {
            sb.append("\n").append(messageToAppend);
          }
        }
      }
    }

    return sb.toString();
  }

  public String decorate(Throwable throwable, String message) {
    return decorate(throwable, message, null);
  }
}
