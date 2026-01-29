package com.example.transformation.cartridge;

public class CartridgeException extends RuntimeException {
  public enum ErrorType {
    FUNCTIONAL,
    TECHNICAL
  }

  private final String code;
  private final ErrorType type;
  private final String field;
  private final String step;

  public CartridgeException(String message) {
    this(ErrorCodes.code(ErrorCodes.GENERIC_FUNCTIONAL), ErrorType.FUNCTIONAL, message, null, null);
  }

  public CartridgeException(String message, Throwable cause) {
    this(ErrorCodes.code(ErrorCodes.GENERIC_TECHNICAL), ErrorType.TECHNICAL, message, cause, null, null);
  }

  public CartridgeException(String code, ErrorType type, String message) {
    this(code, type, message, null, null);
  }

  public CartridgeException(String code, ErrorType type, String message, String field) {
    this(code, type, message, field, null);
  }

  public CartridgeException(String code, ErrorType type, String message, String field, String step) {
    super(message);
    this.code = code;
    this.type = type;
    this.field = field;
    this.step = step;
  }

  public CartridgeException(String code, ErrorType type, String message, Throwable cause, String field, String step) {
    super(message, cause);
    this.code = code;
    this.type = type;
    this.field = field;
    this.step = step;
  }

  public String getCode() {
    return code;
  }

  public ErrorType getType() {
    return type;
  }

  public String getField() {
    return field;
  }

  public String getStep() {
    return step;
  }
}
