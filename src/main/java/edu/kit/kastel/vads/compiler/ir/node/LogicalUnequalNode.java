package edu.kit.kastel.vads.compiler.ir.node;

public final class LogicalUnequalNode extends ComparisonNode{

  public LogicalUnequalNode(Block block, Node left, Node right) {
    super(block, left, right);
  }

}
