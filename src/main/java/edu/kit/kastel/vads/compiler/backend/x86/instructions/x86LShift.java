package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;
import java.util.ArrayList;
import java.util.List;

public record x86LShift(Register op1, Register op2, Register target) implements x86Instruction{
    @Override
    public List<x86Instruction> generate() {
        List<x86Instruction> instructions = new ArrayList<>();

        Register localOp1 = opOrFromStack(op1, HardwareRegister.EAX, instructions);
        Register localOp2 = opOrFromStack(op2, HardwareRegister.R15D, instructions);



        instructions.add(new x86Mov(localOp1, HardwareRegister.ECX));
        instructions.add(new x86Mov(localOp2, target));
        instructions.add(new x86LShift(target, HardwareRegister.ECX, target));


        return instructions;
    }

    @Override
    public String toString() {
        return String.format("sal %s, %s%n", op1, op2);
    }
}
