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

package com.netflix.spinnaker.kork.flags

import org.springframework.boot.context.annotation.ImportCandidates
import org.springframework.context.annotation.ImportSelector
import org.springframework.core.type.AnnotationMetadata

/**
 * Handles importing of classes annotated with [OpenFeatureConfiguration]. These classes must be listed out in
 * resource files named `META-INF/spring/com.netflix.spinnaker.kork.flags.OpenFeatureConfiguration.imports`.
 */
class OpenFeatureImportSelector(private val classLoader: ClassLoader) : ImportSelector {
  override fun selectImports(importingClassMetadata: AnnotationMetadata): Array<String> =
    ImportCandidates.load(OpenFeatureConfiguration::class.java, classLoader).distinct().toTypedArray()
}
