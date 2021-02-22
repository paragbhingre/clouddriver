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

package com.netflix.spinnaker.clouddriver.ecs;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.model.*;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.netflix.spinnaker.cats.agent.DefaultCacheResult;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.DefaultCacheData;
import com.netflix.spinnaker.cats.module.CatsModule;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.cats.provider.ProviderRegistry;
import com.netflix.spinnaker.clouddriver.Main;
import com.netflix.spinnaker.clouddriver.artifacts.ArtifactDownloader;
import com.netflix.spinnaker.clouddriver.aws.security.*;
import com.netflix.spinnaker.clouddriver.aws.security.NetflixAmazonCredentials;
import com.netflix.spinnaker.clouddriver.aws.security.config.CredentialsConfig;
import com.netflix.spinnaker.clouddriver.aws.security.config.CredentialsLoader;
import com.netflix.spinnaker.clouddriver.ecs.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecs.provider.EcsProvider;
import com.netflix.spinnaker.clouddriver.ecs.security.NetflixAssumeRoleEcsCredentials;
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository;
import com.netflix.spinnaker.kork.artifacts.model.Artifact;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {Main.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"spring.config.location = classpath:clouddriver.yml"})
public class EcsSpec {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final String TEST_OPERATIONS_LOCATION = "src/integration/resources/testoperations";
  private final String ECS_ACCOUNT_NAME = "ecs-account";
  private final String TEST_REGION = "us-west-2";

  @Value("${ecs.enabled}")
  Boolean ecsEnabled;

  @LocalServerPort private int port;

  @Autowired private ProviderRegistry providerRegistry;

  @MockBean AmazonClientProvider mockAwsProvider;

  @MockBean AmazonAccountsSynchronizer mockAccountsSyncer;

  @MockBean ArtifactDownloader mockArtifactDownloader;

  @Autowired AccountCredentialsRepository accountCredentialsRepository;

  private AmazonECS mockECS = mock(AmazonECS.class);
  private AmazonElasticLoadBalancing mockELB = mock(AmazonElasticLoadBalancing.class);

  protected static final String TEST_ARTIFACTS_LOCATION = "src/integration/resources/testartifacts";

  @BeforeEach
  void Setup() {
    NetflixAssumeRoleEcsCredentials mockNetflixEcsCreds =
        mock(NetflixAssumeRoleEcsCredentials.class);

    when(mockAwsProvider.getAmazonEcs(
            any(NetflixAmazonCredentials.class), anyString(), anyBoolean()))
        .thenReturn(mockECS);
    when(mockAccountsSyncer.synchronize(
            any(CredentialsLoader.class),
            any(CredentialsConfig.class),
            any(AccountCredentialsRepository.class),
            any(DefaultAccountConfigurationProperties.class),
            any(CatsModule.class)))
        .thenReturn(Collections.singletonList(mockNetflixEcsCreds));

    when(mockECS.describeServices(any(DescribeServicesRequest.class)))
        .thenReturn(new DescribeServicesResult());
    when(mockECS.registerTaskDefinition(any(RegisterTaskDefinitionRequest.class)))
        .thenAnswer(
            (Answer<RegisterTaskDefinitionResult>)
                invocation -> {
                  RegisterTaskDefinitionRequest request =
                      (RegisterTaskDefinitionRequest) invocation.getArguments()[0];
                  String testArn = "arn:aws:ecs:::task-definition/" + request.getFamily() + ":1";
                  TaskDefinition taskDef = new TaskDefinition().withTaskDefinitionArn(testArn);
                  return new RegisterTaskDefinitionResult().withTaskDefinition(taskDef);
                });
    when(mockECS.createService(any(CreateServiceRequest.class)))
        .thenReturn(
            new CreateServiceResult().withService(new Service().withServiceName("createdService")));

    when(mockAwsProvider.getAmazonEcs(
            any(NetflixAssumeRoleAmazonCredentials.class), anyString(), anyBoolean()))
        .thenReturn(mockECS);

    // mock ELB responses
    when(mockELB.describeTargetGroups(any(DescribeTargetGroupsRequest.class)))
        .thenAnswer(
            (Answer<DescribeTargetGroupsResult>)
                invocation -> {
                  DescribeTargetGroupsRequest request =
                      (DescribeTargetGroupsRequest) invocation.getArguments()[0];
                  String testArn =
                      "arn:aws:elasticloadbalancing:::targetgroup/"
                          + request.getNames().get(0)
                          + "/76tgredfc";
                  TargetGroup testTg = new TargetGroup().withTargetGroupArn(testArn);

                  return new DescribeTargetGroupsResult().withTargetGroups(testTg);
                });

    when(mockAwsProvider.getAmazonElasticLoadBalancingV2(
            any(NetflixAmazonCredentials.class), anyString(), anyBoolean()))
        .thenReturn(mockELB);
  }

  protected String generateStringFromTestArtifactFile(String path) throws IOException {
    return new String(Files.readAllBytes(Paths.get(TEST_ARTIFACTS_LOCATION, path)));
  }

  @Test
  public void createServerGroup_ArtifactsFARGATETgMappingsTest()
      throws IOException, InterruptedException {

    // given
    String url = getTestUrl("/ecs/ops/createServerGroup");
    String requestBody =
        generateStringFromTestFile("/createServerGroup-artifact-FARGATE-targetGroupMappings.json");
    String expectedServerGroupName = "ecs-integArtifactsFargateTgMappingsStack-detailTest-v000";
    setEcsAccountCreds();
    ByteArrayInputStream byteArrayInputStreamOfArtifactsForFargateType =
        new ByteArrayInputStream(
            generateStringFromTestArtifactFile(
                    "/createServerGroup-artifact-Fargate-targetGroup-artifactFile.json")
                .getBytes());

    when(mockArtifactDownloader.download(any(Artifact.class)))
        .thenReturn(byteArrayInputStreamOfArtifactsForFargateType);

    String taskId =
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post(url)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", notNullValue())
            .body("resourceUri", containsString("/task/"))
            .extract()
            .path("id");
  }

