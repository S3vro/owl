package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import java.util.Map;
import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public final class MulNode extends BinaryOperationNode {
    public MulNode(Block block, Node left, Node right) {
        super(block, left, right);
    }

    public void toASM(StringBuilder builder, Map<Node, Register> registers) {
        builder.append("imul ")
                .append(registers.get(predecessorSkipProj(this, BinaryOperationNode.RIGHT)))
                .append(", ")
                .append(registers.get(predecessorSkipProj(this, BinaryOperationNode.LEFT)))
                .append("\n");

        builder.append("mov ")
                .append(registers.get(predecessorSkipProj(this, BinaryOperationNode.LEFT)))
                .append(", ")
                .append(registers.get(this));
    }

    @SuppressWarnings("EqualsDoesntCheckParameterClass") // we do, but not here
    @Override
    public boolean equals(Object obj) {
        return commutativeEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return commutativeHashCode(this);
    }
}
