package com.oracle.trial;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

/**
 * Entry point for "The Oracle's Trial" backend.
 *
 * Before Spring starts, we load the .env file (which lives in the backend/
 * folder, next to pom.xml) and copy its values into Java system properties.
 * This lets application.properties reference ${OPENAI_API_KEY} without the
 * key ever being hard-coded or committed to Git.
 *
 * Depending on how you run the app, the working directory can differ:
 *  - `mvn spring-boot:run` from inside backend/      -> working dir = backend/
 *  - Running the main class from an IDE at the        -> working dir = the
 *    project root (common with VS Code / IntelliJ)       project root
 * So we look for .env in a few likely places instead of assuming just one.
 */
@SpringBootApplication
public class OracleTrialApplication {

    private static final String[] ENV_DIR_CANDIDATES = {
            ".",         // running from backend/ (e.g. `mvn spring-boot:run`)
            "backend",   // running from the project root
            ".."         // running from a nested folder, e.g. backend/target
    };

    public static void main(String[] args) {
        loadDotenvIfPresent();
        SpringApplication.run(OracleTrialApplication.class, args);
    }

    private static void loadDotenvIfPresent() {
        for (String dir : ENV_DIR_CANDIDATES) {
            File envFile = new File(dir, ".env");
            if (envFile.isFile()) {
                Dotenv dotenv = Dotenv.configure()
                        .directory(dir)
                        .ignoreIfMissing()
                        .load();
                dotenv.entries().forEach(entry ->
                        System.setProperty(entry.getKey(), entry.getValue())
                );
                return;
            }
        }
        // No .env found anywhere obvious - that's fine as long as
        // OPENAI_API_KEY was exported as a real environment variable instead.
        // If it wasn't, EmbeddingService/AnswerService will fail fast below
        // with a clear message explaining what to do.
    }
}
