package com.instakill.post.api.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FunnyfyRequest(
        @NotBlank @Size(max = 1000) String text
) {
}
