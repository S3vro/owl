package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import java.util.List;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.StackRegister;

public interface x86Instruction {
    List<x86Instruction> generate();

    default Register opOrFromStack(Register src, Register dst, List<x86Instruction> instructions) {
        if (src instanceof StackRegister) {
            instructions.addAll(new x86Mov(src, dst).generate());
            return dst;
        }

        return src;
    }
}
