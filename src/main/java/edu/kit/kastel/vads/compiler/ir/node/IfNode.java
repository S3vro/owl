package edu.kit.kastel.vads.compiler.ir.node;

public final class IfNode extends Node{

    private final Node exp;
    private final Block thenBlock;
    private final Block elseBlock;

    public IfNode(Block block, Node exp, Block thenBlock, Block elseBlock) {
        super(block, exp);
        this.exp = exp;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    public Node getCondition() {
        return exp;
    }

    public Block getThenBlock() {
        return thenBlock;
    }

    public Block getElseBlock() {
        return elseBlock;
    }
}
