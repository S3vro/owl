package edu.kit.kastel.vads.compiler.ir.node;

public final class ConditionalJumpNode extends ControlFlowNode {

    private final Node condition;
    private final Block thenBlock;
    private final Block elseBlock;

    public ConditionalJumpNode(Block block, Node condition, Block thenBlock, Block elseBlock) {
        super(block, condition);
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    public Node getCondition() {
        return condition;
    }

    public Block getThenBlock() {
        return thenBlock;
    }

    public Block getElseBlock() {
        return elseBlock;
    }

    @Override
    protected String info() {
        return "[IF " + condition +" THEN:" + thenBlock + " ELSE:"+ elseBlock + "]";
    }
}
