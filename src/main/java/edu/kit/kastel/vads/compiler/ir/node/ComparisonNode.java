package edu.kit.kastel.vads.compiler.ir.node;

public sealed class ComparisonNode extends BinaryOperationNode
  permits GreaterThanNode, GreaterThanOrEqualNode, LessThanNode, LessThanOrEqualNode, LogicalEqualNode,
  LogicalUnequalNode {

    protected ComparisonNode(Block block, Node left, Node right) {
        super(block, left, right);
    }

}
