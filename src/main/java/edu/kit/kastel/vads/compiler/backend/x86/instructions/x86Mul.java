package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;
import edu.kit.kastel.vads.compiler.backend.x86.StackRegister;

public record x86Mul(Register op1, Register op2, Register target) implements x86Instruction {

    @Override
    public void appendInstruction(StringBuilder builder) {
        Register localOp1 = op1;
        Register localOp2 = op2;


        if (localOp1 instanceof StackRegister) {
            new x86Mov(localOp1, HardwareRegister.EAX).appendInstruction(builder);
            localOp1 = HardwareRegister.EAX;
        }

        if (localOp2 instanceof StackRegister) {
            new x86Mov(localOp2, HardwareRegister.R15D).appendInstruction(builder);
            localOp2 = HardwareRegister.R15D;
        }

        Register srcOp = localOp1;
        Register dstOp = localOp2;

        if (localOp1.equals(target)) {
            srcOp = localOp2;
            dstOp = localOp1;
        }

        new x86Mov(dstOp, target instanceof HardwareRegister ? target : HardwareRegister.R15D).appendInstruction(builder);

        builder.append("imul")
                .append(' ')
                .append(target instanceof HardwareRegister ? target : HardwareRegister.R15D)
                .append(", ")
                .append(srcOp)
                .append('\n');

        if (target instanceof StackRegister) {
            new x86Mov(HardwareRegister.R15D, target).appendInstruction(builder);
        }

    }
}
