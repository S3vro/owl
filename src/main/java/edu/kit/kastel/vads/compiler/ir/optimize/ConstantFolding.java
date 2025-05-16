package edu.kit.kastel.vads.compiler.ir.optimize;

import edu.kit.kastel.vads.compiler.ir.node.*;

public class ConstantFolding implements Optimizer {
    @Override
    public Node transform(Node node) {
        if (node instanceof BinaryOperationNode) {
            if (node.predecessor(0) instanceof ConstIntNode && node.predecessor(1) instanceof ConstIntNode) {
                int cL = ((ConstIntNode) node.predecessor(BinaryOperationNode.LEFT)).value();
                int cR = ((ConstIntNode) node.predecessor(BinaryOperationNode.RIGHT)).value();

                System.out.println(cL + "FOLDED" + cR);

                return switch(node) {
                    case AddNode _ -> new ConstIntNode(node.block(), cL + cR);
                    case SubNode _ -> new ConstIntNode(node.block(), cL - cR);
                    case MulNode _ -> new ConstIntNode(node.block(), cL * cR);
                    default -> node;
                };
            }
        }
        return node;
    }
}
