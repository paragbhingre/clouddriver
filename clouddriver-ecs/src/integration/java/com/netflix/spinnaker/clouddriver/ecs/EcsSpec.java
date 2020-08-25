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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.netflix.spinnaker.clouddriver.Main;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

// consider @WebMvcTest for slimmer runtime
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {Main.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = { // TODO put this in a file after confirming central locale
      "spring.application.name = clouddriver",
      "services.front50.baseUrl = http://front50.net",
      "services.fiat.baseUrl = http://fiat.net",
      "redis.enabled = false",
      "sql.enabled = true",
      "sql.taskRepository.enabled = true",
      "sql.cache.enabled = true",
      "sql.cache.readBatchSize = 500",
      "sql.cache.writeBatchSize = 300",
      "sql.scheduler.enabled = true",
      "sql.connectionPools.default.default = true",
      "sql.connectionPools.default.jdbcUrl = jdbc:tc:mysql:5.7.22://somehostname:someport/clouddriver?user=root?password=&",
      "sql.connectionPools.tasks.jdbcUrl = jdbc:tc:mysql:5.7.22://somehostname:someport/clouddriver?user=root?password=&",
      "sql.migration.jdbcUrl = jdbc:tc:mysql:5.7.22://somehostname:someport/clouddriver?user=root?password=&",
      "aws.enabled = true",
      "aws.primaryAccount = aws-account",
      "aws.accounts[0].name = aws-account",
      "aws.accounts[0].accountId = 679273379347", // TODO: mock this
      "aws.accounts[0].regions[0].name = us-west-2",
      "aws.accounts[0].assumeRole = role/SpinnakerManaged",
      "aws.defaultRegions[0].name = us-west-2",
      "aws.defaults.iamRole = BaseIAMRole",
      "ecs.enabled = true",
      "ecs.primaryAccount = ecs-account",
      "ecs.accounts[0].name = ecs-account",
      "ecs.accounts[0].awsAccount = aws-account"
    })
public class EcsSpec {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Value("${ecs.enabled}")
  Boolean ecsEnabled;

  @LocalServerPort private int port;

  @Test
  public void configTest() {
    log.info("This is the port test! The PORT is {}", port);
    assertTrue(ecsEnabled);
  }

  // TODO set up cached values, debug /ops, remove info logs

  @Test
  public void listCredentialsTest() {
    // given
    String url = getTestUrl("/credentials");

    // when
    Response response = get(url).then().contentType(ContentType.JSON).extract().response();
    log.info(response.asString()); // returns 2 entries, aws-account & ecs-account

    // then
    assertNotNull(response); // TODO inspect response contents
  }

  @Test
  public void listLoadBalancersTest() {
    // given
    String url = getTestUrl("/ecs/loadBalancers");

    // when
    Response response = get(url).then().contentType(ContentType.JSON).extract().response();
    log.info("LOAD BALANCERS response:");
    log.info(response.asString()); // returns empty list

    // then
    assertNotNull(response); // TODO inspect response contents
  }

  @Test
  public void listServerGroupsTest() {
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
    return new String(
        Files.readAllBytes(Paths.get("src/integration/resources/testoperations" + path)));
  }

  private String getTestUrl(String path) {
    return "http://localhost:" + port + path;
  }
}
