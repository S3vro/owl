package edu.kit.kastel.vads.compiler.ir.node;

public final class UndefNode extends Node {

    public UndefNode(Block block, Node... predecessors) {
        super(block, predecessors);
    }
    
}
