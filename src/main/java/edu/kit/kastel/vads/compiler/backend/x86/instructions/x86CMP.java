package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;
import java.util.ArrayList;
import java.util.List;

public record x86CMP(Register op1, Register op2) implements x86Instruction{

    @Override
    public List<x86Instruction> generate() {
        List<x86Instruction> instructions = new ArrayList<>();
        Register actualOp1 = opOrFromStack(op1, HardwareRegister.EAX, instructions);
        Register actualOp2 = opOrFromStack(op2, HardwareRegister.R15D, instructions);

        instructions.add(new x86CMP(actualOp1, actualOp2));

        return instructions;
    }

    @Override
    public String toString() {
        return String.format("cmp %s, %s%n", op1, op2);
    }

}
