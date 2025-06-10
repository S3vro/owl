package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import java.util.List;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;

public record x86Set(x86SetType type, Register reg) implements x86Instruction{

    @Override
    public List<x86Instruction> generate() {
        return List.of(this);
    }

    @Override
    public String toString() {
        String setCmd = switch(type) {
            case EQ -> "sete";
            case NEQ -> "setne";
            case LT -> "setl";
            case LTEQ -> "setle";
        };

        //TODO: Stack Register Handling
        if (reg instanceof HardwareRegister hwd)
            return String.format("%s %s%n", setCmd, hwd.get8bitVersion());
        else
            throw new UnsupportedOperationException("There should be no Stack regs here");
    }

    public enum x86SetType {
        EQ,
        NEQ,
        LT,
        LTEQ
    }
}
