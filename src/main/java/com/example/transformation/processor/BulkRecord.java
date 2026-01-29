package com.example.transformation.processor;

import java.util.LinkedHashMap;
import java.util.Map;

public class BulkRecord {
  private final int index;
  private Map<String, Object> input;
  private Object output;
  private String contentType;
  private BulkError error;

  public BulkRecord(int index, Map<String, Object> input) {
    this.index = index;
    this.input = input;
  }

  public int getIndex() {
    return index;
  }

  public Map<String, Object> getInput() {
    return input;
  }

  public void setInput(Map<String, Object> input) {
    this.input = input;
  }

  public Object getOutput() {
    return output;
  }

  public void setOutput(Object output) {
    this.output = output;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public BulkError getError() {
    return error;
  }

  public void setError(BulkError error) {
    this.error = error;
  }

  public boolean hasError() {
    return error != null;
  }

  public Map<String, Object> toResponse() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("index", index);
    if (error != null) {
      out.put("success", false);
      out.put("error", error);
      return out;
    }
    out.put("success", true);
    out.put("contentType", contentType);
    out.put("body", output);
    return out;
  }
}
