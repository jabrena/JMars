package info.jab.corewars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MarsTest {
    @TempDir
    Path tempDir;

    @Test
    void movProducesWriteForOwningWarrior() throws Exception {
        Mars mars = new Mars(
            writeWarrior("mov.i $0, $1").toString(),
            writeWarrior("dat.f #0, #0").toString()
        );

        Write write = mars.step();

        assertEquals(new Write(1, 1), write);
        assertEquals(0, mars.win);
        assertEquals(0, mars.loss);
        assertEquals(0, mars.tie);
        assertFalse(mars.finished);
    }

    @Test
    void warriorOneWinsWhenWarriorTwoProcessDies() throws Exception {
        Mars mars = new Mars(
            writeWarrior("nop.b $0, $0").toString(),
            writeWarrior("dat.f #0, #0").toString()
        );

        assertNull(mars.step());
        assertNull(mars.step());

        assertEquals(1, mars.win);
        assertEquals(0, mars.loss);
        assertEquals(0, mars.tie);
        assertEquals(1, mars.round);
        assertFalse(mars.finished);
    }

    @Test
    void warriorOneLosesWhenItsOnlyProcessDies() throws Exception {
        Mars mars = new Mars(
            writeWarrior("dat.f #0, #0").toString(),
            writeWarrior("nop.b $0, $0").toString()
        );

        assertNull(mars.step());

        assertEquals(0, mars.win);
        assertEquals(1, mars.loss);
        assertEquals(0, mars.tie);
        assertEquals(1, mars.round);
        assertFalse(mars.finished);
    }

    private Path writeWarrior(String... lines) throws Exception {
        Path path = tempDir.resolve("warrior-" + System.nanoTime() + ".red");
        Files.write(path, java.util.List.of(lines));
        return path;
    }
}
