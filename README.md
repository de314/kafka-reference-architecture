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
Topic pf-sla-yolo is marked for deletion.
Note: This will have no impact if delete.topic.enable is not set to true.
Topic pf-sla-durable is marked for deletion.
Note: This will have no impact if delete.topic.enable is not set to true.
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
	Topic: pf-sla-yolo	Partition: 0	Leader: 0	Replicas: 0,1	Isr: 0,1
	Topic: pf-sla-yolo	Partition: 1	Leader: 1	Replicas: 1,2	Isr: 1,2

Topic:pf-sla-throughput	PartitionCount:2	ReplicationFactor:3	Configs:min.insync.replicas=2
	Topic: pf-sla-throughput	Partition: 0	Leader: 0	Replicas: 0,2,1	Isr: 0,2,1
	Topic: pf-sla-throughput	Partition: 1	Leader: 1	Replicas: 1,0,2	Isr: 1,0,2

Topic:pf-sla-durable	PartitionCount:2	ReplicationFactor:3	Configs:min.insync.replicas=2
	Topic: pf-sla-durable	Partition: 0	Leader: 0	Replicas: 0,1,2	Isr: 0,1,2
	Topic: pf-sla-durable	Partition: 1	Leader: 1	Replicas: 1,2,0	Isr: 1,2,0
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

**100k Iterations**

| Topic | Partitions | Replication | ISR | ACKS | Duration | Msg Latency | m\s |
|-------|:----------:|:-----------:|:---:|:----:|:--------:|:-----------:|:---:|
| pf-sla-yolo | 2 | 2 | 1 | 1| 5682ms | 0.05682ms | 17,599 |
| pf-sla-throughput | 2 | 3 | 1 | 1 | 3391ms | 0.03391ms | 29,489 |
| pf-sla-durable | 2 | 3 | 2 | `all` | 2989ms | 0.02989ms | 33,456 |

**100k Iterations (Reverse Order)**

| Topic | Partitions | Replication | ISR | ACKS | Duration | Msg Latency | m\s |
|-------|:----------:|:-----------:|:---:|:----:|:--------:|:-----------:|:---:|
| pf-sla-yolo | 2 | 2 | 1 | 1| 2899ms | 0.02899ms | 34,494 |
| pf-sla-throughput | 2 | 3 | 1 | 1 | 4152ms | 0.04152ms | 24,084 |
| pf-sla-durable | 2 | 3 | 2 | `all` | 6295ms | 0.06295ms | 15,885 |

**1M Iterations**

| Topic | Partitions | Replication | ISR | ACKS | Duration | Msg Latency |
|-------|:----------:|:-----------:|:---:|:----:|:--------:|:-----------:|
| pf-sla-yolo | 2 | 2 | 1 | 1| 14553ms | 0.014553ms | 
| pf-sla-throughput | 2 | 3 | 1 | 1 | 17080ms | 0.01708ms |
| pf-sla-durable | 2 | 3 | 2 | `all` | 10982ms | 0.010982ms |

Log output for 100k iterations
```
ProducerConfig values:
        acks = 1
        ...

Test for pf-sla-yolo with 100000 messages
    duration=5682ms avg
    avg latency=0.05682ms

Test for pf-sla-throughput with 100000 messages
    duration=3391ms avg
    avg latency=0.03391ms



ProducerConfig values:
        acks = all
        ...

Test for pf-sla-durable with 100000 messages
    duration=2989ms avg
    avg latency=0.02989ms
```

#### Analysis



#### Disclaimer

There results are obtained using a development machine. In production each broker will be running
on a different machine, which will introduce additional network latency. Also, the brokers will
be running much closer to CPU, MEM, and DISK capacity.

Therefore, is it not expected that the metrics collected in the production environment will match
the numbers provided in this document, or local test. However, it is expected that the ratio 
between the observed performance is reflected in deployed environments.