#!/bin/bash

echo "Starting Kafka cluster..."
docker-compose up -d

echo "Waiting for Kafka to be ready..."
sleep 10

echo "Kafka cluster is running!"
echo "Kafka broker: localhost:9092"
echo "Zookeeper: localhost:2181"
echo ""
echo "To stop the cluster: docker-compose down"
echo "To view logs: docker-compose logs -f"