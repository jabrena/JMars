package info.jab.corewars;

import static info.jab.corewars.CoreWarConfig.CORESIZE;
import static info.jab.corewars.CoreWarConfig.FNUMBER;
import static info.jab.corewars.CoreWarConfig.MAXCYCLES;
import static info.jab.corewars.CoreWarConfig.MAXLENGTH;
import static info.jab.corewars.CoreWarConfig.MAXPROCS;
import static info.jab.corewars.CoreWarConfig.ROUNDS;
import static info.jab.corewars.CoreWarConfig.wrap;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.Nullable;

final class Mars {
    private final Instruction[] core = new Instruction[CORESIZE];
    private final int[] owner = new int[CORESIZE];

    private final List<List<Instruction>> warriors;
    private final int[] starts = new int[2];

    private List<ArrayDeque<Integer>> procs;

    int round = 0;
    private int cycle = 0;
    private int war = 0;
    private int prng = FNUMBER - MAXLENGTH;

    int win = 0;
    int loss = 0;
    int tie = 0;

    boolean finished = false;

    Mars(String w1, String w2) {
        ParsedWarrior warrior1 = parseWarrior(w1);
        ParsedWarrior warrior2 = parseWarrior(w2);

        warriors = List.of(
            warrior1.instructions(),
            warrior2.instructions()
        );
        starts[0] = warrior1.start();
        starts[1] = warrior2.start();
        startRound();
    }

    private void startRound() {
        Arrays.fill(owner, 0);

        Instruction dat = new Instruction(
            Opcode.DAT, Modifier.F,
            Mode.DIRECT, 0,
            Mode.DIRECT, 0
        );

        Arrays.fill(core, dat);

        int w2start = prng % (CORESIZE + 1 - 2 * MAXLENGTH) + MAXLENGTH;

        procs = List.of(
            new ArrayDeque<>(),
            new ArrayDeque<>()
        );

        procs.get(0).add(wrap(starts[0]));
        procs.get(1).add(wrap(w2start + starts[1]));

        loadWarrior(0, 0);
        loadWarrior(1, w2start);

        war = round & 1;
        cycle = 0;
    }

    private void loadWarrior(int index, int offset) {
        List<Instruction> warrior = warriors.get(index);
        for (int i = 0; i < warrior.size(); i++) {
            int addr = wrap(offset + i);
            core[addr] = warrior.get(i).copy();
            owner[addr] = index + 1;
        }
    }

    @Nullable Write step() {
        if (finished) return null;

        if (cycle >= MAXCYCLES * 2) {
            tie++;
            nextRound();
            return null;
        }

        if (proc(war).isEmpty()) {
            endRound();
            return null;
        }

        int pc = wrap(proc(war).removeFirst());
        Instruction ir = core[pc].copy();

        Operand a = evalOperand(ir.aMode(), ir.aField(), pc);
        Operand b = evalOperand(ir.bMode(), ir.bField(), pc);
        Write write = null;

        switch (ir.opcode()) {
            case MOV -> {
                owner[b.address()] = war + 1;

                if (ir.modifier() == Modifier.I) {
                    core[b.address()] = a.instruction().copy();
                } else {
                    core[b.address()] = copyFields(ir.modifier(), a.instruction(), core[b.address()]);
                }

                next(pc);
                write = new Write(b.address(), war + 1);
            }

            case SPL -> {
                next(pc);
                if (proc(war).size() < MAXPROCS) {
                    proc(war).addLast(a.address());
                }
            }

            case JMP -> proc(war).addLast(a.address());

            case NOP -> next(pc);

            case ADD, SUB, MUL -> {
                core[b.address()] = arithmetic(ir.opcode(), ir.modifier(), a.instruction(), b.instruction());
                next(pc);
            }

            case DIV, MOD -> {
                Instruction result = divMod(ir.opcode(), ir.modifier(), a.instruction(), b.instruction());
                if (result != null) {
                    core[b.address()] = result;
                    next(pc);
                }
            }

            case JMZ, JMN, DJN -> {
                Instruction test = b.instruction();

                if (ir.opcode() == Opcode.DJN) {
                    Instruction decremented = decrement(ir.modifier(), core[b.address()]);
                    core[b.address()] = decremented;
                    test = decremented;
                }

                boolean zero = true;
                for (Pair p : fields(ir.modifier())) {
                    if (get(test, p.dst()) != 0) {
                        zero = false;
                        break;
                    }
                }

                boolean advance = (ir.opcode() == Opcode.JMZ) ^ zero;
                proc(war).addLast(advance ? wrap(pc + 1) : a.address());
            }

            case SEQ, CMP, SNE -> {
                boolean equal = ir.modifier() == Modifier.I
                    ? a.instruction().equals(b.instruction())
                    : fields(ir.modifier()).stream()
                        .allMatch(p -> get(a.instruction(), p.src()) == get(b.instruction(), p.dst()));

                boolean skip = !((ir.opcode() != Opcode.SNE) ^ equal);
                proc(war).addLast(wrap(pc + (skip ? 2 : 1)));
            }

            case SLT -> {
                boolean less = fields(ir.modifier()).stream()
                    .allMatch(p -> get(a.instruction(), p.src()) < get(b.instruction(), p.dst()));

                proc(war).addLast(wrap(pc + (less ? 2 : 1)));
            }

            case DAT -> {
                // Process dies: do not re-add PC.
            }
        }

        if (proc(war).isEmpty()) {
            endRound();
            return null;
        }

        war ^= 1;
        cycle++;
        return write;
    }

