package edu.kit.kastel.vads.compiler.backend.aasm;

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
        return "%" + this.name().toLowerCase();
    }
}
