package edu.kit.kastel.vads.compiler.backend.x86;


import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Instruction;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Leave;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Mov;

public final class StackManager {
    private final List<StackRegister> registers = new ArrayList<>();
    private int stackSize = 0;

    public void addToStack(StackRegister reg) {
        this.stackSize += 4; // increase stack size by one value
        this.registers.forEach(register -> register.setOffset(register.getOffset() + 4));
        this.registers.add(reg);
    }

    public int getOffset(StackRegister reg) {
        return reg.getOffset();
    }

    public int getStackSize() {
        return this.stackSize;
    }

    public void construct(StringBuilder builder) {

        if (this.stackSize == 0) return;

        builder.append("push ").append(HardwareRegister.RBP).append('\n');

        builder.append(new x86Mov(HardwareRegister.RSP, HardwareRegister.RBP));

        builder.append("sub ")
                .append(HardwareRegister.RSP)
                .append(", ")
                .append(this.stackSize)
                .append("\n");
    }

    public void destruct(List<x86Instruction> instructions) {
        if (this.stackSize == 0) return;
        instructions.add(new x86Leave());
    }
}