    private void next(int pc) {
        proc(war).addLast(wrap(pc + 1));
    }

    private void endRound() {
        if (proc(0).isEmpty()) loss++;
        else if (proc(1).isEmpty()) win++;
        else tie++;

        nextRound();
    }

    private ArrayDeque<Integer> proc(int index) {
        return procs.get(index);
    }

    private void nextRound() {
        round++;

        prng = (int)(((16807L * (prng % 127773)) - (2836L * (prng / 127773))) % 2147483647);
        if (prng < 0) prng += 2147483647;

        if (round >= ROUNDS) {
            finished = true;
        } else {
            startRound();
        }
    }

    private Operand evalOperand(Mode mode, int field, int pc) {
        if (mode == Mode.IMMEDIATE) {
            return new Operand(pc, core[pc].copy());
        }

        int addr = wrap(pc + field);
        Instruction ins = core[addr];

        if (mode == Mode.DIRECT) {
            return new Operand(addr, ins.copy());
        }

        boolean useB =
            mode == Mode.B_INDIRECT ||
            mode == Mode.B_PREDEC ||
            mode == Mode.B_POSTINC;

        int indirect = useB ? ins.bField() : ins.aField();

        if (mode == Mode.B_PREDEC || mode == Mode.A_PREDEC) {
            indirect = wrap(indirect - 1);
            ins = useB ? ins.withB(indirect) : ins.withA(indirect);
            core[addr] = ins;
        }

        int target = wrap(addr + indirect);
        Instruction result = core[target].copy();

        if (mode == Mode.B_POSTINC || mode == Mode.A_POSTINC) {
            indirect = wrap(indirect + 1);
            core[addr] = useB ? ins.withB(indirect) : ins.withA(indirect);
        }

        return new Operand(target, result);
    }

    private static ParsedWarrior parseWarrior(String file) {
        try {
            return new RedcodeAssembler(file).parse();
        } catch (Exception e) {
            throw new RuntimeException("Could not parse warrior: " + file, e);
        }
    }

    private record Pair(char src, char dst) {
    }

    private static List<Pair> fields(Modifier m) {
        return switch (m) {
            case A -> List.of(new Pair('a', 'a'));
            case B -> List.of(new Pair('b', 'b'));
            case AB -> List.of(new Pair('a', 'b'));
            case BA -> List.of(new Pair('b', 'a'));
            case F, I -> List.of(new Pair('a', 'a'), new Pair('b', 'b'));
            case X -> List.of(new Pair('b', 'a'), new Pair('a', 'b'));
        };
    }

    private static int get(Instruction i, char f) {
        return f == 'a' ? i.aField() : i.bField();
    }

    private static Instruction set(Instruction i, char f, int v) {
        return f == 'a' ? i.withA(v) : i.withB(v);
    }

    private static Instruction copyFields(Modifier m, Instruction src, Instruction dst) {
        Instruction result = dst;
        for (Pair p : fields(m)) {
            result = set(result, p.dst(), get(src, p.src()));
        }
        return result;
    }

    private static Instruction arithmetic(Opcode op, Modifier m, Instruction a, Instruction b) {
        Instruction result = b;

        for (Pair p : fields(m)) {
            int x = get(b, p.dst());
            int y = get(a, p.src());

            int v = switch (op) {
                case ADD -> x + y;
                case SUB -> x - y;
                case MUL -> x * y;
                default -> throw new IllegalStateException();
            };

            result = set(result, p.dst(), wrap(v));
        }

        return result;
    }

    private static @Nullable Instruction divMod(Opcode op, Modifier m, Instruction a, Instruction b) {
        Instruction result = b;

        for (Pair p : fields(m)) {
            int divisor = get(a, p.src());
            if (divisor == 0) return null;

            int base = get(b, p.dst());
            int v = op == Opcode.DIV ? base / divisor : base % divisor;

            result = set(result, p.dst(), wrap(v));
        }

        return result;
    }

    private static Instruction decrement(Modifier m, Instruction i) {
        Instruction result = i;

        for (Pair p : fields(m)) {
            result = set(result, p.dst(), wrap(get(result, p.dst()) - 1));
        }

        return result;
    }
}
