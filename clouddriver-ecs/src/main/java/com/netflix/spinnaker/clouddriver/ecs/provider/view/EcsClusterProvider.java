/*
 * Copyright 2017 Lookout, Inc.
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

package com.netflix.spinnaker.clouddriver.ecs.provider.view;

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeClustersResult;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.clouddriver.aws.security.AmazonClientProvider;
import com.netflix.spinnaker.clouddriver.ecs.cache.client.EcsClusterCacheClient;
import com.netflix.spinnaker.clouddriver.ecs.cache.model.EcsCluster;
import com.netflix.spinnaker.clouddriver.ecs.security.NetflixECSCredentials;
import com.netflix.spinnaker.credentials.CredentialsRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EcsClusterProvider {

  private EcsClusterCacheClient ecsClusterCacheClient;
  @Autowired private CredentialsRepository<NetflixECSCredentials> credentialsRepository;
  @Autowired private AmazonClientProvider amazonClientProvider;
  private static final int EcsClusterDescriptionMaxSize = 100;

  @Autowired
  public EcsClusterProvider(Cache cacheView) {
    this.ecsClusterCacheClient = new EcsClusterCacheClient(cacheView);
  }

  public Collection<EcsCluster> getAllEcsClusters() {
    return ecsClusterCacheClient.getAll();
  }

  // TODO include[] input of Describe Cluster is not part of this implementation, need to implement
  // in the future.
  public Collection<Cluster> getAllEcsClusterDetails(String account, String region) {
    List<String> clusterNames = new ArrayList<>();
    Collection<Cluster> clusters = new ArrayList<>();
    Collection<EcsCluster> filteredEcsClusters =
        ecsClusterCacheClient.getAll().stream()
            .filter(
                cluster ->
                    cluster.getRegion().equals(region) && cluster.getAccount().equals(account))
            .collect(Collectors.toList());
    AmazonECS client = getAmazonEcsClient(account, region);
    if (filteredEcsClusters != null && filteredEcsClusters.size() > 0) {
      for (EcsCluster ecsCluster : filteredEcsClusters) {
        clusterNames.add(ecsCluster.getName());
        if (clusterNames.size() % EcsClusterDescriptionMaxSize == 0) {
          List<Cluster> describeClusterResponse =
              makeCallToGetDescribeClusters(client, clusterNames);
          if (!describeClusterResponse.isEmpty()) {
            clusters.addAll(describeClusterResponse);
          }
          clusterNames.clear();
        }
      }
      if (clusterNames.size() % EcsClusterDescriptionMaxSize != 0) {
        List<Cluster> describeClusterResponse = makeCallToGetDescribeClusters(client, clusterNames);
        if (!describeClusterResponse.isEmpty()) {
          clusters.addAll(describeClusterResponse);
        }
      }
    }
    return clusters;
  }

  private AmazonECS getAmazonEcsClient(String account, String region) {
    NetflixECSCredentials credentials = credentialsRepository.getOne(account);
    if (!(credentials instanceof NetflixECSCredentials)) {
      throw new IllegalArgumentException("Invalid credentials:" + account + ":" + region);
    }
    return amazonClientProvider.getAmazonEcs(credentials, region, true);
  }

  private List<Cluster> makeCallToGetDescribeClusters(AmazonECS client, List<String> clusterNames) {
    List<Cluster> describeClusterResponse = getDescribeClusters(client, clusterNames);
    if (describeClusterResponse.size() > 0) {
      return describeClusterResponse;
    }
    return Collections.emptyList();
  }

  private List<Cluster> getDescribeClusters(AmazonECS client, List<String> clusterNames) {
    DescribeClustersRequest describeClustersRequest =
        new DescribeClustersRequest().withClusters(clusterNames);
    DescribeClustersResult describeClustersResult =
        client.describeClusters(describeClustersRequest);
    if (describeClustersResult == null) {
      log.error(
          "Describe Cluster call returned with empty response. Please check your inputs (account, region and cluster list)");
      return Collections.emptyList();
    } else if (!describeClustersResult.getFailures().isEmpty()) {
      log.error(
          "Describe Cluster call responded with failure(s):"
              + describeClustersResult.getFailures());
    }
    return describeClustersResult.getClusters();
  }
}
