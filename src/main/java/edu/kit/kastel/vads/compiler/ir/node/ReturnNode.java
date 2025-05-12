package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.backend.aasm.HardwareRegister;
import edu.kit.kastel.vads.compiler.backend.aasm.StackManager;
import edu.kit.kastel.vads.compiler.backend.aasm.StackRegister;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
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
        if (src instanceof HardwareRegister) {
            builder.append("mov ")
                    .append(src)
                    .append(", ")
                    .append("%eax")
                    .append("\n");
        } else {
            manager.retrieve(builder, (StackRegister) src, HardwareRegister.EAX);
        }

        manager.destruct(builder);
        builder.append("ret");

    }
}
