package com.oracletrial.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body shared by {@code /api/search} and {@code /api/ask}.
 *
 * <p>Both endpoints only need one thing from the caller: the
 * natural-language question, so they share this single request shape
 * instead of having two near-identical classes.</p>
 */
@Getter
@Setter
public class AskRequest {

    /** The user's natural-language question, e.g. "What is the annual leave policy?". */
    @NotBlank(message = "question must not be blank")
    private String question;
}
