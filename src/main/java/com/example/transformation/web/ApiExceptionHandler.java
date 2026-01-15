package com.example.transformation.web;

import com.example.transformation.cartridge.CartridgeException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler({CartridgeException.class, IllegalArgumentException.class})
  public ResponseEntity<Map<String, String>> handleBadRequest(Exception e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
  }
}

