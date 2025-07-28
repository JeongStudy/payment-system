#!/bin/bash

echo "Create Kafka topic [payment] ..."

docker exec local-kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create --if-not-exists \
  --topic payment \
  --partitions 2 \
  --replication-factor 1

echo "Done."