  @Test
  public void createServerGroup_InputsEc2LegacyTargetGroupTest()
      throws IOException, InterruptedException {

    // given
    String url = getTestUrl("/ecs/ops/createServerGroup");
    String requestBody = generateStringFromTestFile("/createServerGroup-inputs-ec2.json");
    // String expectedServerGroupName = "ecs-integInputsEc2LegacyTargetGroup";
    setEcsAccountCreds();
    // when
    String taskId =
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post(url)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", notNullValue())
            .body("resourceUri", containsString("/task/"))
            .extract()
            .path("id");
  }

  @Test
  public void configTest() {
    log.info("This is the port test! The PORT is {}", port);
    assertTrue(ecsEnabled);
  }

  // TODO debug /ops, remove info logs

  @Test
  public void listCredentialsTest() {
    // given
    String url = getTestUrl("/credentials");

    // when
    Response response =
        get(url).then().statusCode(200).contentType(ContentType.JSON).extract().response();
    log.info(response.asString()); // returns 2 entries, aws-account & ecs-account

    // then
    assertNotNull(response); // TODO inspect response contents
  }

  @Test
  public void getEcsClustersTest() {
    // given
    ProviderCache ecsCache = providerRegistry.getProviderCache(EcsProvider.NAME);
    String testClusterName = "integ-test-cluster";
    String testNamespace = Keys.Namespace.ECS_CLUSTERS.ns;

    String clusterKey = Keys.getClusterKey(ECS_ACCOUNT_NAME, TEST_REGION, testClusterName);
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("account", ECS_ACCOUNT_NAME);
    attributes.put("region", TEST_REGION);
    attributes.put("clusterArn", "arn:aws:ecs:::cluster/" + testClusterName);
    attributes.put("clusterName", testClusterName);

    DefaultCacheResult testResult = buildCacheResult(attributes, testNamespace, clusterKey);
    ecsCache.addCacheResult("TestAgent", Collections.singletonList(testNamespace), testResult);

    // when
    String testUrl = getTestUrl("/ecs/ecsClusters");

    Response response =
        get(testUrl).then().statusCode(200).contentType(ContentType.JSON).extract().response();

    // then
    assertNotNull(response);
    // TODO: serialize into expected return type to validate API contract hasn't changed
    String responseStr = response.asString();
    assertTrue(responseStr.contains(testClusterName));
    assertTrue(responseStr.contains(ECS_ACCOUNT_NAME));
    assertTrue(responseStr.contains(TEST_REGION));
  }

  @Test
  public void getLoadBalancersTest() {
    // given
    String url = getTestUrl("/aws/loadBalancers");

    // when
    Response response = get(url).then().contentType(ContentType.JSON).extract().response();
    log.info("LOAD BALANCERS response:");
    log.info(response.asString()); // returns empty list

    // then
    assertNotNull(response); // TODO inspect response contents
  }

  @Test
  public void getServerGroupsTest() {
    // given
    String url = getTestUrl("/serverGroups");

    // when
    Response response =
        given()
            .param("cloudProvider", "ecs")
            .param("applications", "ecs")
            .when()
            .get(url)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .response();
    log.info("SERVER GROUPS response:");
    log.info(response.asString()); // returns empty list

    // then
    assertNotNull(response); // TODO inspect response contents
  }

  @Test
  public void createServerGroupOperationTest() throws IOException { // no response
    // given
    String url = getTestUrl("/ecs/ops");
    String requestBody = generateStringFromTestFile("/createServiceOperation.json");
    log.info("request body:");
    log.info(requestBody);

    // when
    Response response =
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post(url)
            .then()
            .contentType("")
            .extract()
            .response();

    log.info("request response:");
    log.info(response.asString());

    // then
    assertNotNull(response); // TODO inspect response contents
  }

  private String generateStringFromTestFile(String path) throws IOException {
    return new String(Files.readAllBytes(Paths.get(TEST_OPERATIONS_LOCATION, path)));
  }

  private String getTestUrl(String path) {
    return "http://localhost:" + port + path;
  }

  private DefaultCacheResult buildCacheResult(
      Map<String, Object> attributes, String namespace, String key) {
    Collection<CacheData> dataPoints = new LinkedList<>();
    dataPoints.add(new DefaultCacheData(key, attributes, Collections.emptyMap()));

    Map<String, Collection<CacheData>> dataMap = new HashMap<>();
    dataMap.put(namespace, dataPoints);

    return new DefaultCacheResult(dataMap);
  }

  /* private void setEcsAccountCreds() {
    AmazonCredentials.AWSRegion testRegion = new AmazonCredentials.AWSRegion(TEST_REGION, null);

    NetflixAssumeRoleAmazonCredentials ecsCreds =
        new NetflixAssumeRoleAmazonCredentials(
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
            "arn:aws-cn:iam::000000000000:role/spin-managed-role",
            null,
            null);

    accountCredentialsRepository.save(ECS_ACCOUNT_NAME, ecsCreds);
  }*/

  private void setEcsAccountCreds() {
    AmazonCredentials.AWSRegion testRegion = new AmazonCredentials.AWSRegion(TEST_REGION, null);

    NetflixAssumeRoleAmazonCredentials ecsCreds =
        new NetflixAssumeRoleAmazonCredentials(
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
            "role/assumerole",
            null,
            null);

    accountCredentialsRepository.save(ECS_ACCOUNT_NAME, ecsCreds);
  }
}
