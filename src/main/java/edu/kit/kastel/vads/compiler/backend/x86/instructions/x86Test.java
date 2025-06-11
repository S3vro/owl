package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;
import edu.kit.kastel.vads.compiler.backend.x86.StackRegister;
import java.util.ArrayList;
import java.util.List;

public record x86Test(Register reg) implements x86Instruction {

    @Override
    public List<x86Instruction> generate() {
        List<x86Instruction> instructions = new ArrayList<>();
        if (reg instanceof StackRegister) {
            instructions.add(new x86Mov(reg, HardwareRegister.EAX));
            instructions.add(new x86Test(HardwareRegister.EAX));
        } else {
            instructions.add(this);
        }

        return instructions;
    }

    @Override
    public final String toString() {

        if (reg instanceof HardwareRegister hwd)
            return String.format("test %s, %s%n", hwd.get8bitVersion(), hwd.get8bitVersion());
        else
            throw new UnsupportedOperationException("There should be no Stack regs here");
    }

}
