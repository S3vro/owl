package edu.kit.kastel.vads.compiler.ir.node;

public final class LogicalEqualNode extends ComparisonNode{

    public LogicalEqualNode(Block block, Node left, Node right) {
        super(block, left, right);
    }

}
