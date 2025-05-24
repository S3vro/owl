package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public class StackRegister implements Register {

    private final int id;

    private int offset;

    public StackRegister(int id, int offset) {
        this.id = id;
        this.offset = offset;
    }

    public int getId() {
        return id;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public String toString() {
        return String.format("DWORD PTR [%s+%d]",  HardwareRegister.RSP, this.offset);
    }
}
