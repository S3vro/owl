package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import java.util.List;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public record x86Neg(Register toNeg) implements x86Instruction{

    @Override
    public List<x86Instruction> generate() {
        return List.of(this);
    }

    @Override
    public final String toString() {
        return String.format("neg %s%n", toNeg);
    }
    
}
