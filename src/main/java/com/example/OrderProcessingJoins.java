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

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * CONCEPT: Stream Joins and Enrichment
 * 
 * This example demonstrates:
 * 1. Stream-Stream joins (orders with payments)
 * 2. Stream-Table joins (enriching orders with customer data)
 * 3. Different join types (inner, left, outer)
 * 4. Join windows for time-based correlation
 * 
 * Key Kafka Streams concepts:
 * - KStream-KStream joins: Join two event streams
 * - KStream-KTable joins: Enrich stream events with reference data
 * - Join windows: Time constraints for correlating events
 * - Co-partitioning: Ensuring data with same key is on same partition
 */
public class OrderProcessingJoins {
    
    // Order data
    public static class Order {
        @JsonProperty("orderId")
        public String orderId;
        
        @JsonProperty("customerId")
        public String customerId;
        
        @JsonProperty("product")
        public String product;
        
        @JsonProperty("amount")
        public double amount;
        
        @JsonProperty("timestamp")
        public long timestamp;
        
        public Order() {}
        
        public Order(String orderId, String customerId, String product, double amount, long timestamp) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.product = product;
            this.amount = amount;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("Order{id='%s', customer='%s', product='%s', amount=$%.2f}", 
                orderId, customerId, product, amount);
        }
    }
    
    // Payment data
    public static class Payment {
        @JsonProperty("orderId")
        public String orderId;
        
        @JsonProperty("paymentMethod")
        public String paymentMethod;
        
        @JsonProperty("amount")
        public double amount;
        
        @JsonProperty("status")
        public String status;
        
        @JsonProperty("timestamp")
        public long timestamp;
        
        public Payment() {}
        
        public Payment(String orderId, String paymentMethod, double amount, String status, long timestamp) {
            this.orderId = orderId;
            this.paymentMethod = paymentMethod;
            this.amount = amount;
            this.status = status;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("Payment{orderId='%s', method='%s', amount=$%.2f, status='%s'}", 
                orderId, paymentMethod, amount, status);
        }
    }
    
    // Customer profile (reference data)
    public static class Customer {
        @JsonProperty("customerId")
        public String customerId;
        
        @JsonProperty("name")
        public String name;
        
        @JsonProperty("email")
        public String email;
        
        @JsonProperty("tier")
        public String tier;
        
        @JsonProperty("creditLimit")
        public double creditLimit;
        
        public Customer() {}
        
        public Customer(String customerId, String name, String email, String tier, double creditLimit) {
            this.customerId = customerId;
            this.name = name;
            this.email = email;
            this.tier = tier;
            this.creditLimit = creditLimit;
        }
        
        @Override
        public String toString() {
            return String.format("Customer{id='%s', name='%s', tier='%s'}", 
                customerId, name, tier);
        }
    }
    
    // Enriched order with payment and customer data
    public static class EnrichedOrder {
        @JsonProperty("order")
        public Order order;
        
        @JsonProperty("payment")
        public Payment payment;
        
        @JsonProperty("customer")
        public Customer customer;
        
        @JsonProperty("orderStatus")
        public String orderStatus;
        
        public EnrichedOrder() {}
        
        public EnrichedOrder(Order order, Payment payment, Customer customer) {
            this.order = order;
            this.payment = payment;
            this.customer = customer;
            
            // Determine order status based on payment
            if (payment != null && "SUCCESS".equals(payment.status)) {
                this.orderStatus = "COMPLETED";
            } else if (payment != null && "FAILED".equals(payment.status)) {
                this.orderStatus = "PAYMENT_FAILED";
            } else {
                this.orderStatus = "PENDING_PAYMENT";
            }
        }
        
        @Override
        public String toString() {
            return String.format("EnrichedOrder{status='%s', order=%s, payment=%s, customer=%s}", 
                orderStatus, order, payment, customer);
        }
    }
    
    // Serdes
    public static Serde<Order> orderSerde() {
        return Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(Order.class));
    }
    
    public static Serde<Payment> paymentSerde() {
        return Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(Payment.class));
    }
    
    public static Serde<Customer> customerSerde() {
        return Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(Customer.class));
    }
    
    public static Serde<EnrichedOrder> enrichedOrderSerde() {
        return Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(EnrichedOrder.class));
    }
    
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-processing-joins");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        StreamsBuilder builder = new StreamsBuilder();
        
        // Create streams and tables
        KStream<String, Order> orderStream = builder
            .stream("orders", Consumed.with(Serdes.String(), orderSerde()));
        
        KStream<String, Payment> paymentStream = builder
            .stream("payments", Consumed.with(Serdes.String(), paymentSerde()));
        
        // Customer data as a KTable (reference data that changes slowly)
        KTable<String, Customer> customerTable = builder
            .table("customers", Consumed.with(Serdes.String(), customerSerde()));
        
        // STREAM-STREAM JOIN: Join orders with payments within a time window
        // This correlates orders with their corresponding payments
        KStream<String, EnrichedOrder> orderWithPayment = orderStream.join(
            paymentStream,
            
            // Value joiner: how to combine order and payment
            (order, payment) -> new EnrichedOrder(order, payment, null),
            
            // Join window: correlate events within 10 minutes
            JoinWindows.of(Duration.ofMinutes(10)),
            
            // Serialization
            StreamJoined.with(Serdes.String(), orderSerde(), paymentSerde())
        );
        
        // STREAM-TABLE JOIN: Enrich orders with customer information
        // This adds customer profile data to each order
        KStream<String, EnrichedOrder> fullyEnrichedOrders = orderWithPayment
            // Re-key by customerId to join with customer table
            .selectKey((orderId, enrichedOrder) -> enrichedOrder.order.customerId)
            
            // Join with customer table
            .join(
                customerTable,
                
                // Value joiner: combine enriched order with customer data
                (enrichedOrder, customer) -> {
                    enrichedOrder.customer = customer;
                    return enrichedOrder;
                },
                
                // Serialization
                Joined.with(Serdes.String(), enrichedOrderSerde(), customerSerde())
            );
        
        // Process different order statuses
        KStream<String, EnrichedOrder>[] statusBranches = fullyEnrichedOrders.branch(
            Named.as("order-status-"),
            (key, order) -> "COMPLETED".equals(order.orderStatus),
            (key, order) -> "PAYMENT_FAILED".equals(order.orderStatus),
            (key, order) -> true // PENDING_PAYMENT
        );
        
        // Completed orders
        statusBranches[0]
            .mapValues(order -> "Order completed successfully: " + order.toString())
            .to("completed-orders", Produced.with(Serdes.String(), Serdes.String()));
        
        // Failed orders
        statusBranches[1]
            .mapValues(order -> "Order payment failed: " + order.toString())
            .to("failed-orders", Produced.with(Serdes.String(), Serdes.String()));
        
        // Pending orders
        statusBranches[2]
            .mapValues(order -> "Order pending payment: " + order.toString())
            .to("pending-orders", Produced.with(Serdes.String(), Serdes.String()));
        
        // LEFT JOIN example: Orders that may not have payments yet
        KStream<String, String> ordersWithOptionalPayments = orderStream.leftJoin(
            paymentStream,
            (order, payment) -> {
                if (payment != null) {
                    return String.format("Order %s has payment: %s", order.orderId, payment.status);
                } else {
                    return String.format("Order %s is waiting for payment", order.orderId);
                }
            },
            JoinWindows.of(Duration.ofMinutes(10)),
            StreamJoined.with(Serdes.String(), orderSerde(), paymentSerde())
        );
        
        ordersWithOptionalPayments.to("order-payment-status", Produced.with(Serdes.String(), Serdes.String()));
        
        // Start the application
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        
        final CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread("order-processing-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            System.out.println("Starting Order Processing with Joins...");
            System.out.println("Send orders to 'orders' topic (key: orderId)");
            System.out.println("Send payments to 'payments' topic (key: orderId)");  
            System.out.println("Send customers to 'customers' topic (key: customerId)");
            System.out.println("View results in: completed-orders, failed-orders, pending-orders");
            System.out.println("Press Ctrl+C to stop");
            
            streams.start();
            latch.await();
        } catch (Throwable e) {
            e.printStackTrace();
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