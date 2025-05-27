package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;

public record x86Sub(Register op1, Register op2, Register target) implements x86Instruction{
    @Override
    public void appendInstruction(StringBuilder builder) {
        Register localOp1 = opOrFromStack(op1, HardwareRegister.EAX, builder);
        Register localOp2 = opOrFromStack(op2, HardwareRegister.R15D, builder);

        if (localOp1.equals(localOp2) && localOp2.equals(target))  {
            builder.append("xor ")
            .append(target)
            .append(", ")
            .append(target)
            .append('\n');

            return;
        }

        if (localOp1.equals(target)) {
            //FATAL must neg and use add
            builder.append("neg ").append(localOp1).append('\n');
            new x86Mov(localOp1, target).appendInstruction(builder);
            builder.append("add ").append(target).append(", ").append(localOp2).append('\n');
        } else {
            new x86Mov(localOp2, target).appendInstruction(builder);
            builder.append("sub")
                    .append(' ')
                    .append(target)
                    .append(", ")
                    .append(localOp1)
                    .append('\n');
        }



    }
}
