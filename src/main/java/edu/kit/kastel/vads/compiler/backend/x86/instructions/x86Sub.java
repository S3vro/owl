package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;

public record x86Sub(Register op1, Register op2, Register target) implements x86Instruction{
    @Override
    public List<x86Instruction> generate() {
        List<x86Instruction> instructions = new ArrayList<>();

        Register localOp1 = opOrFromStack(op1, HardwareRegister.EAX, instructions);
        Register localOp2 = opOrFromStack(op2, HardwareRegister.R15D, instructions);

        if (localOp1.equals(localOp2) && localOp2.equals(target))  {
            //add single instruction to zero target
            instructions.add(new x86CommutativeBinaryOperation(target, target, target, x86Command.XOR));
        }

        if (localOp1.equals(target)) {
            //FATAL must neg and use add
            instructions.add(new x86Neg(localOp1));
            instructions.add(new x86Mov(localOp1, target));
            instructions.add(new x86CommutativeBinaryOperation(target, localOp2, target, x86Command.ADD));
        } else {
            instructions.add(new x86Mov(localOp2, target));
            instructions.add(new x86Sub(target, localOp1, target));
        }


        return instructions;
    }

    @Override
    public final String toString() {
        return String.format("sub %s, %s%n", op1, op2);
    }
}
