package com.enterprise.integration.connector;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for retrieving connector implementations by type.
 */
@Component
public class ConnectorFactory {

    private final Map<ConnectorType, Connector> connectorMap;

    public ConnectorFactory(List<Connector> connectors) {
        this.connectorMap = connectors.stream()
                .collect(Collectors.toMap(Connector::getType, Function.identity()));
    }

    public Connector getConnector(ConnectorType type) {
        return Optional.ofNullable(connectorMap.get(type))
                .orElseThrow(() -> new IllegalArgumentException("No connector found for type: " + type));
    }
}
