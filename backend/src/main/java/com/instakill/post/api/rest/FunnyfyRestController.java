package com.instakill.post.api.rest;

import com.instakill.infrastructure.llm.LocalLlmClient;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FunnyfyRestController {

    private final LocalLlmClient llmClient;

    public FunnyfyRestController(LocalLlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @PostMapping("/funnyfy")
    public FunnyfyResponse funnyfy(@Valid @RequestBody FunnyfyRequest request) {
        return new FunnyfyResponse(llmClient.makeFunnier(request.text()));
    }
}
