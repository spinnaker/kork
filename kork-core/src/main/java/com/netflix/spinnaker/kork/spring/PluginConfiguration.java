package com.netflix.spinnaker.kork.spring; /*
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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Data;

@Data
public class PluginConfiguration {

  @JsonProperty("name")
  public String name;

  @JsonProperty("path")
  public String path;

  @JsonProperty("enabled")
  public boolean enabled;

  @JsonProperty("extra-configs")
  public Map<String, Object> extraConfigs;
}
