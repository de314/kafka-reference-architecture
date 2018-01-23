# Kafka Producer SLA Load Test

**Summary:** Kafka is fast; most likely much faster than your application code.

## Setup

### Running Multiple Brokers Locally

#### 1. Setup Environment Vars

Set `$KAFKA_HOME` to the base of the confluent directory, for example:

```bash
export KAFKA_HOME=~/workspace/kafka/confluent-4.0.0
```

#### 2. Configure Confluent Services

```bash
echo "\n# Switch to enable topic deletion or not, default value is false" >> $KAFKA_HOME/etc/kafka/server.properties
echo "delete.topic.enable=true" >> $KAFKA_HOME/etc/kafka/server.properties
```

#### 3. Start Confluent Services

Startup confluent with `$KAFKA_HOME/bin/confluent start`

#### 4. Configure the other brokers

```bash
cp $KAFKA_HOME/etc/kafka/server.properties $KAFKA_HOME/etc/kafka/server-1.properties
echo "\n\n# multi broker configs" >> $KAFKA_HOME/etc/kafka/server-1.properties
echo "broker.id=1" >> $KAFKA_HOME/etc/kafka/server-1.properties
echo "listeners=PLAINTEXT://:9093" >> $KAFKA_HOME/etc/kafka/server-1.properties
echo "log.dirs=/tmp/kafka-logs-1" >> $KAFKA_HOME/etc/kafka/server-1.properties

cp $KAFKA_HOME/etc/kafka/server.properties $KAFKA_HOME/etc/kafka/server-2.properties
echo "\n\n# multi broker configs" >> $KAFKA_HOME/etc/kafka/server-2.properties
echo "broker.id=2" >> $KAFKA_HOME/etc/kafka/server-2.properties
echo "listeners=PLAINTEXT://:9094" >> $KAFKA_HOME/etc/kafka/server-2.properties
echo "log.dirs=/tmp/kafka-logs-2" >> $KAFKA_HOME/etc/kafka/server-2.properties
```

#### 5. Startup the other brokers

You will need to run these in separate terminal tabs/windows
```bash
$KAFKA_HOME/bin/kafka-server-start $KAFKA_HOME/etc/kafka/server-1.properties 
$KAFKA_HOME/bin/kafka-server-start $KAFKA_HOME/etc/kafka/server-2.properties 
```

### Creating the test topics

Run the creation script and verify the output matches

````bash
./setup-load-topics.sh

Deleting topics: [pf-sla-yolo, pf-sla-throughput, pf-sla-durable]...
================
Done deleting topics

Creating topics: [pf-sla-yolo, pf-sla-throughput, pf-sla-durable]...
================
Created topic "pf-sla-yolo".
Created topic "pf-sla-throughput".
Created topic "pf-sla-durable".
Done creating topics

Current local topics:
=====================
__confluent.support.metrics
__consumer_offsets
_schemas
connect-configs
connect-offsets
connect-statuses
pf-ref-messages
pf-sla-durable
pf-sla-throughput
pf-sla-yolo

Topic:pf-sla-yolo	PartitionCount:2	ReplicationFactor:2	Configs:min.insync.replicas=1
	Topic: pf-sla-yolo	Partition: 0	Leader: 2	Replicas: 2,1	Isr: 2,1
	Topic: pf-sla-yolo	Partition: 1	Leader: 0	Replicas: 0,2	Isr: 0,2

Topic:pf-sla-throughput	PartitionCount:2	ReplicationFactor:3	Configs:min.insync.replicas=2
	Topic: pf-sla-throughput	Partition: 0	Leader: 1	Replicas: 1,2,0	Isr: 1,2,0
	Topic: pf-sla-throughput	Partition: 1	Leader: 2	Replicas: 2,0,1	Isr: 2,0,1

Topic:pf-sla-durable	PartitionCount:2	ReplicationFactor:3	Configs:min.insync.replicas=2
	Topic: pf-sla-durable	Partition: 0	Leader: 2	Replicas: 2,0,1	Isr: 2,0,1
	Topic: pf-sla-durable	Partition: 1	Leader: 0	Replicas: 0,1,2	Isr: 0,1,2
