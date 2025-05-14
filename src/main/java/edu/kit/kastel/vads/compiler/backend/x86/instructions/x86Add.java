package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public record x86Add(Register op1, Register op2, Register target) implements x86Instruction{
    @Override
    public void appendInstruction(StringBuilder builder) {
        new x86BinaryInstruction(op1, op2, target, "add").appendInstruction(builder);
    }
}
