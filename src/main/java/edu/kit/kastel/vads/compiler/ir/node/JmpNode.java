package edu.kit.kastel.vads.compiler.ir.node;

public final class JmpNode extends Node{

    private Block target;

    public JmpNode(Block block, Block target) {
        super(block);
        this.target = target;
    }

    public Block getTarget() {
        return target;
    }
}
