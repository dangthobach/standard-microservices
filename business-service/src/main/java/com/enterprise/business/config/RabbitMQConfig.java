package com.enterprise.business.config;

import com.google.protobuf.Message;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for Business Service (Producer Side)
 *
 * Uses Protobuf for message serialization.
 *
 * Topology:
 * - Exchange: business.events (Topic Exchange)
 * - Routing Key: process.request.create
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "business.events";
    public static final String ROUTING_KEY_PROCESS_REQUEST = "process.request.create";
    
    // Status Update Queue (Listened by Business Service)
    public static final String QUEUE_PRODUCT_STATUS = "product.status.queue";
    public static final String ROUTING_KEY_PRODUCT_STATUS = "product.status.change";

    /**
     * Topic Exchange - allows routing based on routing key patterns.
     * Durable = true: survives broker restart.
     */
    @Bean
    public TopicExchange businessEventsExchange() {
        return ExchangeBuilder
                .topicExchange(EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    @Bean
    public Queue productStatusQueue() {
        return QueueBuilder.durable(QUEUE_PRODUCT_STATUS).build();
    }

    @Bean
    public Binding productStatusBinding(Queue productStatusQueue, TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(productStatusQueue).to(businessEventsExchange).with(ROUTING_KEY_PRODUCT_STATUS);
    }

    /**
     * Protobuf Message Converter for serializing/deserializing Protobuf messages.
     */
    @Bean
    public MessageConverter protobufMessageConverter() {
        return new ProtobufMessageConverter();
    }

    /**
     * RabbitTemplate with Protobuf converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(protobufMessageConverter());
        return rabbitTemplate;
    }

    /**
     * Custom Protobuf Message Converter.
     */
    public static class ProtobufMessageConverter implements MessageConverter {

        public static final String CONTENT_TYPE_PROTOBUF = "application/x-protobuf";

        @Override
        public org.springframework.amqp.core.Message toMessage(Object object, MessageProperties messageProperties) {
            if (!(object instanceof Message)) {
                throw new IllegalArgumentException("Object must be a Protobuf Message");
            }

            Message protoMessage = (Message) object;
            byte[] body = protoMessage.toByteArray();

            messageProperties.setContentType(CONTENT_TYPE_PROTOBUF);
            messageProperties.setHeader("x-protobuf-class", protoMessage.getClass().getName());

            return new org.springframework.amqp.core.Message(body, messageProperties);
        }

        @Override
        public Object fromMessage(org.springframework.amqp.core.Message message) {
            // Not used on Producer side
            throw new UnsupportedOperationException("Deserialization not supported on Producer side");
        }
    }
}
