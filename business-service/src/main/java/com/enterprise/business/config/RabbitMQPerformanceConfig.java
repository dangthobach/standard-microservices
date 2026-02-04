package com.enterprise.business.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Performance Tuning Configuration
 * Optimizes consumer concurrency and prefetch for high throughput
 */
@Configuration
public class RabbitMQPerformanceConfig {

    /**
     * Optimized Rabbit Listener Container Factory
     * 
     * Configuration:
     * - Prefetch Count: 20 (number of messages to prefetch)
     * - Concurrent Consumers: 5 (initial consumer threads)
     * - Max Concurrent Consumers: 20 (max consumer threads under load)
     * 
     * Performance Impact:
     * - Prefetch: Reduces network round-trips, improves throughput
     * - Concurrency: Allows parallel message processing
     * - Auto-scaling: Dynamically adjusts consumers based on queue depth
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        
        // Prefetch configuration
        factory.setPrefetchCount(20); // Prefetch 20 messages per consumer
        
        // Concurrency configuration
        factory.setConcurrentConsumers(5);    // Start with 5 consumers
        factory.setMaxConcurrentConsumers(20); // Scale up to 20 under load
        
        // Auto-startup
        factory.setAutoStartup(true);
        
        // Acknowledge configuration
        factory.setDefaultRequeueRejected(false); // Don't requeue failed messages
    factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.AUTO);
        
        return factory;
    }

    /**
     * RabbitTemplate with performance optimizations
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        
        // Enable publisher confirms for reliability
        template.setMandatory(true);
        
        return template;
    }
}
