package com.example.transformation.web;

import com.example.transformation.cartridge.CartridgeException;
import com.example.transformation.cartridge.CartridgeException.ErrorType;
import com.example.transformation.cartridge.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(CartridgeException.class)
  public ResponseEntity<ApiError> handleCartridgeException(CartridgeException e) {
    HttpStatus status = (e.getType() == ErrorType.TECHNICAL)
        ? HttpStatus.INTERNAL_SERVER_ERROR
        : HttpStatus.BAD_REQUEST;
    if (status.is5xxServerError()) {
      log.error("Technical cartridge error", e);
    }
    return ResponseEntity.status(status).body(ApiError.from(e));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.functional(ErrorCodes.code(ErrorCodes.GENERIC_FUNCTIONAL),
            e.getMessage(), null, "VALIDATION"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleServerError(Exception e) {
    log.error("Unhandled error while processing request", e);
    // Include actual error message for debugging
    String message = "Unexpected error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
    if (e.getCause() != null) {
      message += " | Cause: " + e.getCause().getClass().getSimpleName() + " - " + e.getCause().getMessage();
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.technical(ErrorCodes.code(ErrorCodes.GENERIC_TECHNICAL),
            message, null, "UNKNOWN"));
  }

  public static class ApiError {
    private final String code;
    private final String type;
    private final String message;
    private final String field;
    private final String step;

    public ApiError(String code, String type, String message, String field, String step) {
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

    public static ApiError functional(String code, String message, String field, String step) {
      return new ApiError(code, "FUNCTIONAL", message, field, step);
    }

    public static ApiError technical(String code, String message, String field, String step) {
      return new ApiError(code, "TECHNICAL", message, field, step);
    }

    public static ApiError from(CartridgeException e) {
      if (e.getType() == ErrorType.TECHNICAL) {
        return new ApiError(e.getCode(), "TECHNICAL", e.getMessage(), e.getField(), e.getStep());
      }
      return new ApiError(e.getCode(), "FUNCTIONAL", e.getMessage(), e.getField(), e.getStep());
    }
  }
}

