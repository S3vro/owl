package edu.kit.kastel.vads.compiler.backend.x86.instructions;

import java.util.List;
import java.util.ArrayList;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;

public record x86Set(x86SetType type, Register reg) implements x86Instruction{

    @Override
    public List<x86Instruction> generate() {
        List<x86Instruction> instructions = new ArrayList<>();

        //TODO: If slow pls fix
        instructions.add(new x86Set(type, reg));

        return instructions;
    }

    @Override
    public String toString() {
        String setCmd = switch(type) {
            case EQ -> "sete";
            case NEQ -> "setne";
            case LT -> "setl";
            case LTEQ -> "setle";
            case GT -> "setg";
            case GTEQ -> "setge";
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
        LTEQ,
        GT,
        GTEQ,
    }
}
