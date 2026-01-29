package com.example.transformation.processor;

import com.example.transformation.cartridge.CartridgeException;
import com.example.transformation.cartridge.ErrorCodes;

public class BulkError {
  private final String code;
  private final String type;
  private final String message;
  private final String field;
  private final String step;

  public BulkError(String code, String type, String message, String field, String step) {
    this.code = code;
    this.type = type;
    this.message = message;
    this.field = field;
    this.step = step;
  }

  public String getCode() {
    return code;
  }

  public String getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public String getField() {
    return field;
  }

  public String getStep() {
    return step;
  }

  public static BulkError from(CartridgeException e) {
    String type = e.getType() == CartridgeException.ErrorType.TECHNICAL ? "TECHNICAL" : "FUNCTIONAL";
    return new BulkError(e.getCode(), type, e.getMessage(), e.getField(), e.getStep());
  }

  public static BulkError functional(String message, String field, String step) {
    return new BulkError(ErrorCodes.code(ErrorCodes.GENERIC_FUNCTIONAL), "FUNCTIONAL", message, field, step);
  }
}
