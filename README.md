# JMars

JMars is a small [Core War / MARS](https://corewar.co.uk/mars.htm) visualizer
written as a single-file Java program. It runs with
[JBang](https://www.jbang.dev/) and uses [Processing](https://processing.org/) to draw the core while two Redcode warriors execute.

## How to run in local

Run JMars with two Redcode warrior files:

```bash
jbang src/main/java/info/jab/corewars/JMars.java src/test/resources/warriors/imp.red src/test/resources/warriors/dwarf.red
```

The Processing window shows core ownership:

- Blue cells belong to warrior 1.
- Orange cells belong to warrior 2.
- Black cells are unowned.

When the match finishes, JMars prints two result lines:

```text
<warrior1 wins> <ties>
<warrior2 wins> <ties>
```

## Example Warriors

This repository includes a few parser-friendly test warriors in
`src/test/resources/warriors`:

- `src/test/resources/warriors/imp.red`: the classic self-copying Imp, useful for seeing movement.
- `src/test/resources/warriors/dwarf.red`: a compact bomber adapted from the classic Dwarf.
- `src/test/resources/warriors/runner.red`: a small process-splitting mover for visual smoke tests.
- `src/test/resources/warriors/stone.red`: a faster bomber with a wider bombing step.
- `src/test/resources/warriors/clear.red`: a tiny sequential core-clear.
- `src/test/resources/warriors/paper.red`: a toy splitter that creates busy traces.
- `src/test/resources/warriors/imp-launcher.red`: starts several simple imps.
- `src/test/resources/warriors/vampire-toy.red`: drops jump instructions into a small pit.

The directory also contains larger pMARS-style warriors that exercise the
assembler support:

- `src/test/resources/warriors/artofcorewar.red`: qscan into paper.
- `src/test/resources/warriors/excalibur.red`: scanner/core-clear style warrior.
- `src/test/resources/warriors/forgottenloreii.red`: scanning vampire and clear.
- `src/test/resources/warriors/sonofvain.red`: qscan into stone/imp.
- `src/test/resources/warriors/validate.red`: ICWS88-oriented validation warrior.

Good test battles:

```bash
jbang src/main/java/info/jab/corewars/JMars.java src/test/resources/warriors/imp.red src/test/resources/warriors/dwarf.red
jbang src/main/java/info/jab/corewars/JMars.java src/test/resources/warriors/runner.red src/test/resources/warriors/dwarf.red
jbang src/main/java/info/jab/corewars/JMars.java src/test/resources/warriors/paper.red src/test/resources/warriors/stone.red
jbang src/main/java/info/jab/corewars/JMars.java src/test/resources/warriors/imp-launcher.red src/test/resources/warriors/clear.red
jbang src/main/java/info/jab/corewars/JMars.java src/test/resources/warriors/vampire-toy.red src/test/resources/warriors/runner.red
jbang src/main/java/info/jab/corewars/JMars.java src/test/resources/warriors/artofcorewar.red src/test/resources/warriors/dwarf.red
jbang src/main/java/info/jab/corewars/JMars.java src/test/resources/warriors/excalibur.red src/test/resources/warriors/sonofvain.red
jbang src/main/java/info/jab/corewars/JMars.java src/test/resources/warriors/forgottenloreii.red src/test/resources/warriors/validate.red
```

## Supported Warrior Format

JMars accepts simple numeric Redcode instructions in this shape:

```text
opcode.modifier a-mode a-field, b-mode b-field
```

Example:

```text
mov.i $0, $1
dat.f $0, $0
```

Supported opcodes:

```text
DAT NOP MOV ADD SUB MUL DIV MOD JMP JMZ JMN DJN SPL SEQ CMP SNE SLT
```

Supported modifiers:

```text
I F X A B AB BA
```

Supported addressing modes:

```text
# immediate
$ direct
* A-indirect
@ B-indirect
{ A-predecrement
} A-postincrement
< B-predecrement
> B-postincrement
```

It also includes a small assembler for common pMARS-style source files. Supported
assembler features include:

- Labels
- `EQU` constants
- `ORG` and `END`
- `FOR` / `ROF` repetition
- Arithmetic expressions with `+`, `-`, `*`, `/`, `%`, and parentheses
- Omitted addressing modes, which default to direct mode
- Omitted instruction modifiers for common opcodes

For example, the tournament-style warrior in
`src/test/resources/warriors/artofcorewar.red` can be run directly:

```bash
jbang src/main/java/info/jab/corewars/JMars.java src/test/resources/warriors/artofcorewar.red src/test/resources/warriors/dwarf.red
```
