package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import java.util.List;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public record x86CMP(Register op1, Register op2) implements x86Instruction{

    @Override
    public List<x86Instruction> generate() {
        return List.of(this);
    }

    @Override
    public final String toString() {
        return String.format("cmp %s, %s%n", op1, op2);
    }
    
}
