package com.pal.dipesh.razorpay.common.exception;

import lombok.Getter;

@Getter
public class InvalidStateTransitionException extends RuntimeException {

    private final String fromState;
    private final String transitionEvent;

    public InvalidStateTransitionException(String fromState, String transitionEvent) {
        super(String.format("Invalid state transition from '%s' using event '%s'", fromState, transitionEvent));
        this.fromState = fromState;
        this.transitionEvent = transitionEvent;
    }
}
