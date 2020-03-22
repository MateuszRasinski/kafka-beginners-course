package com.github.mateuszrasinski.udemy.kafka.beginnerscourse.basics;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static java.time.temporal.ChronoUnit.MILLIS;

class ConsumerDemoWithThread {
    public static void main(String[] args) {
        new ConsumerDemoWithThread().run();
    }

    private void run() {
        Logger logger = LoggerFactory.getLogger(ConsumerDemoWithThread.class);

        String bootstrapServers = "localhost:9092";
        String groupId = "my-sixth-application";
        String topic = "first_topic";

        // latch for dealing with multiple threads
        CountDownLatch latch = new CountDownLatch(1);

        logger.info("Creating the consumer thread");
        ConsumerRunnable myConsumerRunnable = new ConsumerRunnable(
                bootstrapServers,
                groupId,
                topic,
                latch
        );

        // start the thread
        Thread myThread = new Thread(myConsumerRunnable);
        myThread.start();

        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Caught shutdown hook");
            myConsumerRunnable.shutdown();
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }
            logger.info("Application has exited");
        }));

        // wait until latch counts to zero (in other threads)
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.info("Application got interrupted", e);
        } finally {
            logger.info("Application is closing");
        }
    }

    public static class ConsumerRunnable implements Runnable {

        private final CountDownLatch latch;
        private final KafkaConsumer<String, String> consumer;
        private static final Logger logger = LoggerFactory.getLogger(ConsumerRunnable.class);

        public ConsumerRunnable(
                String bootstrapServers,
                String groupId,
                String topic,
                CountDownLatch latch) {
            this.latch = latch;

            // create consumer properties
            Properties properties = new Properties();
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");


            // create consumer
            consumer = new KafkaConsumer<>(properties);

            // subscribe consumer to our topic(s)
            consumer.subscribe(Collections.singleton(topic));
        }

        @Override
        public void run() {
            try {
                // poll for new data
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.of(100, MILLIS));

                    for (ConsumerRecord<String, String> record : records) {
                        logger.info("Key: {}, Value: {}", record.key(), record.value());
                        logger.info("Partition: {}", record.partition());
                        logger.info("Offset: {}", record.offset());
                    }
                }
            } catch (WakeupException e) {
                logger.info("Received shutdown signal!");
            } finally {
                consumer.close();

                // tell our main code we're done with the customer
                latch.countDown();
            }
        }

        public void shutdown() {
            // the wakeup() method is a special method to interrupt consumer.poll()
            // it will throw the exception WakeUpException
            consumer.wakeup();
        }
    }
}
