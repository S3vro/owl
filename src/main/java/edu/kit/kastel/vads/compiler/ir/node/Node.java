package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.ir.util.DebugInfo;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfoHelper;

import java.util.ArrayList;
import java.util.List;

/// The base class for all nodes.
public sealed abstract class Node
  permits BinaryOperationNode, Block, ConstBoolNode, ConstIntNode, ControlFlowNode, InCodeJmpNode, Phi, ProjNode, StartNode,
  UndefNode {
    private final IrGraph graph;
    private final Block block;
    private final List<Node> predecessors = new ArrayList<>();
    private final DebugInfo debugInfo;
    private final List<Node> successors = new ArrayList<>();

    protected Node(Block block, Node... predecessors) {
        this.graph = block.graph();
        this.block = block;
        this.predecessors.addAll(List.of(predecessors));
        for (Node predecessor : predecessors) {
            graph.registerSuccessor(predecessor, this);
            predecessor.addSuccessor(this);
        }
        this.debugInfo = DebugInfoHelper.getDebugInfo();
    }

    protected Node(IrGraph graph) {
        assert this.getClass() == Block.class : "must be used by Block only";
        this.graph = graph;
        this.block = (Block) this;
        this.debugInfo = DebugInfo.NoInfo.INSTANCE;
    }

    public final IrGraph graph() {
        return this.graph;
    }

    public final Block block() {
        return this.block;
    }

    public final List<? extends Node> predecessors() {
        return List.copyOf(this.predecessors);
    }

    public final List<? extends Node> successors() {
        return List.copyOf(this.successors);
    }

    public final void addSuccessor(Node successor) {
        this.successors.add(successor);
    }

    public void setPredecessor(int idx, Node node) {
        this.graph.removeSuccessor(this.predecessors.get(idx), this);
        this.predecessors.set(idx, node);
        this.graph.registerSuccessor(node, this);
    }

    public final void addPredecessor(Node node) {
        node.addSuccessor(this);
        this.predecessors.add(node);
        this.graph.registerSuccessor(node, this);
    }

    public final Node predecessor(int idx) {
        return this.predecessors.get(idx);
    }

    @Override
    public final String toString() {
        return (this.getClass().getSimpleName().replace("Node", "") + " " + info()).stripTrailing();
    }

    protected String info() {
        return "";
    }

    public DebugInfo debugInfo() {
        return debugInfo;
    }

    protected static int predecessorHash(Node node, int predecessor) {
        return System.identityHashCode(node.predecessor(predecessor));
    }

    public void removePredecessor(Node node) {
        this.graph.removeSuccessor(node, this);
        this.predecessors.remove(node);
    }

    public void removeAllPreds() {
        for (Node pred : this.predecessors) {
            this.graph.removeSuccessor(pred, this);
        }
        this.predecessors.clear();
    }
}
