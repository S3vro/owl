package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;

public record x86Div(Register op1, Register op2, Register target, boolean useRemainder) implements x86Instruction {
    @Override
    public void appendInstruction(StringBuilder builder) {
        Register localOp1 = opOrFromStack(op1, HardwareRegister.R15D, builder);

        new x86Mov(op2, HardwareRegister.EAX).appendInstruction(builder);

        //Sign extend into EDX
        builder.append("cdq").append('\n');

        builder.append("idiv ").append(localOp1).append('\n');

        HardwareRegister resultRegister = useRemainder ? HardwareRegister.EDX : HardwareRegister.EAX;

        new x86Mov(resultRegister, target).appendInstruction(builder);
    }
}
