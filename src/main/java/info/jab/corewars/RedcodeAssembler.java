package info.jab.corewars;

import static info.jab.corewars.CoreWarConfig.CORESIZE;
import static info.jab.corewars.CoreWarConfig.MAXCYCLES;
import static info.jab.corewars.CoreWarConfig.MAXLENGTH;
import static info.jab.corewars.CoreWarConfig.MAXPROCS;
import static info.jab.corewars.CoreWarConfig.ROUNDS;
import static info.jab.corewars.CoreWarConfig.wrap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

final class RedcodeAssembler {
    private static final Set<String> DIRECTIVES = Set.of(
        "equ", "org", "end", "for", "rof", "program"
    );
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final String file;
    private final List<SourceLine> source = new ArrayList<>();
    private final Map<String, String> equs = new LinkedHashMap<>();
    private final Map<String, String> operandEqus = new HashMap<>();
    private final Map<String, Integer> constants = new HashMap<>();
    private final Map<String, Integer> labels = new HashMap<>();
    private List<SourceLine> expanded = List.of();

    RedcodeAssembler(String file) {
        this.file = file;
    }

    ParsedWarrior parse() throws Exception {
        loadSource();
        collectEqus(source);
        resolveConstants(false);
        expanded = expandFors(source);
        collectLabels(expanded);
        resolveConstants(true);

        List<Instruction> instructions = new ArrayList<>();
        int start = 0;
        int pc = 0;

        for (SourceLine sourceLine : expanded) {
            ParsedLine line = parseLine(sourceLine);
            if (line == null || line.op().isBlank()) continue;

            if (line.op().equals("org")) {
                start = wrap(eval(line.rest(), pc));
                continue;
            }

            if (line.op().equals("end")) {
                if (!line.rest().isBlank()) {
                    start = wrap(eval(line.rest(), pc));
                }
                break;
            }

            if (DIRECTIVES.contains(line.op())) continue;

            instructions.add(parseInstruction(line, pc));
            pc++;
        }

        if (instructions.isEmpty()) {
            throw new IllegalArgumentException("No instructions found");
        }
        if (instructions.size() > MAXLENGTH) {
            throw new IllegalArgumentException(
                "Warrior length " + instructions.size() + " exceeds MAXLENGTH " + MAXLENGTH
            );
        }

        return new ParsedWarrior(instructions, start);
    }

    private void loadSource() throws Exception {
        int lineNumber = 0;
        for (String raw : Files.readAllLines(Path.of(file))) {
            lineNumber++;
            String line = raw.split(";", 2)[0].strip();
            if (!line.isEmpty()) {
                source.add(new SourceLine(line, lineNumber));
            }
        }
    }

    private void collectEqus(List<SourceLine> lines) {
        for (SourceLine sourceLine : lines) {
            ParsedLine line = parseLine(sourceLine);
            if (line != null && line.op().equals("equ") && line.label() != null) {
                String label = firstWhitespaceToken(line.label());
                String rest = line.rest().strip();
                if (!rest.isEmpty() && "#$*@{}<>".indexOf(rest.charAt(0)) >= 0) {
                    operandEqus.put(label, rest);
                } else {
                    equs.put(label, rest);
                }
            }
        }
    }

    private void resolveConstants(boolean allowLabels) {
        constants.put("coresize", CORESIZE);
        constants.put("maxprocesses", MAXPROCS);
        constants.put("maxprocs", MAXPROCS);
        constants.put("maxcycles", MAXCYCLES);
        constants.put("maxlength", MAXLENGTH);
        constants.put("mindistance", MAXLENGTH);
        constants.put("warriors", 2);
        constants.put("rounds", ROUNDS);

        Set<String> unresolved = new LinkedHashSet<>();
        for (String name : equs.keySet()) {
            if (!constants.containsKey(name)) {
                unresolved.add(name);
            }
        }

        while (!unresolved.isEmpty()) {
            boolean progress = false;
            Iterator<String> iterator = unresolved.iterator();

            while (iterator.hasNext()) {
                String name = iterator.next();
                String expression = equs.get(name);
                if (expression == null) {
                    throw new IllegalStateException("Missing EQU symbol: " + name);
                }
                try {
                    int value = new Expression(
                        expression,
                        constants,
                        labels,
                        0,
                        !allowLabels
                    ).parse();
                    constants.put(name, value);
                    iterator.remove();
                    progress = true;
                } catch (IllegalArgumentException e) {
                    String message = e.getMessage();
                    if (message == null || !message.startsWith("Unknown symbol")) {
                        throw e;
                    }
                }
            }

            if (!progress) {
                if (!allowLabels) {
                    return;
                }
                throw new IllegalArgumentException("Unresolved EQU symbols: " + unresolved);
            }
        }
    }

