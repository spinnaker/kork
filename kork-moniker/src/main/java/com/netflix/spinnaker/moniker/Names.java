/*
 * Copyright 2017 Google, Inc.
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

package com.netflix.spinnaker.moniker;

public interface Names {
  String getApp();

  String getCluster();

  // TODO(lwander) determine if necessary for Spinnaker
  String getCountries();

  String getDetail();

  // TODO(lwander) determine if necessary for Spinnaker
  String getDevPhase();

  // TODO(lwander) determine if necessary for Spinnaker
  String getGroup();

  // TODO(lwander) determine if necessary for Spinnaker
  String getHardware();

  // TODO(lwander) determine if necessary for Spinnaker
  String getPush();

  // TODO(lwander) determine if necessary for Spinnaker
  String getPartners();

  // TODO(lwander) determine if necessary for Spinnaker
  String getRevision();

  // TODO(lwander) determine if necessary for Spinnaker
  String getRedBlackSwap();

  Integer getSequence();

  String getStack();

  // TODO(lwander) determine if necessary for Spinnaker
  String getUsedBy();

  // TODO(lwander) determine if necessary for Spinnaker
  String getZone();
}

