package com.enterprise.integration.connector;

import com.enterprise.integration.dto.ConnectorRequest;
import com.enterprise.integration.dto.ConnectorResponse;
import com.enterprise.integration.entity.ConnectorConfig;

/**
 * Interface for all connector implementations.
 */
public interface Connector {

    /**
     * Execute the connector with the given configuration and request.
     *
     * @param config  The connector configuration (URL, auth, etc.)
     * @param request The request payload and dynamic parameters
     * @return The execution response
     */
    ConnectorResponse execute(ConnectorConfig config, ConnectorRequest request);

    /**
     * Get the supported connector type.
     *
     * @return The connector type
     */
    ConnectorType getType();
}
