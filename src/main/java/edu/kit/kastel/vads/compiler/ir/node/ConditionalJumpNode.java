package edu.kit.kastel.vads.compiler.ir.node;

public final class ConditionalJumpNode extends ControlFlowNode {

    private Node condition;
    private final Block thenBlock;
    private final Block elseBlock;

    public ConditionalJumpNode(Block block, Node condition, Block thenBlock, Block elseBlock) {
        super(block, condition);
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }


    @Override
    public void setPredecessor(int idx, Node node) {
        super.setPredecessor(idx, node);
        this.condition = node;
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
