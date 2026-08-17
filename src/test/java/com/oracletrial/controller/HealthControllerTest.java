package com.oracletrial.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test for the health endpoint - just calls the controller
 * method directly, no Spring context needed for a one-line method.
 */
class HealthControllerTest {

    @Test
    void healthReturnsRunningMessage() {
        HealthController controller = new HealthController();

        String result = controller.health();

        assertThat(result).isEqualTo("Oracle Trial API is running");
    }
}
