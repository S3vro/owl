package edu.kit.kastel.vads.compiler.backend.x86.instructions;


import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;
import edu.kit.kastel.vads.compiler.backend.x86.StackRegister;

public record x86BinaryInstruction(Register op1, Register op2, Register target, String opcode) implements x86Instruction {

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


        builder.append(opcode)
                .append(' ')
                .append(localOp1)
                .append(", ")
                .append(localOp2)
                .append('\n');

        new x86Mov(localOp2, target).appendInstruction(builder);
    }
}
