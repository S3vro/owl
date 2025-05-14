package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.backend.x86.HardwareRegister;
import edu.kit.kastel.vads.compiler.backend.x86.StackManager;
import edu.kit.kastel.vads.compiler.backend.x86.StackRegister;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.x86.instructions.x86Mov;

import java.util.Map;
import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public final class ReturnNode extends Node {
    public static final int SIDE_EFFECT = 0;
    public static final int RESULT = 1;
    public ReturnNode(Block block, Node sideEffect, Node result) {
        super(block, sideEffect, result);
    }

    public void toASM(StringBuilder builder, Map<Node, Register> registers, StackManager manager) {
        Register src = registers.get(predecessorSkipProj(this, ReturnNode.RESULT));
        new x86Mov(src, HardwareRegister.EAX).appendInstruction(builder);
        manager.destruct(builder);
        builder.append("ret");

    }
}
