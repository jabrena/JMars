package info.jab.corewars;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ResourceWarriorsTest {
    private static final Path TEST_WARRIORS = Path.of("src", "test", "resources", "warriors");

    @Test
    void testResourceWarriorsAssemble() throws IOException {
        List<Path> warriors = redFiles();

        assertFalse(warriors.isEmpty());
        for (Path warrior : warriors) {
            assertDoesNotThrow(
                () -> new RedcodeAssembler(warrior.toString()).parse(),
                () -> "Could not assemble " + warrior
            );
        }
    }

    private static List<Path> redFiles() throws IOException {
        try (var stream = Files.list(TEST_WARRIORS)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".red"))
                .sorted()
                .toList();
        }
    }

}
