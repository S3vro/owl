package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import java.util.List;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public record x86Mov(Register src, Register dst) implements x86Instruction {

    @Override
    public List<x86Instruction> generate() {
        if (src == null || dst == null) {
            throw new IllegalArgumentException("src and dst cannot be null (" + src + ", " + dst + ")");
        }
        if(src.equals(dst)) return List.of();
        return List.of(this);
    }

    @Override
    public final String toString() {
        return String.format("mov %s, %s%n", dst, src);
    }

}
