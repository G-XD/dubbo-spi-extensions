/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.remoting.etcd.jetcd;

import com.google.common.base.Charsets;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdClusterImpl;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.support.CloseableClient;
import io.etcd.jetcd.support.Observers;
import io.etcd.jetcd.test.EtcdClusterExtension;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LeaseTest
 */
@Disabled
public class LeaseTest {

    private static final Logger logger = LoggerFactory.getLogger(LeaseTest.class);

    private static EtcdCluster cluster;

    private KV kvClient;
    private Client client;
    private Lease leaseClient;

    private static final ByteSequence KEY = ByteSequence.from("foo", Charsets.UTF_8);
    private static final ByteSequence KEY_2 = ByteSequence.from("foo2", Charsets.UTF_8);
    private static final ByteSequence VALUE = ByteSequence.from("bar", Charsets.UTF_8);

    @BeforeAll
    public static void beforeClass() {
        EtcdClusterExtension clusterExtension = EtcdClusterExtension.builder()
            .withClusterName("etcd-lease")
            .withNodes(3)
            .withSsl(false)
            .build();
        try {
            cluster = clusterExtension.cluster();
        } catch (Exception e) {
            logger.error("Init etcd cluster failed");
        }
        cluster.start();
    }

    @AfterAll
    public static void afterClass() {
        cluster.close();
    }

    @BeforeEach
    public void setUp() {
        client = Client.builder().endpoints(cluster.clientEndpoints()).build();
        kvClient = client.getKVClient();
        leaseClient = client.getLeaseClient();
    }

    @AfterEach
    public void tearDown() {
        if (client != null) {
            client.close();
        }

    }

    @Test
    public void testGrant() throws Exception {
        long leaseID = leaseClient.grant(5).get().getID();

        kvClient.put(KEY, VALUE, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
        assertThat(kvClient.get(KEY).get().getCount()).isEqualTo(1);

        Thread.sleep(6000);
        assertThat(kvClient.get(KEY).get().getCount()).isEqualTo(0);
    }

    @Test
    public void testRevoke() throws Exception {
        long leaseID = leaseClient.grant(5).get().getID();
        kvClient.put(KEY, VALUE, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
        assertThat(kvClient.get(KEY).get().getCount()).isEqualTo(1);
        leaseClient.revoke(leaseID).get();
        assertThat(kvClient.get(KEY).get().getCount()).isEqualTo(0);
    }

    @Test
    public void testKeepAliveOnce() throws ExecutionException, InterruptedException {
        long leaseID = leaseClient.grant(2).get().getID();
        kvClient.put(KEY, VALUE, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
        assertThat(kvClient.get(KEY).get().getCount()).isEqualTo(1);
        LeaseKeepAliveResponse rp = leaseClient.keepAliveOnce(leaseID).get();
        assertThat(rp.getTTL()).isGreaterThan(0);
    }

    @Test
    public void testKeepAlive() throws ExecutionException, InterruptedException {
        long leaseID = leaseClient.grant(2).get().getID();
        kvClient.put(KEY, VALUE, PutOption.newBuilder().withLeaseId(leaseID).build()).get();
        assertThat(kvClient.get(KEY).get().getCount()).isEqualTo(1);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<LeaseKeepAliveResponse> responseRef = new AtomicReference<>();
        StreamObserver<LeaseKeepAliveResponse> observer = Observers.observer(response -> {
            responseRef.set(response);
            latch.countDown();
        });

        try (CloseableClient c = leaseClient.keepAlive(leaseID, observer)) {
            latch.await(5, TimeUnit.SECONDS);
            LeaseKeepAliveResponse response = responseRef.get();
            assertThat(response.getTTL()).isGreaterThan(0);
        }

        Thread.sleep(3000);
        assertThat(kvClient.get(KEY).get().getCount()).isEqualTo(0);
    }

}
