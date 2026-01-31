package com.enterprise.process.config;

import com.enterprise.process.proto.ProcessRequestProto;
import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for Process Management Service (Consumer Side)
 *
 * Uses Protobuf for message serialization.
 *
 * Topology:
 * - Exchange: business.events (Topic Exchange)
 * - Queue: process.request.queue
 * - Routing Key: process.request.create
 *
 * Dead Letter Queue (DLQ):
 * - Exchange: business.events.dlx (Direct Exchange)
 * - Queue: process.request.dlq
 * - Routing Key: process.request.dlq
 */
@Configuration
public class RabbitMQConfig {

    // Main Exchange & Queue
    public static final String EXCHANGE_NAME = "business.events";
    public static final String QUEUE_NAME = "process.request.queue";
    public static final String ROUTING_KEY = "process.request.create";

    // Dead Letter Exchange & Queue
    public static final String DLX_EXCHANGE_NAME = "business.events.dlx";
    public static final String DLQ_QUEUE_NAME = "process.request.dlq";
    public static final String DLQ_ROUTING_KEY = "process.request.dlq";
    
    public static final String ROUTING_KEY_PRODUCT_STATUS = "product.status.change";

    // ==================== Main Exchange ====================

    @Bean
    public TopicExchange businessEventsExchange() {
        return ExchangeBuilder
                .topicExchange(EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    // ==================== Dead Letter Exchange ====================

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange(DLX_EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    // ==================== Main Queue (with DLQ config) ====================

    @Bean
    public Queue processRequestQueue() {
        return QueueBuilder
                .durable(QUEUE_NAME)
                // When message is rejected or expires, send to DLX
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    // ==================== Dead Letter Queue ====================

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(DLQ_QUEUE_NAME)
                .build();
    }

    // ==================== Bindings ====================

    @Bean
    public Binding processRequestBinding(@Qualifier("processRequestQueue") Queue processRequestQueue, 
                                         TopicExchange businessEventsExchange) {
        return BindingBuilder
                .bind(processRequestQueue)
                .to(businessEventsExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding(@Qualifier("deadLetterQueue") Queue deadLetterQueue, 
                                     DirectExchange deadLetterExchange) {
        return BindingBuilder
                .bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(DLQ_ROUTING_KEY);
    }

    // ==================== Protobuf Message Converter ====================

    @Bean
    public MessageConverter protobufMessageConverter() {
        return new ProtobufMessageConverter();
    }

    /**
     * Listener Container Factory with Protobuf converter.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(protobufMessageConverter());
        factory.setDefaultRequeueRejected(false); // Don't requeue, send to DLQ instead
        factory.setPrefetchCount(10);
        return factory;
    }

    /**
     * Custom Protobuf Message Converter for Consumer side.
     */
    public static class ProtobufMessageConverter implements MessageConverter {

        public static final String CONTENT_TYPE_PROTOBUF = "application/x-protobuf";

        @Override
        public org.springframework.amqp.core.Message toMessage(Object object, MessageProperties messageProperties) {
            throw new UnsupportedOperationException("Serialization not supported on Consumer side");
        }

        @Override
        public Object fromMessage(org.springframework.amqp.core.Message message) {
            String contentType = message.getMessageProperties().getContentType();

            if (!CONTENT_TYPE_PROTOBUF.equals(contentType)) {
                throw new MessageConversionException("Expected content type: " + CONTENT_TYPE_PROTOBUF
                        + ", but got: " + contentType);
            }

            try {
                return ProcessRequestProto.ProcessRequest.parseFrom(message.getBody());
            } catch (InvalidProtocolBufferException e) {
                throw new MessageConversionException("Failed to parse Protobuf message", e);
            }
        }
    }
}
