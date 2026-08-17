package com.oracletrial.controller;

import com.oracletrial.model.AskRequest;
import com.oracletrial.model.AskResponse;
import com.oracletrial.service.RagService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Plain unit test for {@code /api/search} and {@code /api/ask}. Bean
 * Validation is checked directly against a {@link Validator} instead of
 * through a Spring web context, and the controller itself is built with a
 * mocked {@link RagService} - no Qdrant or OpenAI needed to run this.
 */
class RagControllerTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void blankQuestionFailsValidation() {
        AskRequest request = new AskRequest();
        request.setQuestion("");

        Set<ConstraintViolation<AskRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void missingQuestionFailsValidation() {
        AskRequest request = new AskRequest();

        Set<ConstraintViolation<AskRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void askReturnsAnswerAndSourcesFromRagService() {
        RagService ragService = mock(RagService.class);
        AskResponse mocked = new AskResponse(
                "Employees receive 24 days of annual leave.",
                List.of(new AskResponse.Source("hr-policy.pdf", 2, 1))
        );
        when(ragService.ask(anyString())).thenReturn(mocked);

        RagController controller = new RagController(ragService);
        AskRequest request = new AskRequest();
        request.setQuestion("What is the annual leave policy?");

        AskResponse response = controller.ask(request);

        assertThat(response.getAnswer()).isEqualTo("Employees receive 24 days of annual leave.");
        assertThat(response.getSources()).hasSize(1);
        assertThat(response.getSources().get(0).getDocument()).isEqualTo("hr-policy.pdf");
    }
}
