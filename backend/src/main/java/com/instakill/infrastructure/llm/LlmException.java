package com.instakill.infrastructure.llm;

import com.instakill.common.error.InstakillException;

/** The LLM server answered, but with an error or an unusable response. */
public class LlmException extends InstakillException {
    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
