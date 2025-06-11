package edu.kit.kastel.vads.compiler.ir.node;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;
import java.util.ArrayList;
import java.util.List;

public final class Phi extends Node {
    private final List<Node> users;
    private boolean isSideEffectPhi = false;
    private Block operationBlock;

    public Phi(Block block, Node... preds) {
        super(block, preds);
        this.users = new ArrayList<>();
    }

    public List<Node> users() {
        return List.copyOf(this.users);
    }

    public void setOperationBlock(Block operationBlock) {
        this.operationBlock = operationBlock;
    }

    public Block operationBlock() {
        return operationBlock;
    }

    public boolean addUser(Node node) {
        return this.users.add(node);
    }

    public void replaceBy(Node node) {
        for (Node user : users) {
            for (int i = 0; i < user.predecessors().size(); i++) {
                if (user.predecessor(i).equals(this)) {
                    user.setPredecessor(i, node);
                }
            }
        }
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
        for (int i = 0; i < this.predecessors().size(); i++) {

            if (predecessorSkipProj(this, i) instanceof Phi phi) {
                phi.setSideEffectPhi();
            }
        }
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
