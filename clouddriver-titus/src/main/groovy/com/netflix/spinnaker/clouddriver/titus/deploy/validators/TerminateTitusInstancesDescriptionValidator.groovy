/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.titus.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.ValidationErrors
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider
import com.netflix.spinnaker.clouddriver.titus.TitusOperation
import com.netflix.spinnaker.clouddriver.titus.credentials.NetflixTitusCredentials
import com.netflix.spinnaker.clouddriver.titus.deploy.description.TerminateTitusInstancesDescription
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@TitusOperation(AtomicOperations.TERMINATE_INSTANCES)
class TerminateTitusInstancesDescriptionValidator extends AbstractTitusDescriptionValidatorSupport<TerminateTitusInstancesDescription> {

  @Autowired
  TerminateTitusInstancesDescriptionValidator() {
    super("terminateTitusInstancesDescription")
  }

  @Override
  void validate(List priorDescriptions, TerminateTitusInstancesDescription description, ValidationErrors errors) {

    super.validate(priorDescriptions, description, errors)

    if (!description.region) {
      errors.rejectValue "region", "terminateTitusInstancesDescription.region.empty"
    }

    if (description?.credentials && !((NetflixTitusCredentials) description?.credentials).regions.name.contains(description.region)) {
      errors.rejectValue "region", "terminateTitusInstancesDescription.region.not.configured", description.region, "Region not configured"
    }

    if (description.instanceIds) {
      description.instanceIds.each {
        if (!it) {
          errors.rejectValue "instanceId", "terminateTitusInstancesDescription.instanceId.empty"
        }
      }
    } else {
      errors.rejectValue "instanceIds", "terminateTitusInstancesDescription.instanceIds.empty"
    }
  }

}
