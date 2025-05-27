package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.StackRegister;

public interface x86Instruction {
    void appendInstruction(StringBuilder builder);

    default Register opOrFromStack(Register src, Register dst, StringBuilder builder) {
        if (src instanceof StackRegister) {
            new x86Mov(src, dst).appendInstruction(builder);
            return dst;
        }

        return src;
    }
}
