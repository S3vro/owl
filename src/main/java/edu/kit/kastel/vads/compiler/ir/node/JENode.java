package edu.kit.kastel.vads.compiler.ir.node;

public final class JENode extends ControlFlowNode {

    private final Block target;

    public JENode(Block block, Block target) {
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
