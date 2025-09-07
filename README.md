# Kafka Streams Tutorial 🚀

[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.5+-green.svg)](https://kafka.apache.org/)

A comprehensive, hands-on tutorial to learn Apache Kafka Streams through practical examples. This tutorial teaches core stream processing concepts through three progressively complex examples that build on each other.

## 🎯 What You'll Learn

- **Stream Processing Fundamentals**: KStream, KTable, transformations, and aggregations
- **Windowing & Time**: Time-based processing and tumbling windows
- **Stream Joins**: Correlate and enrich data from multiple streams
- **Custom Serdes**: Work with JSON data and custom serialization
- **Production Patterns**: Error handling, monitoring, and best practices

## 📚 Tutorial Examples

### 1. 📊 Word Count - Stream Processing Basics
**Concepts**: Basic transformations, aggregations, stateful operations
```java
// Split text → Group by word → Count occurrences → Output results
textLines.flatMapValues(line -> Arrays.asList(line.toLowerCase().split("\\W+")))
         .groupBy((key, word) -> word)
         .count()
```

### 2. 🌡️ Temperature Monitoring - Windowing & Time
**Concepts**: JSON processing, time windows, stream branching, alerts
```java
// Process sensor data → Window by time → Calculate statistics → Generate alerts
temperatureStream.groupBy((key, reading) -> reading.location)
                 .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
                 .aggregate(/* calculate stats */)
```

### 3. 🛒 Order Processing - Joins & Enrichment
**Concepts**: Stream-stream joins, stream-table joins, data correlation
```java
// Join orders with payments → Enrich with customer data → Route by status
orderStream.join(paymentStream, joinOrderWithPayment, joinWindow)
           .join(customerTable, enrichWithCustomerData)
```

## 🛠️ Quick Start

### Prerequisites
- **Java 11+** ([Download](https://adoptopenjdk.net/))
- **Maven 3.6+** ([Download](https://maven.apache.org/download.cgi))
- **Docker & Docker Compose** ([Download](https://docs.docker.com/get-docker/))

### 1. Start Kafka Cluster
```bash
# Clone the repository
git clone https://github.com/yourusername/kafka-streams-tutorial.git
cd kafka-streams-tutorial

# Start Kafka using Docker Compose (from parent directory)
cd .. && docker-compose up -d && cd kafka-streams-tutorial

# Create required topics
./scripts/setup-topics.sh

# Generate sample data
./scripts/generate-sample-data.sh
```

### 2. Run Examples

#### Word Count Example
```bash
# Compile the project
mvn clean compile

# Run the word count application
mvn exec:java -Dexec.mainClass="com.example.WordCountExample"

# In another terminal, view results
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic word-count-output \
  --from-beginning \
  --property print.key=true \
  --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

#### Temperature Monitoring
```bash
mvn exec:java -Dexec.mainClass="com.example.TemperatureMonitoring"

# View alerts
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic temperature-alerts \
  --from-beginning
```

#### Order Processing
```bash
mvn exec:java -Dexec.mainClass="com.example.OrderProcessingJoins"

# View completed orders
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic completed-orders \
  --from-beginning
```

## 🏗️ Project Structure

```
kafka-streams-tutorial/
├── src/main/java/com/example/
│   ├── WordCountExample.java          # 📊 Basic stream processing
│   ├── TemperatureMonitoring.java     # 🌡️ Windowing and time-based processing  
│   └── OrderProcessingJoins.java      # 🛒 Joins and data enrichment
├── scripts/
│   ├── setup-topics.sh               # 🔧 Create Kafka topics
│   ├── generate-sample-data.sh       # 📝 Generate test data
│   └── cleanup-cluster.sh            # 🛑 Stop cluster and cleanup
├── pom.xml                           # 📦 Maven dependencies
├── docker-compose.yml               # 🐳 Kafka cluster (in parent dir)
└── README.md                         # 📖 This file
```

## 🧪 Testing & Experimentation

### Send Custom Data
```bash
# Send text for word counting
echo "your custom message here" | docker exec -i kafka kafka-console-producer \
  --bootstrap-server localhost:9092 --topic text-input

# Send temperature reading (triggers alert if > 30°C)
echo '{"sensorId":"sensor1","temperature":35.0,"timestamp":'$(date +%s000)',"location":"office"}' | \
docker exec -i kafka kafka-console-producer \
  --bootstrap-server localhost:9092 --topic temperature-readings

# Add a customer
echo 'cust123:{"customerId":"cust123","name":"Alice","tier":"GOLD","creditLimit":15000}' | \
docker exec -i kafka kafka-console-producer \
  --bootstrap-server localhost:9092 --topic customers \
  --property "parse.key=true" --property "key.separator=:"
```

### Monitor Application Health
```bash
# View all topics
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Check consumer group status
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group word-count-tutorial
```

## 🎓 Learning Path

1. **Start Simple**: Begin with the Word Count example to understand basic concepts
2. **Add Complexity**: Move to Temperature Monitoring for windowing and time concepts  
3. **Master Advanced**: Complete Order Processing for joins and real-world patterns
4. **Go Production**: Explore monitoring, error handling, and scaling patterns

## 🔧 Key Kafka Streams Concepts Covered

| Concept | Word Count | Temperature | Order Processing |
|---------|------------|-------------|------------------|
| **KStream/KTable** | ✅ | ✅ | ✅ |
| **Transformations** | ✅ | ✅ | ✅ |
| **Aggregations** | ✅ | ✅ | ✅ |
| **JSON Processing** | ❌ | ✅ | ✅ |
| **Windowing** | ❌ | ✅ | ❌ |
| **Stream Branching** | ❌ | ✅ | ✅ |
| **Stream-Stream Joins** | ❌ | ❌ | ✅ |
| **Stream-Table Joins** | ❌ | ❌ | ✅ |
| **Custom Serdes** | ❌ | ✅ | ✅ |

## 🐛 Troubleshooting

### Common Issues

**Application won't start**
```bash
# Check if Kafka is running
docker ps | grep kafka

# Verify topics exist  
./scripts/setup-topics.sh
```

**No data flowing**
```bash
# Generate fresh test data
./scripts/generate-sample-data.sh

# Check topic has data
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 --topic text-input --from-beginning --max-messages 5
```

**State directory locked**
```bash
# Another instance is running - stop it first
# Or clean state directory
./scripts/cleanup-cluster.sh
```

## 🛑 Cleanup & Shutdown

When you're done with the tutorial, clean up everything:

```bash
# Stop all applications and clean up the cluster
./scripts/cleanup-cluster.sh
```

This script will:
- Stop all running Kafka Streams applications
- Delete all tutorial topics
- Stop and remove Docker containers
- Clean up local state directories
- Remove Docker volumes

## 🤝 Contributing

Contributions are welcome! Please feel free to:

- 🐛 Report bugs
- 💡 Suggest new examples  
- 📝 Improve documentation
- ⚡ Add performance optimizations
- 🧪 Add test cases


## 🌟 Acknowledgments

- Built with ❤️ for the Kafka community
- Inspired by real-world streaming use cases
- Special thanks to the Apache Kafka team for the amazing framework

## 📞 Support & Contact

- 📖 [Kafka Streams Documentation](https://kafka.apache.org/documentation/streams/)
- 💬 [Confluent Community](https://forum.confluent.io/)
- 🐛 [Report Issues](https://github.com/yourusername/kafka-streams-tutorial/issues)
- 📧 Contact: [sanghviaadish@gmail.com](mailto:sanghviaadish@gmail.com)

---

⭐ **Found this helpful?** Give it a star to help others discover it!

🔄 **Want updates?** Watch this repository for new examples and improvements.