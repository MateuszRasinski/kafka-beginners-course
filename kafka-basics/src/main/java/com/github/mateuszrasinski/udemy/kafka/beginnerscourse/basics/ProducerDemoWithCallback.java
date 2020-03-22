package com.github.mateuszrasinski.udemy.kafka.beginnerscourse.basics;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

class ProducerDemoWithCallback {
    public static void main(String[] args) {

        final Logger logger = LoggerFactory.getLogger(ProducerDemoWithCallback.class);

        String bootstrapServers = "localhost:9092";

        // create Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // create the producer
        final KafkaProducer<String, String> producer = new KafkaProducer<>(properties);


        for (int i = 0; i < 10; i++) {
            // create a producer record
            ProducerRecord<String, String> record = new ProducerRecord<>("first_topic", "hello world " + i);

            // send data - asynchronous
            producer.send(record, (metadata, exception) -> {
                // executes every time a record is successfully sent or an exception is thrown
                if (exception == null) {
                    // the record was successfully sent
                    logger.info("Received new metadata: \n"
                                    + "Topic: {}\n"
                                    + "Partition: {}\n"
                                    + "Offset: {}\n"
                                    + "Timestamp: {}\n",
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset(),
                            metadata.timestamp()
                    );
                } else {
                    logger.error("Error while producing", exception);
                }
            });
        }

        // flush data
        producer.flush();
        // flush and close producer
        producer.close();
    }
}
