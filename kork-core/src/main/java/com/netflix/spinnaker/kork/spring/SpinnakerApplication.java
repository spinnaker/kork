package com.netflix.spinnaker.kork.spring;
/*
 * Copyright 2019 Armory, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
 */

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class SpinnakerApplication extends SpringBootServletInitializer {

  public static void initialize() {
    addPlugins();
  }

  public static void addPlugins() {
    List<Path> jarPaths = listJars();
    try {
      addJarsToClasspath(jarPaths);
    } catch (Exception e) {
      System.out.println("exception: " + e.getMessage());
    }
  }

  public static List<Path> listJars() {
    try {
      return Files.walk(Paths.get("/opt/spinnaker/plugins/"))
          .filter(Files::isRegularFile)
          .collect(Collectors.toList());
    } catch (Exception e) {
      // TODO (cmotevasselani): do something here, or not
      System.out.println(e.getMessage());
    }
    // Should probably just raise an error here, but only if plugins were configured
    return new ArrayList<>();
  }

  public static void addJarsToClasspath(List<Path> jars)
      throws NoSuchMethodException, SecurityException, IllegalAccessException,
          IllegalArgumentException, InvocationTargetException, MalformedURLException {

    // Get the ClassLoader class
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    Class<?> clazz = cl.getClass();

    // Get the protected addURL method from the parent URLClassLoader class
    Method method = clazz.getSuperclass().getDeclaredMethod("addURL", new Class[] {URL.class});

    for (Path jarPath : jars) {
      // Run projected addURL method to add JAR to classpath
      method.setAccessible(true);
      method.invoke(cl, new Object[] {jarPath.toUri().toURL()});
    }
  }

  public static void addJarToClasspath(File jar)
      throws NoSuchMethodException, SecurityException, IllegalAccessException,
          IllegalArgumentException, InvocationTargetException, MalformedURLException {
    // Get the ClassLoader class
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    Class<?> clazz = cl.getClass();

    // Get the protected addURL method from the parent URLClassLoader class
    Method method = clazz.getSuperclass().getDeclaredMethod("addURL", new Class[] {URL.class});

    // Run projected addURL method to add JAR to classpath
    method.setAccessible(true);
    method.invoke(cl, new Object[] {jar.toURI().toURL()});
  }
}
