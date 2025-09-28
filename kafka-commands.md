# 🚀 Kafka Commands Reference for Chronos

## 🔧 Container Access
```bash
# Access Kafka container
docker exec -it chronos-kafka bash
```

## 📝 Topic Management

### List All Topics
```bash
docker exec -it chronos-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Create Topic
```bash
docker exec -it chronos-kafka kafka-topics --bootstrap-server localhost:9092 --create --topic my-topic --partitions 3 --replication-factor 1
```

### Describe Topic
```bash
docker exec -it chronos-kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic chronos-test-topic
```

### Delete Topic
```bash
docker exec -it chronos-kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic my-topic
```

## 📤 Producer Commands

### Console Producer (Interactive)
```bash
docker exec -it chronos-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic chronos-test-topic
```

### Producer with Key
```bash
docker exec -it chronos-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic chronos-test-topic --property "key.separator=:" --property "parse.key=true"
```

### Producer Performance Test
```bash
docker exec -it chronos-kafka kafka-producer-perf-test --topic chronos-test-topic --num-records 1000 --record-size 100 --throughput 10 --producer-props bootstrap.servers=localhost:9092
```

## 📥 Consumer Commands

### Console Consumer (from beginning)
```bash
docker exec -it chronos-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic chronos-test-topic --from-beginning
```

### Console Consumer (latest messages only)
```bash
docker exec -it chronos-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic chronos-test-topic
```

### Consumer with Key and Timestamp
```bash
docker exec -it chronos-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic chronos-test-topic --from-beginning --property print.key=true --property print.timestamp=true --property key.separator=":"
```

### Consumer Group
```bash
docker exec -it chronos-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic chronos-test-topic --group my-consumer-group
```

## 👥 Consumer Group Management

### List Consumer Groups
```bash
docker exec -it chronos-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

### Describe Consumer Group
```bash
docker exec -it chronos-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group chronos-consumer-group
```

### Reset Consumer Group Offset
```bash
docker exec -it chronos-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group my-group --reset-offsets --to-earliest --topic chronos-test-topic --execute
```

## 📊 Monitoring Commands

### Check Topic Offsets
```bash
docker exec -it chronos-kafka kafka-run-class kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic chronos-test-topic
```

### Log Segments
```bash
docker exec -it chronos-kafka kafka-log-dirs --bootstrap-server localhost:9092 --describe --json
```

### Broker Configuration
```bash
docker exec -it chronos-kafka kafka-configs --bootstrap-server localhost:9092 --entity-type brokers --entity-name 1 --describe
```

## 🔍 Message Inspection

### Dump Log Segments
```bash
docker exec -it chronos-kafka kafka-dump-log --files /var/lib/kafka/data/chronos-test-topic-0/00000000000000000000.log --print-data-log
```

### Verify Consumer Lag
```bash
docker exec -it chronos-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group chronos-consumer-group --verbose
```

## 🚀 Quick Test Commands

### 1. List topics to see our chronos-test-topic
```bash
docker exec -it chronos-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### 2. Read messages from our topic
```bash
docker exec -it chronos-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic chronos-test-topic --from-beginning
```

### 3. Check topic details
```bash
docker exec -it chronos-kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic chronos-test-topic
```

### 4. Send a test message
```bash
echo "Test message from command line" | docker exec -i chronos-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic chronos-test-topic
```

## 🔧 External Access (from host machine)

### Producer (External)
```bash
echo "External message" | docker exec -i chronos-kafka kafka-console-producer --bootstrap-server localhost:29092 --topic chronos-test-topic
```

### Consumer (External)
```bash
docker exec -it chronos-kafka kafka-console-consumer --bootstrap-server localhost:29092 --topic chronos-test-topic --from-beginning
```

## 📈 Performance Testing

### Producer Performance
```bash
docker exec -it chronos-kafka kafka-producer-perf-test --topic chronos-test-topic --num-records 10000 --record-size 1024 --throughput 1000 --producer-props bootstrap.servers=localhost:9092
```

### Consumer Performance
```bash
docker exec -it chronos-kafka kafka-consumer-perf-test --bootstrap-server localhost:9092 --topic chronos-test-topic --messages 10000
```
