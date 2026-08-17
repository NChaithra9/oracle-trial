package com.oracle.trial.service;

/**
 * Fails fast with a clear, beginner-friendly message when the OpenAI API
 * key hasn't been configured, instead of letting Spring surface a generic
 * "could not resolve placeholder ${OPENAI_API_KEY}" error.
 */
final class OpenAiKeyValidator {

    private OpenAiKeyValidator() {
    }

    static void validate(String apiKey) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your_openai_api_key_here")) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY is not set. Add it to backend/.env as OPENAI_API_KEY=sk-... "
                            + "(create the file next to backend/pom.xml if it's missing), "
                            + "or export OPENAI_API_KEY as an environment variable, then restart the app."
            );
        }
    }
}
