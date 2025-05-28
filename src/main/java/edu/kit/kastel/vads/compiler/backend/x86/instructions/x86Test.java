package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import java.util.List;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;

public record x86Test(Register reg) implements x86Instruction {

    @Override
    public List<x86Instruction> generate() {
        return List.of(this);
    }

    @Override
    public final String toString() {

        if (reg instanceof HardwareRegister hwd)
            return String.format("test %s, %s%n", hwd.get8bitVersion(), hwd.get8bitVersion());
        else
            return "";
    }
    
}
