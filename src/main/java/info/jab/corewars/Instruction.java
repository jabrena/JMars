package info.jab.corewars;

record Instruction(
    Opcode opcode,
    Modifier modifier,
    Mode aMode,
    int aField,
    Mode bMode,
    int bField
) {
    Instruction withA(int v) {
        return new Instruction(opcode, modifier, aMode, CoreWarConfig.wrap(v), bMode, bField);
    }

    Instruction withB(int v) {
        return new Instruction(opcode, modifier, aMode, aField, bMode, CoreWarConfig.wrap(v));
    }

    Instruction copy() {
        return new Instruction(opcode, modifier, aMode, aField, bMode, bField);
    }
}
