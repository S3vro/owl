package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import java.util.List;

public record x86JE(String target) implements x86Instruction {

    @Override
    public List<x86Instruction> generate() {
        return List.of(this);
    }

    @Override
    public final String toString() {
        return String.format("je %s%n", target);
    }
    
}

