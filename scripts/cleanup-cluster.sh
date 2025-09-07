#!/bin/bash

# Cleanup and stop Kafka cluster
echo "🛑 Cleaning up Kafka cluster..."

# Function to check if Docker is running
check_docker() {
    if ! docker info >/dev/null 2>&1; then
        echo "❌ Docker is not running. Please start Docker first."
        exit 1
    fi
}

# Function to stop running Kafka Streams applications
stop_streams_apps() {
    echo "Stopping any running Kafka Streams applications..."
    
    # Find and kill Java processes running Kafka Streams examples
    pkill -f "com.example.WordCountExample" 2>/dev/null || true
    pkill -f "com.example.TemperatureMonitoring" 2>/dev/null || true  
    pkill -f "com.example.OrderProcessingJoins" 2>/dev/null || true
    
    # Also kill any Maven exec processes
    pkill -f "exec-maven-plugin" 2>/dev/null || true
    
    echo "✅ Kafka Streams applications stopped"
}

# Function to clean up Kafka topics and data
cleanup_kafka_data() {
    echo "Cleaning up Kafka topics and data..."
    
    # Check if Kafka container is running
    if docker ps | grep kafka > /dev/null; then
        echo "Deleting all tutorial topics..."
        
        # List of topics to delete
        topics=(
            "text-input"
            "word-count-output"
            "temperature-readings"
            "temperature-alerts"
            "temperature-stats"
            "orders"
            "payments"
            "customers"
            "completed-orders"
            "failed-orders"
            "pending-orders"
            "order-payment-status"
        )
        
        for topic in "${topics[@]}"; do
            docker exec kafka kafka-topics --delete \
                --bootstrap-server localhost:9092 \
                --topic $topic 2>/dev/null || true
        done
        
        # Delete any auto-created internal topics
        echo "Cleaning up internal Kafka Streams topics..."
        docker exec kafka kafka-topics --list --bootstrap-server localhost:9092 | \
            grep -E "(word-count-tutorial|temperature-monitoring|order-processing)" | \
            xargs -I {} docker exec kafka kafka-topics --delete \
                --bootstrap-server localhost:9092 --topic {} 2>/dev/null || true
                
        echo "✅ Kafka topics cleaned up"
    else
        echo "ℹ️  Kafka container not running, skipping topic cleanup"
    fi
}

# Function to stop Docker containers
stop_containers() {
    echo "Stopping Docker containers..."
    
    # Go to parent directory where docker-compose.yml is located
    cd ..
    
    if [ -f "docker-compose.yml" ]; then
        docker-compose down -v --remove-orphans
        echo "✅ Docker containers stopped and volumes removed"
    else
        echo "⚠️  docker-compose.yml not found in parent directory"
        echo "Manually stopping kafka containers..."
        docker stop kafka zookeeper 2>/dev/null || true
        docker rm kafka zookeeper 2>/dev/null || true
        echo "✅ Kafka containers stopped"
    fi
    
    # Return to tutorial directory
    cd kafka-streams-tutorial
}

# Function to clean up local state
cleanup_local_state() {
    echo "Cleaning up local Kafka Streams state..."
    
    # Remove Kafka Streams state directories
    rm -rf /tmp/kafka-streams/ 2>/dev/null || true
    rm -rf /var/folders/*/T/kafka-streams* 2>/dev/null || true
    rm -rf target/ 2>/dev/null || true
    
    echo "✅ Local state cleaned up"
}

# Function to show cleanup summary
show_summary() {
    echo ""
    echo "🎉 Cleanup completed!"
    echo ""
    echo "What was cleaned up:"
    echo "  ✅ Stopped all Kafka Streams applications"
    echo "  ✅ Deleted all tutorial Kafka topics"
    echo "  ✅ Stopped and removed Docker containers"
    echo "  ✅ Removed Docker volumes"
    echo "  ✅ Cleaned up local Kafka Streams state"
    echo ""
    echo "To restart the cluster, run:"
    echo "  cd .. && docker-compose up -d && cd kafka-streams-tutorial"
    echo "  ./scripts/setup-topics.sh"
}

# Main execution
main() {
    check_docker
    stop_streams_apps
    cleanup_kafka_data
    stop_containers
    cleanup_local_state
    show_summary
}

# Run main function
main