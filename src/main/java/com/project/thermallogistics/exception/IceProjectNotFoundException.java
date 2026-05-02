package com.project.thermallogistics.exception;

public class IceProjectNotFoundException extends RuntimeException {

    public IceProjectNotFoundException(Long id) {
        super("Ice project not found with id: " + id);
    }

    public IceProjectNotFoundException(String message) {
        super(message);
    }
}
