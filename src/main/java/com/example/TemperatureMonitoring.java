package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.KeyValue;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * CONCEPT: Windowing and Time-Based Processing
 * 
 * This example demonstrates:
 * 1. Processing JSON messages from sensors
 * 2. Time-based windowing (tumbling windows)
 * 3. Aggregations within time windows
 * 4. Alert generation based on thresholds
 * 
 * Key Kafka Streams concepts:
 * - Custom Serdes for JSON data
 * - Windowing (grouping events by time periods)
 * - TimeWindows: Fixed-size, non-overlapping time intervals
 * - Aggregation functions (average, max, min)
 * - Stream filtering and branching
 */
public class TemperatureMonitoring {
    
    // Data class for temperature readings
    public static class TemperatureReading {
        @JsonProperty("sensorId")
        public String sensorId;
        
        @JsonProperty("temperature")
        public double temperature;
        
        @JsonProperty("timestamp")
        public long timestamp;
        
        @JsonProperty("location")
        public String location;
        
        public TemperatureReading() {}
        
        public TemperatureReading(String sensorId, double temperature, long timestamp, String location) {
            this.sensorId = sensorId;
            this.temperature = temperature;
            this.timestamp = timestamp;
            this.location = location;
        }
        
        @Override
        public String toString() {
            return String.format("TemperatureReading{sensorId='%s', temp=%.1f°C, location='%s'}", 
                sensorId, temperature, location);
        }
    }
    
    // Aggregate class for window statistics
    public static class TemperatureStats {
        @JsonProperty("location")
        public String location;
        
        @JsonProperty("count")
        public long count;
        
        @JsonProperty("sum")
        public double sum;
        
        @JsonProperty("average")
        public double average;
        
        @JsonProperty("max")
        public double max;
        
        @JsonProperty("min")
        public double min;
        
        @JsonProperty("windowStart")
        public long windowStart;
        
        @JsonProperty("windowEnd")
        public long windowEnd;
        
        public TemperatureStats() {}
        
        public TemperatureStats(String location) {
            this.location = location;
            this.count = 0;
            this.sum = 0.0;
            this.max = Double.NEGATIVE_INFINITY;
            this.min = Double.POSITIVE_INFINITY;
        }
        
        public TemperatureStats add(double temperature) {
            this.count++;
            this.sum += temperature;
            this.average = sum / count;
            this.max = Math.max(this.max, temperature);
            this.min = Math.min(this.min, temperature);
            return this;
        }
        
        @Override
        public String toString() {
            return String.format("Stats{location='%s', avg=%.1f°C, max=%.1f°C, min=%.1f°C, count=%d}", 
                location, average, max, min, count);
        }
    }
    
    // JSON Serde for TemperatureReading
    public static Serde<TemperatureReading> temperatureReadingSerde() {
        return Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(TemperatureReading.class));
    }
    
    // JSON Serde for TemperatureStats
    public static Serde<TemperatureStats> temperatureStatsSerde() {
        return Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(TemperatureStats.class));
    }
    
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "temperature-monitoring");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        StreamsBuilder builder = new StreamsBuilder();
        
        // Create stream of temperature readings
        KStream<String, TemperatureReading> temperatureStream = builder
            .stream("temperature-readings", Consumed.with(Serdes.String(), temperatureReadingSerde()));
        
        // Branch the stream for different processing
        KStream<String, TemperatureReading>[] branches = temperatureStream.branch(
            Named.as("high-temp-"),
            (key, reading) -> reading.temperature > 30.0,  // High temperature alert
            (key, reading) -> true                         // All other temperatures
        );
        
        KStream<String, TemperatureReading> highTempStream = branches[0];
        KStream<String, TemperatureReading> normalTempStream = branches[1];
        
        // Generate alerts for high temperatures
        highTempStream
            .mapValues(reading -> "ALERT: High temperature detected! " + reading.toString())
            .to("temperature-alerts", Produced.with(Serdes.String(), Serdes.String()));
        
        // Calculate statistics using time windows (5-minute tumbling windows)
        KTable<Windowed<String>, TemperatureStats> temperatureStats = normalTempStream
            // Group by location
            .groupBy((key, reading) -> reading.location, Grouped.with(Serdes.String(), temperatureReadingSerde()))
            
            // Create 5-minute tumbling windows
            .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
            
            // Aggregate temperature readings within each window
            .aggregate(
                // Initializer: create empty stats
                () -> new TemperatureStats(),
                
                // Aggregator: add temperature reading to stats
                (location, reading, stats) -> {
                    if (stats.location == null) {
                        stats.location = location;
                    }
                    return stats.add(reading.temperature);
                },
                
                // Materialized store configuration
                Materialized.with(Serdes.String(), temperatureStatsSerde())
            );
        
        // Write windowed statistics to output topic
        temperatureStats.toStream()
            .map((windowedKey, stats) -> {
                // Extract window information
                stats.windowStart = windowedKey.window().start();
                stats.windowEnd = windowedKey.window().end();
                return KeyValue.pair(windowedKey.key(), stats);
            })
            .to("temperature-stats", Produced.with(Serdes.String(), temperatureStatsSerde()));
        
        // Start the application
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        
        final CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread("temperature-monitoring-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            System.out.println("Starting Temperature Monitoring stream processor...");
            System.out.println("Send temperature readings to 'temperature-readings' topic");
            System.out.println("View alerts in 'temperature-alerts' topic");
            System.out.println("View statistics in 'temperature-stats' topic");
            System.out.println("Press Ctrl+C to stop");
            
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
    
    // Generic JSON Serializer
    public static class JsonSerializer<T> implements Serializer<T> {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public byte[] serialize(String topic, T data) {
            try {
                if (data == null) {
                    return null;
                }
                return objectMapper.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new RuntimeException("Error serializing JSON message", e);
            }
        }
    }
    
    // Generic JSON Deserializer
    public static class JsonDeserializer<T> implements Deserializer<T> {
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final Class<T> type;

        public JsonDeserializer(Class<T> type) {
            this.type = type;
        }

        @Override
        public T deserialize(String topic, byte[] data) {
            try {
                if (data == null) {
                    return null;
                }
                return objectMapper.readValue(data, type);
            } catch (IOException e) {
                throw new RuntimeException("Error deserializing JSON message", e);
            }
        }
    }
}