package edu.kit.kastel.vads.compiler.backend.x86.instructions;

public enum x86Command {
    ADD,
    SUB,
    IMUL,
    IDIV,
    XOR,
    AND;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
