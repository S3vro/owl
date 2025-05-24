package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public record x86Mov(Register src, Register dst) implements x86Instruction {

    @Override
    public void appendInstruction(StringBuilder builder) {
        if(src.equals(dst)) return;

        builder.append("mov ")
                .append(dst)
                .append(", ")
                .append(src)
                .append('\n');

    }

}
