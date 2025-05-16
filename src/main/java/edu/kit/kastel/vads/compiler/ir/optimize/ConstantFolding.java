package edu.kit.kastel.vads.compiler.ir.optimize;

import edu.kit.kastel.vads.compiler.ir.node.*;

public class ConstantFolding implements Optimizer {
    @Override
    public Node transform(Node node) {
        if (node instanceof BinaryOperationNode) {
            if (node.predecessor(BinaryOperationNode.LEFT) instanceof ConstIntNode
                    && node.predecessor(BinaryOperationNode.RIGHT) instanceof ConstIntNode) {
                int cL = ((ConstIntNode) node.predecessor(BinaryOperationNode.LEFT)).value();
                int cR = ((ConstIntNode) node.predecessor(BinaryOperationNode.RIGHT)).value();

                return switch(node) {
                    case AddNode _ -> new ConstIntNode(node.block(), cL + cR);
                    case SubNode _ -> new ConstIntNode(node.block(), cL - cR);
                    case MulNode _ -> new ConstIntNode(node.block(), cL * cR);
                    case DivNode _ -> {
                        if (cR == 0) yield node;
                        if (cL == Integer.MIN_VALUE && cR == -1) yield node;
                        yield new ConstIntNode(node.block(), cL / cR);
                    }
                    case ModNode _ -> {
                        if (cR == 0) yield node;
                        if (cL == Integer.MIN_VALUE && cR == -1) yield node;
                        yield new ConstIntNode(node.block(), cL % cR);
                    }
                    default -> node;
                };
            }
        }
        return node;
    }
}
