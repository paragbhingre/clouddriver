/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under
 * the License.
 */

package com.netflix.spinnaker.clouddriver.ecs.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.applicationautoscaling.AWSApplicationAutoScalingClient;
import com.amazonaws.services.applicationautoscaling.model.*;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalableTargetsRequest;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.*;
import com.amazonaws.services.ecs.model.Service;
import com.netflix.spinnaker.clouddriver.artifacts.ArtifactDownloader;
import com.netflix.spinnaker.clouddriver.aws.security.AmazonClientProvider;
import com.netflix.spinnaker.clouddriver.aws.security.AmazonCredentials;
import com.netflix.spinnaker.clouddriver.aws.security.NetflixAmazonCredentials;
import com.netflix.spinnaker.clouddriver.ecs.EcsSpec;
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.util.*;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class CreateServerGroupSpec extends EcsSpec {

  @Autowired AccountCredentialsRepository accountCredentialsRepository;

  @Autowired ArtifactDownloader artifactDownloader;

  @Autowired AmazonClientProvider amazonClientProvider;

  @MockBean AWSApplicationAutoScalingClient mockAWSApplicationAutoScalingClient;

  @MockBean AmazonECS ecs;

  // @Autowired Artifact artifact;

  @MockBean AmazonECSClient amazonECSClient;

  private final Logger log = LoggerFactory.getLogger(getClass());

  @BeforeEach
  public void setup() {
    ecs = mock(AmazonECS.class);
  }

  @DisplayName(
      ".\n===\n"
          + "Given description w/ task def inputs, FARGATE launch type, and new target group fields, "
          + "successfully submit createServerGroup operation"
          + "\n===")
  @Test
  public void createSGOWithArtifactsEC2TargetGroupMappingsTest() throws IOException {
    /**
     * TODO (pbhingre): Ideally this test would go further and actually assert that the resulting
     * ecs:create-service call is formed as expected, but for now, it asserts that the given
     * operation is correctly validated and submitted as a task.
     */

    // given
    String url = getTestUrl("/ecs/ops/createServerGroup");
    String requestBody =
        generateStringFromTestFile("/createServerGroup-artifact-EC2-targetGroupMappings.json");
    NetflixAmazonCredentials ecsCreds = setEcsAccountCreds();

    // mocking ListServicesRequest
    ListServicesRequest listServicesRequest =
        new ListServicesRequest().withCluster("spinnaker-deployment-cluster");

    // mocking ListServicesResult
    List<String> serviceArnsList = new ArrayList<>();
    serviceArnsList.add("arn:aws:service:region:account-id:ecs-integTestStack-detailTest");
    ListServicesResult listServicesResult =
        new ListServicesResult().withServiceArns(serviceArnsList);

    // mocking DescribeServicesRequest
    DescribeServicesRequest describeServicesRequest =
        new DescribeServicesRequest()
            .withCluster("spinnaker-deployment-cluster")
            .withServices("ecs-integTestStack-detailTest-v001");

    DescribeServicesRequest describeServicesRequestForSourceService =
        new DescribeServicesRequest()
            .withCluster("spinnaker-deployment-cluster")
            .withServices("ecs");

    DescribeServicesRequest describeServicesRequestForServices =
        new DescribeServicesRequest()
            .withCluster("spinnaker-deployment-cluster")
            .withServices("arn:aws:service:region:account-id:ecs-integTestStack-detailTest");

    // mocking DescribeServicesResult
    List<Service> listOfServices = new ArrayList<>();
    Service service =
        new Service()
            .withServiceName("test-service")
            .withLaunchType("EC2")
            .withStatus("INACTIVE")
            .withCreatedAt(new Date());
    listOfServices.add(service);

    DescribeServicesResult describeServicesResult =
        new DescribeServicesResult().withServices(listOfServices);

    DescribeServicesResult describeServiceResultForServices =
        new DescribeServicesResult().withServices(listOfServices);

    DescribeServicesResult describeServicesResultForgetSourceService =
        new DescribeServicesResult().withServices(listOfServices);

    // mocking DescribeScalableTargetsRequest
    DescribeScalableTargetsRequest describeScalableTargetsRequest =
        new DescribeScalableTargetsRequest()
            .withServiceNamespace(ServiceNamespace.Ecs)
            .withScalableDimension(ScalableDimension.EcsServiceDesiredCount)
            .withResourceIds(String.format("service/%s/%s", "spinnaker-deployment-cluster", "ecs"));
    // mocking DescribeScalableTargetsResult
    DescribeScalableTargetsResult mockDescribeScalableTargetsResult =
        new DescribeScalableTargetsResult();

    // mocking Artifact
    /*artifact.toBuilder().artifactAccount("ecs");
    artifact.toBuilder().customKind(true);
    artifact.toBuilder().name("application");
    artifact.toBuilder().version("ver-0.1");
    artifact.toBuilder().location("us-east-1");
    artifact.toBuilder().reference("reference");
    artifact.builder().putMetadata("account", "ecs-account");
    artifact.toBuilder().artifactAccount("my-github");
    artifact.toBuilder().provenance("prov");
    artifact.toBuilder().uuid("uid-123");*/

    // mocking calls
    when(amazonClientProvider.getAmazonEcs(ecsCreds, "us-west-2", false))
        .thenReturn(ecs); // mocking getAmazonEcsClient(); in operate method of
    // CreateServerGroupOperation.java
    when(amazonClientProvider.getAmazonApplicationAutoScaling(ecsCreds, "us-west-2", false))
        .thenReturn(mockAWSApplicationAutoScalingClient);
    when(ecs.listServices(listServicesRequest))
        .thenReturn(listServicesResult); // mocking listServices() in getTakenSlots()  method of
    // EcsServerGroupNameResolver.java
    when(ecs.describeServices(describeServicesRequest)).thenReturn(describeServicesResult); //
    when(ecs.describeServices(describeServicesRequestForSourceService))
        .thenReturn(describeServicesResultForgetSourceService); // mocking describeServices in
    // getSourceService() method of
    // CreateServerGroupAtomicOperation.java
    when(ecs.describeServices(describeServicesRequestForServices))
        .thenReturn(
            describeServicesResult); // mocking describeServices() in getTakenSlots() method of
    // super.EcsServerGroupNameResolver.java
    when(ecs.describeServices(describeServicesRequestForServices))
        .thenReturn(describeServiceResultForServices); // mocking describeServices() in
    // resolveNextServerGroupName() method of
    // EcsServerGroupNameResolver.java
    when(mockAWSApplicationAutoScalingClient.describeScalableTargets(
            describeScalableTargetsRequest))
        .thenReturn(mockDescribeScalableTargetsResult); // mocking describeScalableTargets() in
    // getSourceScalableTarget() of
    // CreateServerGroupAtomicOperation.java
    // when(artifactDownloader.download(artifact)).thenReturn(new
    // ByteArrayInputStream(generateStringFromTestFile("/createServerGroup-artifact-EC2-targetGroup-artifactFile.json").getBytes()));
    // when
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post(url)
        // then
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("id", notNullValue())
        .body("resourceUri", containsString("/task/"));
  }

  @DisplayName(
      ".\n===\n"
          + "Given description w/ task def inputs, FARGATE launch type, and new target group fields, "
          + "successfully submit createServerGroup operation"
          + "\n===")
  @Test
  public void createSGOWithArtifactsFargateTargetGroupMappingsTest() throws IOException {
    /**
     * TODO (pbhingre): Ideally this test would go further and actually assert that the resulting
     * ecs:create-service call is formed as expected, but for now, it asserts that the given
     * operation is correctly validated and submitted as a task.
     */

    // given
    String url = getTestUrl("/ecs/ops/createServerGroup");
    String requestBody =
        generateStringFromTestFile("/createServerGroup-artifact-FARGATE-targetGroupMappings.json");
    NetflixAmazonCredentials ecsCreds = setEcsAccountCreds();

    // mocking ListServicesRequest
    ListServicesRequest listServicesRequest =
        new ListServicesRequest().withCluster("spinnaker-deployment-cluster");

    // mocking ListServicesResult
    List<String> serviceArnsList = new ArrayList<>();
    serviceArnsList.add("arn:aws:service:region:account-id:ecs-integTestStack-detailTest");
    ListServicesResult mockListServicesResult =
        new ListServicesResult().withServiceArns(serviceArnsList);

    // mocking DescribeServicesRequest
    DescribeServicesRequest describeServicesRequest =
        new DescribeServicesRequest()
            .withCluster("spinnaker-deployment-cluster")
            .withServices("ecs-integTestStack-detailTest-v001");

    DescribeServicesRequest describeServicesRequestForSourceService =
        new DescribeServicesRequest()
            .withCluster("spinnaker-deployment-cluster")
            .withServices("ecs");

    DescribeServicesRequest describeServicesRequestForServices =
        new DescribeServicesRequest()
            .withCluster("spinnaker-deployment-cluster")
            .withServices("arn:aws:service:region:account-id:ecs-integTestStack-detailTest");

    // mocking DescribeServicesResult
    List<Service> listOfServices = new ArrayList<>();
    Service service =
        new Service()
            .withServiceName("test-service")
            .withLaunchType("FARGATE")
            .withStatus("INACTIVE")
            .withCreatedAt(new Date());
    listOfServices.add(service);

    DescribeServicesResult describeServicesResult =
        new DescribeServicesResult().withServices(listOfServices);

    DescribeServicesResult describeServiceResultForServices =
        new DescribeServicesResult().withServices(listOfServices);

    DescribeServicesResult describeServicesResultForgetSourceService =
        new DescribeServicesResult().withServices(listOfServices);

    // mocking DescribeScalableTargetsRequest
    DescribeScalableTargetsRequest describeScalableTargetsRequest =
        new DescribeScalableTargetsRequest()
            .withServiceNamespace(ServiceNamespace.Ecs)
            .withScalableDimension(ScalableDimension.EcsServiceDesiredCount)
            .withResourceIds(String.format("service/%s/%s", "spinnaker-deployment-cluster", "ecs"));

    // mocking DescribeScalableTargetsResult
    DescribeScalableTargetsResult describeScalableTargetsResult =
        new DescribeScalableTargetsResult();

    // mocking Artifact
    /*artifact.toBuilder().artifactAccount("ecs");
    artifact.toBuilder().customKind(true);
    artifact.toBuilder().name("application");
    artifact.toBuilder().version("ver-0.1");
    artifact.toBuilder().location("us-east-1");
    artifact.toBuilder().reference("reference");
    artifact.builder().putMetadata("account", "ecs-account");
    artifact.toBuilder().artifactAccount("my-github");
    artifact.toBuilder().provenance("prov");
    artifact.toBuilder().uuid("uid-123");*/

    // mocking calls
    when(amazonClientProvider.getAmazonEcs(ecsCreds, "us-west-2", false)).thenReturn(ecs);
    when(amazonClientProvider.getAmazonApplicationAutoScaling(ecsCreds, "us-west-2", false))
        .thenReturn(mockAWSApplicationAutoScalingClient);
    when(ecs.listServices(listServicesRequest)).thenReturn(mockListServicesResult);
    when(ecs.describeServices(describeServicesRequest)).thenReturn(describeServicesResult);
    when(ecs.describeServices(describeServicesRequestForSourceService))
        .thenReturn(describeServicesResultForgetSourceService);
    when(ecs.describeServices(describeServicesRequestForServices))
        .thenReturn(describeServicesResult);
    when(ecs.describeServices(describeServicesRequestForServices))
        .thenReturn(describeServiceResultForServices);
    when(mockAWSApplicationAutoScalingClient.describeScalableTargets(
            describeScalableTargetsRequest))
        .thenReturn(describeScalableTargetsResult);
    // when(artifactDownloader.download(artifact)).thenReturn(new
    // ByteArrayInputStream(generateStringFromTestFile("/createServerGroup-artifact-EC2-targetGroup-artifactFile.json").getBytes()));
    // when
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post(url)
        // then
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("id", notNullValue())
        .body("resourceUri", containsString("/task/"));
  }

  @DisplayName(
      ".\n===\n"
          + "Given description w/ inputs, EC2 launch type, and legacy target group fields, "
          + "successfully submit createServerGroup operation"
          + "\n===")
  @Test
  public void createServerGroupOperationTest() throws IOException {
    /**
     * TODO (allisaurus): Ideally this test would go further and actually assert that the resulting
     * ecs:create-service call is formed as expected, but for now, it asserts that the given
     * operation is correctly validated and submitted as a task.
     */

    // given
    String url = getTestUrl("/ecs/ops/createServerGroup");
    String requestBody = generateStringFromTestFile("/createServerGroup-inputs-ec2.json");
    setEcsAccountCreds();

    // when
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post(url)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("id", notNullValue())
        .body("resourceUri", containsString("/task/"));
  }

  @DisplayName(
      ".\n===\n"
          + "Given description w/ task def inputs, FARGATE launch type, and legacy target group fields, "
          + "successfully submit createServerGroup operation"
          + "\n===")
  @Test
  public void createSGOWithInputsFargateLegacyTargetGroupTest() throws IOException {
    /**
     * TODO (pbhingre): Ideally this test would go further and actually assert that the resulting
     * ecs:create-service call is formed as expected, but for now, it asserts that the given
     * operation is correctly validated and submitted as a task.
     */

    // given
    String url = getTestUrl("/ecs/ops/createServerGroup");
    String requestBody =
        generateStringFromTestFile("/createServerGroup-inputs-fargate-legacyTargetGroup.json");
    setEcsAccountCreds();

    /*NetflixAmazonCredentials ecsCreds =  setEcsAccountCreds();

    //mocking mockListServicesRequest
    ListServicesRequest listServicesRequest = new ListServicesRequest().withCluster("spinnaker-deployment-cluster");

    //mocking mockListServicesResult
    List<String> serviceArnsList = new ArrayList<>();
    serviceArnsList.add("arn:aws:service:region:account-id:ecs-integTestStack-detailTest");
    ListServicesResult listServicesResult = new ListServicesResult().withServiceArns(serviceArnsList);

    //mocking DescribeServicesRequest
    DescribeServicesRequest describeServicesRequest =
      new DescribeServicesRequest()
        .withCluster("ecs-integTestStack-detailTest")
        .withServices("ecs-integTestStack-detailTest-v001");

    DescribeServicesRequest describeServicesRequestForSourceService =
      new DescribeServicesRequest()
        .withCluster("spinnaker-deployment-cluster")
        .withServices("ecs");

    DescribeServicesRequest describeServicesRequestForServices =
      new DescribeServicesRequest()
        .withCluster("spinnaker-deployment-cluster")
        .withServices("arn:aws:service:region:account-id:ecs-integTestStack-detailTest");

    //mocking mockDescribeServicesResult
    List<Service> listOfServices = new ArrayList<>();
    Service service = new Service().withServiceName("test-service").withLaunchType("FARGATE")
      .withStatus("INACTIVE")
      .withCreatedAt(new Date());
    listOfServices.add(service);

    DescribeServicesResult describeServicesResult = new  DescribeServicesResult()
      .withServices(listOfServices);

    DescribeServicesResult describeServiceResultForServices = new DescribeServicesResult()
      .withServices(listOfServices);

    DescribeServicesResult describeServicesResultForSourceService = new DescribeServicesResult()
      .withServices(listOfServices);

    //mocking DescribeScalableTargetsRequest
    DescribeScalableTargetsRequest describeScalableTargetsRequest =
      new DescribeScalableTargetsRequest()
        .withServiceNamespace(ServiceNamespace.Ecs)
        .withScalableDimension(ScalableDimension.EcsServiceDesiredCount)
        .withResourceIds(
          String.format(
            "service/%s/%s",
            "spinnaker-deployment-cluster", "ecs"));
    //mocking DescribeScalableTargetsResult
    DescribeScalableTargetsResult describeScalableTargetsResult = new DescribeScalableTargetsResult();

    //mocking RegisterTaskDefinitionRequest
    RegisterTaskDefinitionRequest registerTaskDefinitionRequest = new RegisterTaskDefinitionRequest();

    //mocking
    RegisterTaskDefinitionResult registerTaskDefinitionResult = new RegisterTaskDefinitionResult();

    //mocking calls
    when(amazonClientProvider.getAmazonEcs(ecsCreds, "us-west-2", false)).thenReturn(ecs);
    when(amazonClientProvider.getAmazonApplicationAutoScaling(ecsCreds, "us-west-2", false)).thenReturn(mockAWSApplicationAutoScalingClient);
    when(ecs.listServices(listServicesRequest)).thenReturn(listServicesResult);
    when(ecs.describeServices(describeServicesRequest)).thenReturn(describeServicesResult);
    when(ecs.describeServices(describeServicesRequestForSourceService)).thenReturn(describeServicesResultForSourceService);
    when(ecs.describeServices(describeServicesRequestForServices)).thenReturn(describeServicesResult);
    when(ecs.describeServices( describeServicesRequestForServices)).thenReturn(describeServiceResultForServices);
    //when(ecs.registerTaskDefinition())
    when(mockAWSApplicationAutoScalingClient.describeScalableTargets(describeScalableTargetsRequest)).thenReturn(describeScalableTargetsResult);*/

    // when
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post(url)

        // then
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("id", notNullValue())
        .body("resourceUri", containsString("/task/"));
  }

  @DisplayName(
      ".\n===\n"
          + "Given description w/ task def inputs, FARGATE launch type, and new target group fields, "
          + "successfully submit createServerGroup operation"
          + "\n===")
  @Test
  public void createSGOWithInputsFargateNewTargetGroupMappingsTest() throws IOException {
    /**
     * TODO (pbhingre): Ideally this test would go further and actually assert that the resulting
     * ecs:create-service call is formed as expected, but for now, it asserts that the given
     * operation is correctly validated and submitted as a task.
     */

    // given
    String url = getTestUrl("/ecs/ops/createServerGroup");
    String requestBody =
        generateStringFromTestFile("/createServerGroup-inputs-fargate-NewTargetGroup.json");
    NetflixAmazonCredentials ecsCreds = setEcsAccountCreds();

    /*//Mocking starts here

    //mocking listServicesRequest
    ListServicesRequest listServicesRequest = new ListServicesRequest().withCluster("spinnaker-deployment-cluster");

    //mocking listServicesResult
    List<String> serviceArnsList = new ArrayList<>();
    serviceArnsList.add("arn:aws:service:region:account-id:ecs-integTestStack-detailTest");
    ListServicesResult listServicesResult = new ListServicesResult().withServiceArns(serviceArnsList);

    //Mocking DescribeServicesRequest
    DescribeServicesRequest describeServicesRequest =
      new DescribeServicesRequest()
        .withCluster("spinnaker-deployment-cluster")
        .withServices("ecs-integTestStack-detailTest-v001");

    DescribeServicesRequest describeServicesRequestForSourceService =
      new DescribeServicesRequest()
        .withCluster("spinnaker-deployment-cluster")
        .withServices("ecs");

    DescribeServicesRequest describeServicesRequestForServices =
      new DescribeServicesRequest()
        .withCluster("spinnaker-deployment-cluster")
        .withServices("arn:aws:service:region:account-id:ecs-integTestStack-detailTest");

    //mocking mockDescribeServicesResult
    List<Service> listOfServices = new ArrayList<>();
    Service service = new Service().withServiceName("test-service").withLaunchType("FARGATE")
      .withStatus("INACTIVE")
      .withCreatedAt(new Date());
    listOfServices.add(service);

    DescribeServicesResult describeServicesResult = new DescribeServicesResult().withServices(listOfServices);

    DescribeServicesResult describeServiceResultForServices = new DescribeServicesResult().withServices(listOfServices);

    DescribeServicesResult describeServicesResultForSourceService = new DescribeServicesResult().withServices(listOfServices);

    //mocking DescribeScalableTargetsRequest
    DescribeScalableTargetsRequest describeScalableTargetsRequest =
      new DescribeScalableTargetsRequest()
        .withServiceNamespace(ServiceNamespace.Ecs)
        .withScalableDimension(ScalableDimension.EcsServiceDesiredCount)
        .withResourceIds(
          String.format(
            "service/%s/%s",
            "spinnaker-deployment-cluster", "ecs"));

    //mocking DescribeScalableTargetsResult
    DescribeScalableTargetsResult describeScalableTargetsResult = new DescribeScalableTargetsResult();

    when(amazonClientProvider.getAmazonEcs(ecsCreds, "us-west-2", false)).thenReturn(ecs);
    when(amazonClientProvider.getAmazonApplicationAutoScaling(ecsCreds, "us-west-2", false)).thenReturn(mockAWSApplicationAutoScalingClient);
    when(ecs.listServices(listServicesRequest)).thenReturn(listServicesResult);
    when(ecs.describeServices(describeServicesRequest)).thenReturn(describeServicesResult);
    when(ecs.describeServices(describeServicesRequestForSourceService)).thenReturn(describeServicesResultForSourceService);
    when(ecs.describeServices(describeServicesRequestForServices)).thenReturn(describeServicesResult);
    when(ecs.describeServices(describeServicesRequestForServices)).thenReturn(describeServiceResultForServices);
    when(mockAWSApplicationAutoScalingClient.describeScalableTargets(describeScalableTargetsRequest)).thenReturn(describeScalableTargetsResult);*/

    // when
    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post(url)

        // then
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("id", notNullValue())
        .body("resourceUri", containsString("/task/"));
  }

  private NetflixAmazonCredentials setEcsAccountCreds() {
    AmazonCredentials.AWSRegion testRegion = new AmazonCredentials.AWSRegion(TEST_REGION, null);

    NetflixAmazonCredentials ecsCreds =
        new NetflixAmazonCredentials(
            ECS_ACCOUNT_NAME,
            "test",
            "test",
            "123456789012",
            null,
            true,
            Collections.singletonList(testRegion),
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            false);

    accountCredentialsRepository.save(ECS_ACCOUNT_NAME, ecsCreds);

    return ecsCreds;
  }
}
