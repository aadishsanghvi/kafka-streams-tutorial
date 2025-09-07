#!/bin/bash

# Create Kafka topics for the tutorial examples
echo "Creating Kafka topics..."

# Check if kafka is running
docker ps | grep kafka > /dev/null
if [ $? -ne 0 ]; then
    echo "Error: Kafka is not running. Please start Kafka first."
    exit 1
fi

# Function to create topic
create_topic() {
    local topic_name=$1
    local partitions=${2:-3}
    local replication=${3:-1}
    
    echo "Creating topic: $topic_name"
    docker exec kafka kafka-topics --create \
        --bootstrap-server localhost:9092 \
        --topic $topic_name \
        --partitions $partitions \
        --replication-factor $replication \
        --if-not-exists
}

# Topics for Word Count example
create_topic "text-input"
create_topic "word-count-output"

# Topics for Temperature Monitoring example  
create_topic "temperature-readings"
create_topic "temperature-alerts"
create_topic "temperature-stats"

# Topics for Order Processing example
create_topic "orders"
create_topic "payments" 
create_topic "customers"
create_topic "completed-orders"
create_topic "failed-orders"
create_topic "pending-orders"
create_topic "order-payment-status"

echo "All topics created successfully!"
echo ""
echo "List of created topics:"
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092