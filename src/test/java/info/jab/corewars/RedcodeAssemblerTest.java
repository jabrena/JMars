package info.jab.corewars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RedcodeAssemblerTest {
    @TempDir
    Path tempDir;

    @Test
    void parsesInstructionsLabelsEqusForsAndStartAddress() throws Exception {
        Path warrior = writeWarrior(
            "step equ 4",
            "where dat.f #0, #0",
            "org 1",
            "token equ #step + 1",
            "start mov.i token, >where",
            "for 2",
            "nop.b $0, $0",
            "rof",
            "end"
        );

        ParsedWarrior parsed = new RedcodeAssembler(warrior.toString()).parse();

        assertEquals(4, parsed.instructions().size());
        assertEquals(1, parsed.start());

        Instruction dat = parsed.instructions().getFirst();
        assertEquals(new Instruction(Opcode.DAT, Modifier.F, Mode.IMMEDIATE, 0, Mode.IMMEDIATE, 0), dat);

        Instruction mov = parsed.instructions().get(1);
        assertEquals(Opcode.MOV, mov.opcode());
        assertEquals(Modifier.I, mov.modifier());
        assertEquals(Mode.IMMEDIATE, mov.aMode());
        assertEquals(5, mov.aField());
        assertEquals(Mode.B_POSTINC, mov.bMode());
        assertEquals(CoreWarConfig.CORESIZE - 1, mov.bField());

        assertEquals(Opcode.NOP, parsed.instructions().get(2).opcode());
        assertEquals(Opcode.NOP, parsed.instructions().get(3).opcode());
    }

    @Test
    void supportsEveryAddressingMode() throws Exception {
        Path warrior = writeWarrior(
            "mov.i #1, $2",
            "mov.i *1, @2",
            "mov.i {1, }2",
            "mov.i <1, >2"
        );

        List<Instruction> instructions = new RedcodeAssembler(warrior.toString()).parse().instructions();

        assertEquals(Mode.IMMEDIATE, instructions.get(0).aMode());
        assertEquals(Mode.DIRECT, instructions.get(0).bMode());
        assertEquals(Mode.A_INDIRECT, instructions.get(1).aMode());
        assertEquals(Mode.B_INDIRECT, instructions.get(1).bMode());
        assertEquals(Mode.A_PREDEC, instructions.get(2).aMode());
        assertEquals(Mode.A_POSTINC, instructions.get(2).bMode());
        assertEquals(Mode.B_PREDEC, instructions.get(3).aMode());
        assertEquals(Mode.B_POSTINC, instructions.get(3).bMode());
    }

    @Test
    void appliesDefaultModifiers() throws Exception {
        Path warrior = writeWarrior(
            "dat #0, #0",
            "mov 0, 1",
            "add #1, 2",
            "jmp 0"
        );

        List<Instruction> instructions = new RedcodeAssembler(warrior.toString()).parse().instructions();

        assertEquals(Modifier.F, instructions.get(0).modifier());
        assertEquals(Modifier.I, instructions.get(1).modifier());
        assertEquals(Modifier.F, instructions.get(2).modifier());
        assertEquals(Modifier.B, instructions.get(3).modifier());
    }

    @Test
    void rejectsInvalidWarriors() throws Exception {
        assertThrows(
            IllegalArgumentException.class,
            () -> new RedcodeAssembler(writeWarrior("; only comments").toString()).parse()
        );

        IllegalArgumentException missingRof = assertThrows(
            IllegalArgumentException.class,
            () -> new RedcodeAssembler(writeWarrior("for 1", "nop.b 0, 0").toString()).parse()
        );
        assertTrue(String.valueOf(missingRof.getMessage()).contains("FOR without ROF"));

        IllegalArgumentException unknownSymbol = assertThrows(
            IllegalArgumentException.class,
            () -> new RedcodeAssembler(writeWarrior("mov.i unknown, 0").toString()).parse()
        );
        assertTrue(String.valueOf(unknownSymbol.getMessage()).contains("Unknown symbol"));
    }

    private Path writeWarrior(String... lines) throws Exception {
        Path path = tempDir.resolve("warrior-" + System.nanoTime() + ".red");
        Files.write(path, List.of(lines));
        return path;
    }
}
