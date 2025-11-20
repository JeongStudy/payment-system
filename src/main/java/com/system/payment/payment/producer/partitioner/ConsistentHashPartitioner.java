package com.system.payment.payment.producer.partitioner;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashPartitioner implements Partitioner {

    // 가상 노드 개수(Partition당 할당)
    private static final int VIRTUAL_NODE_COUNT = 100;

    @Override
    public int partition(String topic, Object keyObj, byte[] keyBytes,
                         Object value, byte[] valueBytes, Cluster cluster) {

        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();

        if (keyObj == null) {
            // key가 없으면 라운드로빈
            return Math.abs((int) (System.nanoTime() % numPartitions));
        }

        String key = keyObj.toString();
        SortedMap<Long, Integer> ring = buildConsistentHashRing(numPartitions);
        long hash = hash(key);

        // hash 이상인 첫 번째 노드
        SortedMap<Long, Integer> tailMap = ring.tailMap(hash);
        Long nodeHash = !tailMap.isEmpty() ? tailMap.firstKey() : ring.firstKey();

        return ring.get(nodeHash);
    }

    private SortedMap<Long, Integer> buildConsistentHashRing(int numPartitions) {
        SortedMap<Long, Integer> ring = new TreeMap<>();
        for (int partition = 0; partition < numPartitions; partition++) {
            for (int vNode = 0; vNode < VIRTUAL_NODE_COUNT; vNode++) {
                long hash = hash(partition + "-VN" + vNode);
                ring.put(hash, partition);
            }
        }
        return ring;
    }

    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            // 4바이트만 사용
            return ((long) (digest[3] & 0xFF) << 24)
                    | ((long) (digest[2] & 0xFF) << 16)
                    | ((long) (digest[1] & 0xFF) << 8)
                    | (digest[0] & 0xFF);
        } catch (Exception e) {
            throw new RuntimeException("Hash error", e);
        }
    }

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}