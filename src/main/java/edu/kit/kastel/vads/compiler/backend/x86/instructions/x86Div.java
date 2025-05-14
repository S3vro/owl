package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;
import edu.kit.kastel.vads.compiler.backend.x86.StackRegister;

public record x86Div(Register op1, Register op2, Register target, boolean useRemainder) implements x86Instruction {
    @Override
    public void appendInstruction(StringBuilder builder) {
        Register localOp1 = op1;

        if (localOp1 instanceof StackRegister) {
            new x86Mov(localOp1, HardwareRegister.R15D).appendInstruction(builder);
            localOp1 = HardwareRegister.R15D;
        }

        new x86Mov(op2, HardwareRegister.EAX).appendInstruction(builder);

        //Sign extend into EDX
        builder.append("cdq").append('\n');

        builder.append("idiv ").append(localOp1).append('\n');

        HardwareRegister resultRegister = useRemainder ? HardwareRegister.EDX : HardwareRegister.EAX;

        new x86Mov(resultRegister, target).appendInstruction(builder);
    }
}
