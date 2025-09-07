package com.example;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * CONCEPT: Basic Stream Processing
 * 
 * This example demonstrates:
 * 1. Reading from a Kafka topic
 * 2. Processing stream data (splitting text into words)
 * 3. Aggregating data (counting words)
 * 4. Writing results to another topic
 * 
 * Key Kafka Streams concepts:
 * - KStream: A stream of records (infinite sequence of data)
 * - KTable: A changelog stream (table representation)
 * - Serdes: Serializers/Deserializers for data types
 * - Topology: The processing graph of your application
 */
public class WordCountExample {

    public static void main(String[] args) {
        // Configuration for Kafka Streams application
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "word-count-tutorial");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        
        // Start reading from the beginning of the topic for demo purposes
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Create the processing topology
        StreamsBuilder builder = new StreamsBuilder();
        
        // Step 1: Create a stream from the input topic
        KStream<String, String> textLines = builder.stream("text-input");
        
        // Step 2: Process the stream
        KTable<String, Long> wordCounts = textLines
            // Split each line into words (flatMapValues transforms each value into multiple values)
            .flatMapValues(textLine -> Arrays.asList(textLine.toLowerCase().split("\\W+")))
            
            // Group by word (this creates a KGroupedStream)
            .groupBy((key, word) -> word)
            
            // Count occurrences of each word (this creates a KTable)
            .count();

        // Step 3: Write the results to output topic
        // Convert KTable back to KStream to write to topic
        wordCounts.toStream().to("word-count-output", Produced.with(Serdes.String(), Serdes.Long()));

        // Build and start the Kafka Streams application
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        
        // Set up graceful shutdown
        final CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread("word-count-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            System.out.println("Starting Word Count stream processor...");
            System.out.println("Send text messages to 'text-input' topic");
            System.out.println("View word counts in 'word-count-output' topic");
            System.out.println("Press Ctrl+C to stop");
            
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
}