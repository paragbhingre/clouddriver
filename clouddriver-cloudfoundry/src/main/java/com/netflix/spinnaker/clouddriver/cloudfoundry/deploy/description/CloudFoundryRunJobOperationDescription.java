/*
 * Copyright 2019 Pivotal, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.spinnaker.clouddriver.cloudfoundry.deploy.description;

import com.netflix.frigga.Names;
import com.netflix.spinnaker.clouddriver.cloudfoundry.model.CloudFoundryServerGroup;
import com.netflix.spinnaker.clouddriver.security.resources.ApplicationNameable;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CloudFoundryRunJobOperationDescription extends AbstractCloudFoundryDescription
    implements ApplicationNameable {

  private CloudFoundryServerGroup serverGroup;
  @Nullable private String jobName;
  private String command;

  @Override
  public Collection<String> getApplications() {
    return Collections.singletonList(Names.parseName(serverGroup.getName()).getApp());
  }
}