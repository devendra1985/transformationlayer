package com.example.transformation.cartridge;

public class CartridgeException extends RuntimeException {
  public CartridgeException(String message) {
    super(message);
  }

  public CartridgeException(String message, Throwable cause) {
    super(message, cause);
  }
}