````

#### Errors

```bash
Error while executing topic command : Replication factor: 3 larger than available brokers: 2.
[2018-01-23 14:28:00,040] ERROR org.apache.kafka.common.errors.InvalidReplicationFactorException: Replication factor: 3 larger than available brokers: 2.
 (kafka.admin.TopicCommand$)
```

This just means that the brokers did not register with Zookeeper correctly. You need to shut down
ALL of the kakfa servers and start over.

## Execution

### Configuration

You can set `pf.reference.kafka.sla.loadtest.iterations` in `src/main/resources/application.properties`

### Running

`./gradlew bootRun`

### Results

First ensure that one of the producer has config `acks=1` and the other has config `acks=all`

You can view the load results in lines formatted as follows

```
2018-01-23 13:55:03.573  INFO 70490 --- [           main] c.b.p.r.kafka.ReferenceKafkaApplication  : Test for pf-sla-durable with 100000 messages
2018-01-23 13:55:03.575  INFO 70490 --- [           main] c.b.p.r.kafka.ReferenceKafkaApplication  :    duration=2989ms avg
2018-01-23 13:55:03.577  INFO 70490 --- [           main] c.b.p.r.kafka.ReferenceKafkaApplication  :    avg latency=0.02989ms
```

#### Local Results on my Mac Book Pro

**50k Iterations**

| Topic | Partitions | Replication | ISR | ACKS | Msg Latency | m\s |
|-------|:----------:|:-----------:|:---:|:----:|:-----------:|:---:|
| pf-sla-durable | 2 | 3 | 2 | `all` | 0.02354 ms | 42480 |
| pf-sla-throughput | 2 | 3 | 1 | 1 | 0.04094 ms | 22956 |
| pf-sla-yolo | 2 | 2 | 1 | 1| 0.04356 ms | 36075 |

**250K Iterations**

| Topic | Partitions | Replication | ISR | ACKS | Msg Latency | m\s |
|-------|:----------:|:-----------:|:---:|:----:|:-----------:|:---:|
| pf-sla-durable | 2 | 3 | 2 | `all` | 0.019168 ms | 52170 |
| pf-sla-throughput | 2 | 3 | 1 | 1 | 0.03442 ms | 29052 |
| pf-sla-yolo | 2 | 2 | 1 | 1| 0.02342 ms | 42698 |

**750K Iterations**

| Topic | Partitions | Replication | ISR | ACKS | Msg Latency | m\s |
|-------|:----------:|:-----------:|:---:|:----:|:-----------:|:---:|
| pf-sla-durable | 2 | 3 | 2 | `all` | 0.018996 ms | 52642 |
| pf-sla-throughput | 2 | 3 | 1 | 1 | 0.038984 ms | 25651 |
| pf-sla-yolo | 2 | 2 | 1 | 1| 0.026968 ms | 37080 |

**750K Iterations (run yolo first)**

| Topic | Partitions | Replication | ISR | ACKS | Msg Latency | m\s |
|-------|:----------:|:-----------:|:---:|:----:|:-----------:|:---:|
| pf-sla-yolo | 2 | 2 | 1 | 1| 0.0231413 ms | 43212 |
| pf-sla-throughput | 2 | 3 | 1 | 1 | 0.037824 ms | 26438 |
| pf-sla-durable | 2 | 3 | 2 | `all` | 0.021286 ms | 46977 |


#### Analysis

I am still not sure why `durable` is faster than `yolo`. Durable has

1. an additional replica
1. an additional required in-sync-replica
1. requires 2 additional acks for a successful publish

It may be related to how the partitions are allocated to each broker, and which node is the
leader.

#### Disclaimer

There results are obtained using a development machine. In production each broker will be running
on a different machine, which will introduce additional network latency. Also, the brokers will
be running much closer to CPU, MEM, and DISK capacity.

Therefore, is it not expected that the metrics collected in the production environment will match
the numbers provided in this document, or local test. However, it is expected that the ratio 
between the observed performance is reflected in deployed environments.