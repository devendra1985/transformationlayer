package com.example.transformation.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(name = "app.persistence.enabled", havingValue = "true")
@Service
public class PayloadPersistenceService {
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final String rawTable;
  private final String transformedTable;

  public PayloadPersistenceService(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      @Value("${app.persistence.raw-table:TRANSFORM_RAW}") String rawTable,
      @Value("${app.persistence.transformed-table:TRANSFORM_OUT}") String transformedTable
  ) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.rawTable = rawTable;
    this.transformedTable = transformedTable;
  }

  public void storeRaw(String requestId, Object payload, String status) {
    String json = writeJson(payload);
    jdbcTemplate.update(
        "insert into " + rawTable + " (REQUEST_ID, RAW_PAYLOAD, STATUS, CREATED_AT) values (?, ?, ?, ?)",
        requestId,
        json,
        status,
        new Timestamp(System.currentTimeMillis())
    );
  }

  public void storeTransformed(String requestId, Object payload, String status) {
    String json = writeJson(payload);
    jdbcTemplate.update(
        "insert into " + transformedTable + " (REQUEST_ID, TRANSFORMED_PAYLOAD, STATUS, CREATED_AT) values (?, ?, ?, ?)",
        requestId,
        json,
        status,
        new Timestamp(System.currentTimeMillis())
    );
  }

  private String writeJson(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize payload for persistence", e);
    }
  }
}
