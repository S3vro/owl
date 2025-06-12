package edu.kit.kastel.vads.compiler.ir.node;

public final class InCodeJmpNode extends Node {

    private final Block target;

    public InCodeJmpNode(Block block, Block target) {
        super(block);
        this.target = target;
    }

    public Block target() {
        return target;
    }

    @Override
    public String info() {
        return "[target=" + target + "]";
    }
}
