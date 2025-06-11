package edu.kit.kastel.vads.compiler.ir.node;

public sealed class ControlFlowNode extends Node
  permits ConditionalJumpNode, JENode, JmpNode, ReturnNode {

  public ControlFlowNode(Block block, Node... predecessors) {
    super(block, predecessors);
  }

}
