package com.instakill.infrastructure.llm;

import com.instakill.common.error.InstakillException;

/** The LLM server could not be reached or did not answer in time. */
public class LlmUnavailableException extends InstakillException {
    private final boolean timeout;

    public LlmUnavailableException(String message, boolean timeout, Throwable cause) {
        super(message, cause);
        this.timeout = timeout;
    }

    public boolean isTimeout() {
        return timeout;
    }
}
