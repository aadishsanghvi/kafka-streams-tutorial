#!/bin/bash

# Generate sample data for Kafka Streams tutorial

echo "Generating sample data..."

# Function to send message to topic
send_message() {
    local topic=$1
    local key=$2
    local message=$3
    
    echo "$key:$message" | docker exec -i kafka kafka-console-producer \
        --bootstrap-server localhost:9092 \
        --topic $topic \
        --property "parse.key=true" \
        --property "key.separator=:"
}

# Generate sample text data for Word Count example
echo "Generating text data..."
send_message "text-input" "msg1" "kafka streams is awesome"
send_message "text-input" "msg2" "streams processing with kafka"  
send_message "text-input" "msg3" "kafka kafka kafka streams streams"
send_message "text-input" "msg4" "real time data processing"
send_message "text-input" "msg5" "apache kafka streams tutorial"

sleep 2

# Generate temperature readings
echo "Generating temperature data..."
timestamp=$(date +%s000)

send_message "temperature-readings" "sensor1" "{\"sensorId\":\"sensor1\",\"temperature\":25.5,\"timestamp\":$timestamp,\"location\":\"office\"}"

timestamp=$((timestamp + 60000))
send_message "temperature-readings" "sensor2" "{\"sensorId\":\"sensor2\",\"temperature\":32.0,\"timestamp\":$timestamp,\"location\":\"warehouse\"}"

timestamp=$((timestamp + 60000))  
send_message "temperature-readings" "sensor1" "{\"sensorId\":\"sensor1\",\"temperature\":26.2,\"timestamp\":$timestamp,\"location\":\"office\"}"

timestamp=$((timestamp + 60000))
send_message "temperature-readings" "sensor3" "{\"sensorId\":\"sensor3\",\"temperature\":35.5,\"timestamp\":$timestamp,\"location\":\"server-room\"}"

sleep 2

# Generate customer data (reference data)
echo "Generating customer data..."
send_message "customers" "cust001" "{\"customerId\":\"cust001\",\"name\":\"John Doe\",\"email\":\"john@example.com\",\"tier\":\"GOLD\",\"creditLimit\":10000.0}"
send_message "customers" "cust002" "{\"customerId\":\"cust002\",\"name\":\"Jane Smith\",\"email\":\"jane@example.com\",\"tier\":\"SILVER\",\"creditLimit\":5000.0}"
send_message "customers" "cust003" "{\"customerId\":\"cust003\",\"name\":\"Bob Wilson\",\"email\":\"bob@example.com\",\"tier\":\"BRONZE\",\"creditLimit\":2000.0}"

sleep 2

# Generate order and payment data
echo "Generating order and payment data..."
current_time=$(date +%s000)

# Order 1 with successful payment
send_message "orders" "order001" "{\"orderId\":\"order001\",\"customerId\":\"cust001\",\"product\":\"Laptop\",\"amount\":999.99,\"timestamp\":$current_time}"
sleep 1
send_message "payments" "order001" "{\"orderId\":\"order001\",\"paymentMethod\":\"CREDIT_CARD\",\"amount\":999.99,\"status\":\"SUCCESS\",\"timestamp\":$((current_time + 5000))}"

sleep 2

# Order 2 with failed payment  
current_time=$((current_time + 10000))
send_message "orders" "order002" "{\"orderId\":\"order002\",\"customerId\":\"cust002\",\"product\":\"Phone\",\"amount\":599.99,\"timestamp\":$current_time}"
sleep 1
send_message "payments" "order002" "{\"orderId\":\"order002\",\"paymentMethod\":\"DEBIT_CARD\",\"amount\":599.99,\"status\":\"FAILED\",\"timestamp\":$((current_time + 3000))}"

sleep 2

# Order 3 without payment (will timeout in join window)
current_time=$((current_time + 15000))
send_message "orders" "order003" "{\"orderId\":\"order003\",\"customerId\":\"cust003\",\"product\":\"Tablet\",\"amount\":399.99,\"timestamp\":$current_time}"

echo "Sample data generation completed!"
echo ""
echo "You can now run the Kafka Streams applications to see the processing in action."