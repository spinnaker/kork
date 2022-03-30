package com.netflix.spinnaker.config;

import org.springframework.core.annotation.Order;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Temporary mitigation for RCE in Spring Core
 * (https://bugalert.org/content/notices/2022-03-30-spring.html) Code from
 * https://www.praetorian.com/blog/spring-core-jdk9-rce/
 */
@ControllerAdvice
@Order(10000)
public class BinderControllerAdvice {

  @InitBinder
  public void setAllowedFields(WebDataBinder dataBinder) {
    String[] denylist = new String[] {"class.*", "Class.*", "*.class.*", "*.Class.*"};
    dataBinder.setDisallowedFields(denylist);
  }
}
