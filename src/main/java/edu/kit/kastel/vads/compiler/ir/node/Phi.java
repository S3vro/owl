package edu.kit.kastel.vads.compiler.ir.node;

public final class Phi extends Node {
    private boolean isSideEffectPhi = false;

    public Phi(Block block, Node... preds) {
        super(block, preds);
    }


    public void appendOperand(Node node) {
        addPredecessor(node);
    }

    public boolean isSideEffectPhi() {
        return this.isSideEffectPhi;
    }

    public void setSideEffectPhi() {
        if (this.isSideEffectPhi) {
            return;
        }

        this.isSideEffectPhi = true;
    }

    @Override
    protected String info() {
        String preds = "";

        for (Node node : predecessors()) {
            if (node instanceof Phi) {
                preds += "Phi";
            } else {
                preds += node;
            }
            preds += ",";
        }

        return "[" + preds + "]";
    }
}
