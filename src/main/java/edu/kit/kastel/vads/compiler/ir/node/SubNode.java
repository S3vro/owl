package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import java.util.Map;
import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public final class SubNode extends BinaryOperationNode {
    public SubNode(Block block, Node left, Node right) {
        super(block, left, right);
    }

    public void toASM(StringBuilder builder, Map<Node, Register> registers) {
        builder.append("sub ")
                .append(registers.get(predecessorSkipProj(this, BinaryOperationNode.RIGHT)))
                .append(", ")
                .append(registers.get(predecessorSkipProj(this, BinaryOperationNode.LEFT)))
                .append("\n");

        builder.append("mov ")
                .append(registers.get(predecessorSkipProj(this, BinaryOperationNode.LEFT)))
                .append(", ")
                .append(registers.get(this));
    }
}
