package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;
import edu.kit.kastel.vads.compiler.backend.x86.StackRegister;

public record x86CommutativeBinaryOperation(Register op1, Register op2, Register target, x86Command command) implements x86Instruction{

    @Override
    public List<x86Instruction> generate() {
        List<x86Instruction> instructions = new ArrayList<>();
        Register localOp1 = opOrFromStack(op1, HardwareRegister.EAX, instructions);
        Register localOp2 = opOrFromStack(op2, HardwareRegister.R15D, instructions);

        /* If operations are the wrong way swap (allowed because commutative) */
        Register srcOp = localOp1;
        Register dstOp = localOp2;

        if (localOp1.equals(target)) {
            srcOp = localOp2;
            dstOp = localOp1;
        }

        Register newTarget =  target instanceof HardwareRegister ? target : HardwareRegister.R15D;

        instructions.addAll(new x86Mov(dstOp, newTarget).generate());

        instructions.add(new x86CommutativeBinaryOperation(newTarget, srcOp, newTarget, command));

        if (target instanceof StackRegister) {
            instructions.addAll(new x86Mov(HardwareRegister.R15D, target).generate());
        }
    
        return instructions;
    }

    @Override
    public final String toString() {
        return String.format("%s %s, %s%n", command, op1, op2);
    }
    
}
