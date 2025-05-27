package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;
import edu.kit.kastel.vads.compiler.backend.x86.StackRegister;

public record x86CommutativeBinaryOperation(Register op1, Register op2, Register target, x86Command command) implements x86Instruction{

    @Override
    public void appendInstruction(StringBuilder builder) {
        Register localOp1 = opOrFromStack(op1, HardwareRegister.EAX, builder);
        Register localOp2 = opOrFromStack(op2, HardwareRegister.R15D, builder);

        /* If operations are the wrong way swap (allowed because commutative) */
        Register srcOp = localOp1;
        Register dstOp = localOp2;

        if (localOp1.equals(target)) {
            srcOp = localOp2;
            dstOp = localOp1;
        }

        new x86Mov(dstOp, target instanceof HardwareRegister ? target : HardwareRegister.R15D).appendInstruction(builder);

        builder.append(command)
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
