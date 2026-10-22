package io.github.sudabake.appliedagristics.block;

public interface IOControls {
    void configureSide(int side, int mode);
    int sideMode(int side);
    void toggleAutoOutput();
}
