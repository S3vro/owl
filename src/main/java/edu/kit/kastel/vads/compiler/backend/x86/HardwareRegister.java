package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public enum HardwareRegister implements Register {

    EAX, //Not used
    EBX,
    ECX,
    EDX, //Not used
    ESI,
    EDI,
    R8D,
    R9D,
    R10D,
    R11D,
    R12D,
    R13D,
    R14D,
    R15D, // used as swap reg -> not used
    RSP, //Not used
    RBP; //Not used

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }

    public String get8bitVersion() {
        return switch(this) {
            case EAX -> "al";
            case EBX -> "bl";
            case ECX -> "cl";
            case EDX -> "dl";
            case ESI -> "sil";
            case EDI -> "dil";
            case R8D -> "r8b";
            case R9D -> "r9b";
            case R10D -> "r10b";
            case R11D -> "r11b";
            case R12D -> "r13b";
            case R13D -> "r14b";
            case R14D -> "r15b";
            case R15D -> "al";
            case RSP -> "spl";
            case RBP -> "bpl";
        };
    }
}
