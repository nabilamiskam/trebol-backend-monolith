package org.trebol.product.domain.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainArchitectureTest {
    private static final List<String> FORBIDDEN_TOKENS = List.of(
        "org.springframework.",
        "jakarta.persistence.",
        "javax.persistence.",
        "@Entity",
        "@Service",
        "@Autowired"
    );

    @Test
    void domainPackageShouldNotReferenceFrameworks() throws IOException {
        Path domainRoot = Path.of("src/main/java/org/trebol/product/domain");

        try (Stream<Path> files = Files.walk(domainRoot)) {
            List<Path> javaFiles = files
                .filter(path -> path.toString().endsWith(".java"))
                .toList();

            for (Path file : javaFiles) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                for (String forbiddenToken : FORBIDDEN_TOKENS) {
                    assertTrue(
                        !source.contains(forbiddenToken),
                        () -> "Forbidden token " + forbiddenToken + " found in " + file
                    );
                }
            }
        }
    }
}