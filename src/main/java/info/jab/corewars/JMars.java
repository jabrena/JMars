///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.processing:core:4.5.3
//SOURCES CoreWarConfig.java
//SOURCES Instruction.java
//SOURCES Mars.java
//SOURCES Mode.java
//SOURCES Modifier.java
//SOURCES Operand.java
//SOURCES Opcode.java
//SOURCES ParsedWarrior.java
//SOURCES RedcodeAssembler.java
//SOURCES Write.java
//JAVA_OPTIONS --enable-native-access=ALL-UNNAMED

package info.jab.corewars;

import static info.jab.corewars.CoreWarConfig.CHEIGHT;
import static info.jab.corewars.CoreWarConfig.CPIX;
import static info.jab.corewars.CoreWarConfig.CWIDTH;

import org.jspecify.annotations.Nullable;
import processing.core.PApplet;

public class JMars extends PApplet {
    private @Nullable Mars mars;
    private int cyclesPerFrame = 20000;
    private int renderedRound = -1;

    @Override
    public void settings() {
        size(CWIDTH * CPIX, CHEIGHT * CPIX);
        pixelDensity(1);
    }

    @Override
    public void setup() {
        frameRate(50);

        if (args == null || args.length != 2) {
            System.err.println("Usage: jbang src/main/java/info/jab/corewars/JMars.java warrior1.red warrior2.red");
            exit();
            return;
        }

        mars = new Mars(args[0], args[1]);
        noStroke();
        background(0);
        renderedRound = mars.round;
    }

    @Override
    public void draw() {
        Mars current = mars;
        if (current == null) {
            return;
        }

        for (int i = 0; i < cyclesPerFrame && !current.finished; i++) {
            Write write = current.step();

            if (current.round != renderedRound) {
                background(0);
                renderedRound = current.round;
            }

            if (write != null) {
                paintWrite(write);
            }
        }

        if (current.finished) {
            println(current.win + " " + current.tie);
            println(current.loss + " " + current.tie);
            noLoop();
            exit();
        }
    }

    @Override
    public void keyPressed() {
        if (key == '+' || key == '=') {
            cyclesPerFrame = Math.min(cyclesPerFrame * 2, 1_000_000);
            println("cyclesPerFrame=" + cyclesPerFrame);
        } else if (key == '-' || key == '_') {
            cyclesPerFrame = Math.max(cyclesPerFrame / 2, 1);
            println("cyclesPerFrame=" + cyclesPerFrame);
        }
    }

    private void paintWrite(Write write) {
        int x = (write.address() % CWIDTH) * CPIX;
        int y = (write.address() / CWIDTH) * CPIX;

        switch (write.owner()) {
            case 1 -> fill(0, 0, 255);
            case 2 -> fill(255, 140, 0);
            default -> fill(0);
        }

        rect(x, y, CPIX - 1, CPIX - 1);
    }

    public static void main(String[] args) {
        PApplet.main(JMars.class, args);
    }
}
