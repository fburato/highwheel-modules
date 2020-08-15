package com.github.fburato.highwheelmodules.core.analysis;

public class AnalyserException extends RuntimeException {
    private static final long serialVersionUID = -1769077848917606716L;

    public AnalyserException(String msg) {
        super(msg);
    }

    public AnalyserException(Exception cause) {
        super(cause);
    }
}
