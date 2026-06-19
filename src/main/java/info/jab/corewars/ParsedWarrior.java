package info.jab.corewars;

import java.util.List;

record ParsedWarrior(List<Instruction> instructions, int start) {
}
