package info.jab.corewars;

final class CoreWarConfig {
    static final int CORESIZE = 8000;
    static final int MAXPROCS = 8000;
    static final int MAXCYCLES = 80000;
    static final int MAXLENGTH = 100;
    static final int ROUNDS = 100;
    static final int FNUMBER = 7293;

    static final int CWIDTH = 100;
    static final int CHEIGHT = 80;
    static final int CPIX = 6;

    private CoreWarConfig() {
    }

    static int wrap(int v) {
        return Math.floorMod(v, CORESIZE);
    }
}
