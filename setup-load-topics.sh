#!/bin/bash


echo "Deleting topics: [pf-sla-yolo, pf-sla-throughput, pf-sla-durable]..."
echo "================"

$KAFKA_HOME/bin/kafka-topics --zookeeper localhost:2181 --delete --topic pf-sla-yolo --if-exists
$KAFKA_HOME/bin/kafka-topics --zookeeper localhost:2181 --delete --topic pf-sla-throughput --if-exists
$KAFKA_HOME/bin/kafka-topics --zookeeper localhost:2181 --delete --topic pf-sla-durable --if-exists

echo "Done deleting topics"
echo ""
echo "Creating topics: [pf-sla-yolo, pf-sla-throughput, pf-sla-durable]..."
echo "================"

$KAFKA_HOME/bin/kafka-topics --zookeeper localhost:2181 --create --topic pf-sla-yolo --partitions 2 --replication-factor 2 --config min.insync.replicas=1
$KAFKA_HOME/bin/kafka-topics --zookeeper localhost:2181 --create --topic pf-sla-throughput --partitions 2 --replication-factor 3 --config min.insync.replicas=2
$KAFKA_HOME/bin/kafka-topics --zookeeper localhost:2181 --create --topic pf-sla-durable --partitions 2 --replication-factor 3 --config min.insync.replicas=2

echo "Done creating topics"
echo ""
echo "Current local topics:"
echo "====================="

$KAFKA_HOME/bin/kafka-topics --zookeeper localhost:2181 --list

echo ""
$KAFKA_HOME/bin/kafka-topics --zookeeper localhost:2181 --describe --topic pf-sla-yolo
echo ""
$KAFKA_HOME/bin/kafka-topics --zookeeper localhost:2181 --describe --topic pf-sla-throughput
echo ""
$KAFKA_HOME/bin/kafka-topics --zookeeper localhost:2181 --describe --topic pf-sla-durable
