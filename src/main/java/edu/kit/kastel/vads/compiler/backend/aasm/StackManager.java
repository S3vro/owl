package edu.kit.kastel.vads.compiler.backend.aasm;

import java.util.HashMap;
import java.util.Map;

public final class StackManager {
    private final Map<StackRegister, Integer> regOffset = new HashMap<>();
    private int stackSize = 0;

    public void addToStack(StackRegister reg) {
        this.stackSize += 4; // increase stack size by one value
        this.regOffset.forEach((r, v) -> regOffset.put(r, v+4)); // increase offset for every other var
        this.regOffset.put(reg, 0);
    }

    public int getOffset(StackRegister reg) {
        return this.regOffset.get(reg);
    }

    public void retrieve(StringBuilder builder, StackRegister src,  HardwareRegister dst) {
        builder.append("mov ")
                .append(this.regOffset.get(src))
                .append(String.format("(%s)", HardwareRegister.RSP))
                .append(", ")
                .append(dst)
                .append("\n");
    }

    public void store(StringBuilder builder, HardwareRegister src, StackRegister dst) {
        builder.append("\n")
                .append("mov ")
                .append(src)
                .append(", ")
                .append(this.regOffset.get(dst))
                .append(String.format("(%s)", HardwareRegister.RSP));
    }

    public int getStackSize() {
        return this.stackSize;
    }

    public void construct(StringBuilder builder) {

        if (this.stackSize == 0) return;

        builder.append("sub ")
                .append("$")
                .append(this.stackSize)
                .append(", ")
                .append(HardwareRegister.RSP)
                .append("\n");
    }

    public void destruct(StringBuilder builder) {
        if (this.stackSize == 0) return;

        builder.append("add ")
                .append("$")
                .append(this.stackSize)
                .append(", ")
                .append(HardwareRegister.RSP)
                .append("\n");
    }
}
