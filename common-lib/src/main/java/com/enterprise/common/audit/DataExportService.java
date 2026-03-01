package com.enterprise.common.audit;

import java.util.Map;

/**
 * Service interface for GDPR data export and deletion.
 * Each microservice should implement this to handle its specific data.
 */
public interface DataExportService {

    /**
     * Export all personal data for a given user.
     * 
     * @param userId The user ID (or subject ID)
     * @return Map of data (key: entity/category, value: data objects)
     */
    Map<String, Object> exportData(String userId);

    /**
     * Delete (or anonymize) all personal data for a given user.
     * 
     * @param userId The user ID (or subject ID)
     */
    void deleteData(String userId);
}