    private List<SourceLine> expandFors(List<SourceLine> lines) {
        List<SourceLine> result = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            SourceLine sourceLine = lines.get(i);
            ParsedLine line = parseLine(sourceLine);
            if (line == null) continue;

            if (!line.op().equals("for")) {
                if (!line.op().equals("rof")) {
                    result.add(sourceLine);
                }
                continue;
            }

            if (line.label() != null) {
                result.add(new SourceLine(line.label(), sourceLine.lineNumber()));
            }

            int depth = 1;
            List<SourceLine> body = new ArrayList<>();
            while (++i < lines.size()) {
                SourceLine bodyLine = lines.get(i);
                ParsedLine parsed = parseLine(bodyLine);
                if (parsed != null && parsed.op().equals("for")) depth++;
                if (parsed != null && parsed.op().equals("rof")) {
                    depth--;
                    if (depth == 0) break;
                }
                body.add(bodyLine);
            }
            if (depth != 0) {
                throw new IllegalArgumentException("FOR without ROF at line " + sourceLine.lineNumber());
            }

            int count = Math.max(0, eval(line.rest(), 0));
            List<SourceLine> expandedBody = expandFors(body);
            for (int n = 0; n < count; n++) {
                result.addAll(expandedBody);
            }
        }

        return result;
    }

    private void collectLabels(List<SourceLine> lines) {
        int pc = 0;
        for (SourceLine sourceLine : lines) {
            ParsedLine line = parseLine(sourceLine);
            if (line == null) continue;

            if (line.label() != null && !line.op().equals("equ")) {
                for (String label : splitWhitespace(line.label())) {
                    if (!label.isBlank()) {
                        labels.put(label, pc);
                    }
                }
            }

            if (!line.op().isBlank() && !DIRECTIVES.contains(line.op())) {
                pc++;
            }
        }
    }

    private Instruction parseInstruction(ParsedLine line, int pc) {
        String opText = line.op();
        String modText = null;
        int dot = opText.indexOf('.');
        if (dot >= 0) {
            modText = opText.substring(dot + 1);
            opText = opText.substring(0, dot);
        }

        Opcode opcode = Opcode.valueOf(opText.toUpperCase(Locale.ROOT));
        Modifier modifier = modText == null || modText.isBlank()
            ? defaultModifier(opcode)
            : Modifier.valueOf(modText.toUpperCase(Locale.ROOT));

        String[] operands = splitOperands(line.rest());
        OperandText a = parseOperandText(operands[0]);
        OperandText b = parseOperandText(operands.length > 1 ? operands[1] : "0");

        return new Instruction(
            opcode,
            modifier,
            a.mode(),
            wrap(eval(a.expression(), pc)),
            b.mode(),
            wrap(eval(b.expression(), pc))
        );
    }

    private String[] splitOperands(String rest) {
        List<String> operands = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                operands.add(rest.substring(start, i).strip());
                start = i + 1;
            }
        }
        operands.add(rest.substring(start).strip());
        return operands.toArray(String[]::new);
    }

    private OperandText parseOperandText(String text) {
        text = text.strip();
        if (text.isEmpty()) text = "0";
        if (operandEqus.containsKey(text)) {
            String operandEqu = operandEqus.get(text);
            if (operandEqu == null) {
                throw new IllegalStateException("Missing operand EQU: " + text);
            }
            text = operandEqu;
        }
        Mode mode = modeOf(String.valueOf(text.charAt(0)));
        if ("#$*@{}<>".indexOf(text.charAt(0)) >= 0) {
            text = text.substring(1).strip();
        }
        if (text.isEmpty()) text = "0";
        return new OperandText(mode, text);
    }

    private @Nullable ParsedLine parseLine(SourceLine sourceLine) {
        String text = sourceLine.text().toLowerCase(Locale.ROOT).strip();
        if (text.isEmpty()) return null;

        List<String> parts = splitWhitespace(text);
        String label = null;
        String op;
        String rest;

        if (parts.size() >= 3 && text.substring(text.indexOf(parts.get(1)) + parts.get(1).length()).strip().startsWith("for")) {
            label = parts.getFirst();
            op = "for";
            String afterSecond = text.substring(text.indexOf(parts.get(1)) + parts.get(1).length()).strip();
            rest = afterSecond.substring("for".length()).strip();
        } else if (isOperation(parts.getFirst())) {
            op = parts.getFirst();
            rest = parts.size() > 1 ? text.substring(parts.getFirst().length()).strip() : "";
        } else {
            int opIndex = -1;
            for (int i = 1; i < parts.size(); i++) {
                if (isOperation(parts.get(i))) {
                    opIndex = i;
                    break;
                }
            }

            if (opIndex < 0 && parts.size() == 1) {
                return new ParsedLine(parts.getFirst(), "", "", sourceLine.lineNumber());
            } else if (opIndex < 0) {
                throw new IllegalArgumentException(
                    "Unknown operation at line " + sourceLine.lineNumber() + ": " + sourceLine.text()
                );
            }

            label = String.join(" ", parts.subList(0, opIndex));
            op = parts.get(opIndex);
            int searchFrom = label.length();
            int restStart = text.indexOf(op, searchFrom) + op.length();
            rest = text.substring(restStart).strip();
        }

        return new ParsedLine(label, op, rest, sourceLine.lineNumber());
    }

    private boolean isOperation(String token) {
        String base = token.contains(".") ? token.substring(0, token.indexOf('.')) : token;
        if (DIRECTIVES.contains(base)) return true;
        try {
            Opcode.valueOf(base.toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String firstWhitespaceToken(String text) {
        return splitWhitespace(text).getFirst();
    }

    private static List<String> splitWhitespace(String text) {
        return WHITESPACE.splitAsStream(text).toList();
    }

    private int eval(String expression, int pc) {
        return new Expression(expression, constants, labels, pc, false).parse();
    }

    private static Mode modeOf(String s) {
        if (s == null || s.isEmpty() || s.equals("$")) return Mode.DIRECT;

        return switch (s.charAt(0)) {
            case '#' -> Mode.IMMEDIATE;
            case '*' -> Mode.A_INDIRECT;
            case '@' -> Mode.B_INDIRECT;
            case '{' -> Mode.A_PREDEC;
            case '}' -> Mode.A_POSTINC;
            case '<' -> Mode.B_PREDEC;
            case '>' -> Mode.B_POSTINC;
            default -> Mode.DIRECT;
        };
    }

    private static Modifier defaultModifier(Opcode opcode) {
        return switch (opcode) {
            case DAT, ADD, SUB, MUL, DIV, MOD -> Modifier.F;
            case MOV, SEQ, CMP, SNE -> Modifier.I;
            case SLT, JMP, JMZ, JMN, DJN, SPL, NOP -> Modifier.B;
        };
    }

    private record SourceLine(String text, int lineNumber) {
    }

    private record ParsedLine(@Nullable String label, String op, String rest, int lineNumber) {
    }

    private record OperandText(Mode mode, String expression) {
    }

    private static final class Expression {
        private final String input;
        private final Map<String, Integer> constants;
        private final Map<String, Integer> labels;
        private final int pc;
        private final boolean constantsOnly;
        private int pos = 0;

        Expression(
            @Nullable String input,
            Map<String, Integer> constants,
            Map<String, Integer> labels,
            int pc,
            boolean constantsOnly
        ) {
            this.input = input == null || input.isBlank() ? "0" : input.toLowerCase(Locale.ROOT);
            this.constants = constants;
            this.labels = labels;
            this.pc = pc;
            this.constantsOnly = constantsOnly;
        }

        int parse() {
            int value = parseAddSub();
            skipSpace();
            if (pos != input.length()) {
                throw new IllegalArgumentException("Unexpected expression text: " + input.substring(pos));
            }
            return value;
        }

        private int parseAddSub() {
            int value = parseMulDivMod();
            while (true) {
                skipSpace();
                if (match('+')) value += parseMulDivMod();
                else if (match('-')) value -= parseMulDivMod();
                else return value;
            }
        }

        private int parseMulDivMod() {
            int value = parseUnary();
            while (true) {
                skipSpace();
                if (match('*')) value *= parseUnary();
                else if (match('/')) value /= parseUnary();
                else if (match('%')) value %= parseUnary();
                else return value;
            }
        }

        private int parseUnary() {
            skipSpace();
            if (match('+')) return parseUnary();
            if (match('-')) return -parseUnary();
            return parsePrimary();
        }

        private int parsePrimary() {
            skipSpace();
            if (match('(')) {
                int value = parseAddSub();
                expect(')');
                return value;
            }

            if (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                int start = pos;
                while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
                return Integer.parseInt(input.substring(start, pos));
            }

            if (pos < input.length() && isIdentifierStart(input.charAt(pos))) {
                int start = pos;
                pos++;
                while (pos < input.length() && isIdentifierPart(input.charAt(pos))) pos++;
                String name = input.substring(start, pos);

                if (constants.containsKey(name)) return constants.get(name);
                if (labels.containsKey(name) && !constantsOnly) return labels.get(name) - pc;
                throw new IllegalArgumentException("Unknown symbol in expression: " + name);
            }

            throw new IllegalArgumentException("Invalid expression: " + input);
        }

        private boolean match(char c) {
            skipSpace();
            if (pos < input.length() && input.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        private void expect(char c) {
            if (!match(c)) {
                throw new IllegalArgumentException("Expected '" + c + "' in expression: " + input);
            }
        }

        private void skipSpace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
        }

        private boolean isIdentifierStart(char c) {
            return Character.isLetter(c) || c == '_';
        }

        private boolean isIdentifierPart(char c) {
            return Character.isLetterOrDigit(c) || c == '_';
        }
    }
}
