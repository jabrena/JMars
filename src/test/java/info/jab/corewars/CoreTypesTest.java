package info.jab.corewars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;
import org.junit.jupiter.api.Test;

class CoreTypesTest {
    @Test
    void wrapKeepsAddressesInsideCore() {
        assertEquals(0, CoreWarConfig.wrap(0));
        assertEquals(CoreWarConfig.CORESIZE - 1, CoreWarConfig.wrap(-1));
        assertEquals(0, CoreWarConfig.wrap(CoreWarConfig.CORESIZE));
        assertEquals(1, CoreWarConfig.wrap(CoreWarConfig.CORESIZE + 1));
    }

    @Test
    void instructionHelpersWrapOnlySelectedField() {
        Instruction instruction = new Instruction(
            Opcode.MOV,
            Modifier.I,
            Mode.DIRECT,
            7,
            Mode.IMMEDIATE,
            11
        );

        assertEquals(3, instruction.withA(CoreWarConfig.CORESIZE + 3).aField());
        assertEquals(11, instruction.withA(CoreWarConfig.CORESIZE + 3).bField());
        assertEquals(7, instruction.withB(-2).aField());
        assertEquals(CoreWarConfig.CORESIZE - 2, instruction.withB(-2).bField());

        Instruction copy = instruction.copy();
        assertEquals(instruction, copy);
        assertNotSame(instruction, copy);
    }

    @Test
    void recordsExposeTheirValues() {
        Instruction instruction = new Instruction(
            Opcode.DAT,
            Modifier.F,
            Mode.IMMEDIATE,
            1,
            Mode.DIRECT,
            2
        );

        assertEquals(23, new Operand(23, instruction).address());
        assertEquals(instruction, new Operand(23, instruction).instruction());
        assertEquals(42, new Write(42, 2).address());
        assertEquals(2, new Write(42, 2).owner());
        assertEquals(List.of(instruction), new ParsedWarrior(List.of(instruction), 5).instructions());
        assertEquals(5, new ParsedWarrior(List.of(instruction), 5).start());
    }

    @Test
    void enumsContainExpectedCoreWarVocabulary() {
        assertEquals(17, Opcode.values().length);
        assertEquals(7, Modifier.values().length);
        assertEquals(8, Mode.values().length);

        assertEquals(Opcode.MOV, Opcode.valueOf("MOV"));
        assertEquals(Modifier.AB, Modifier.valueOf("AB"));
        assertEquals(Mode.B_POSTINC, Mode.valueOf("B_POSTINC"));
    }
}
