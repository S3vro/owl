package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;

public record x86Div(Register op1, Register op2, Register target, boolean useRemainder) implements x86Instruction {
    @Override
    public List<x86Instruction> generate() {
        List<x86Instruction> instructions = new ArrayList<x86Instruction>();

        Register localOp1 = opOrFromStack(op1, HardwareRegister.R15D, instructions);

        instructions.add(new x86Mov(op2, HardwareRegister.EAX));

        //Sign extend into EDX
        instructions.add(new x86CDQ());

        instructions.add(new x86Div(localOp1, localOp1, localOp1, useRemainder));

        HardwareRegister resultRegister = useRemainder ? HardwareRegister.EDX : HardwareRegister.EAX;

        instructions.add(new x86Mov(resultRegister, target));

        return instructions;
    }

    @Override
    public final String toString() {
        return String.format("idiv %s%n", op1);
    }
}
