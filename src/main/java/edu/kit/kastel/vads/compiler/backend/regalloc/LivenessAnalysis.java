package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.Main;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConditionalJumpNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.JmpNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LivenessAnalysis {

    private final Map<Node, Set<Node>> liveAt = new HashMap<>();
    private final Map<Node, Set<Node>> liveOut = new HashMap<>();

    public Map<Node, Set<Node>> getLiveOut() {
        return this.liveOut;
    }

    public  Map<Node, Set<Node>> calculateLiveness(List<Block> blocks) {
        for (Block block : blocks.reversed()) {
            this.calcLive(block);
        }

        this.prettyPrint(blocks);
        return this.liveAt;
    }

    private void prettyPrint(List<Block> blocks) {
        if (Main.DEBUG) {
            System.out.println("-------------LIVENESS-------------");
            for (Block block : blocks) {
                System.out.println(block.getLabel());
                for (Node node : block.nodesWithPhis()) {
                    System.out.println("\t" + node + " | " + liveAt.get(node));
                }
                System.out.println(block.blockExit() + " | " + liveAt.get(block.blockExit()));
            }
        System.out.println("-------------LIVENESS-------------");
        }
    }


    private void calcLive(Block block) {
        // K1 Rule
        for (int i = block.nodesWithExitAndPhi().size() - 1; i >= 0; i--) {
            Node node = block.nodesWithExitAndPhi().get(i);
            this.liveAt.put(node, uses(node));
        }
        // K2 Rule

        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = block.nodesWithExitAndPhi().size() - 1; i >= 0; i--) {
                Node node = block.nodesWithExitAndPhi().get(i);

                Node nextNode = i >= block.nodesWithExitAndPhi().size() - 1 ? null : block.nodesWithExitAndPhi().get(i + 1);

                Set<Node> liveAtSuc = new HashSet<>();
                for (Node succ : succ(node, nextNode)) {
                    liveAtSuc.addAll(this.liveAt.computeIfAbsent(succ, _ -> new HashSet<>()));
                    liveAtSuc.removeAll(defines(node));
                }

                this.liveOut.put(node, liveAtSuc);
                if (this.liveAt.computeIfAbsent(node, _ -> new HashSet<>()).addAll(liveAtSuc))
                    changed = true;
            }
        }

    }

    private Set<Node> succ(Node node, Node succ) {
        return switch(node) {
            case Phi _, BinaryOperationNode _ , ConstIntNode _ -> Set.of(succ);
            case JmpNode jmpNode -> Set.of(jmpNode.target().nodesWithExitAndPhi().get(0));
            case ConditionalJumpNode ifNode -> Set.of(ifNode.getThenBlock().nodesWithExitAndPhi().get(0),
              ifNode.getElseBlock().nodesWithExitAndPhi().get(0)
            );
            default -> Set.of();
        };
    }

    public Set<Node> uses(Node node) {

        return switch (node) {
            case ReturnNode r -> {
                Node usedVal = predecessorSkipProj(r, ReturnNode.RESULT);
                yield new HashSet<>(Set.of(usedVal));
            }

            case ConditionalJumpNode i -> new HashSet<>(Set.of(i.getCondition()));

            case Phi p -> {
                Set<Node> used = new HashSet<>();
                if (p.isSideEffectPhi()) yield used;
                //TODO: this is a dirty suboptimal fix
                for (int i = 0; i < p.predecessors().size(); i++) {
                    used.add(predecessorSkipProj(p, i));
                }

                yield used;
            }

            case BinaryOperationNode b -> {
                Node usedVal1 = predecessorSkipProj(b, BinaryOperationNode.LEFT);
                Node usedVal2 = predecessorSkipProj(b, BinaryOperationNode.RIGHT);
                Set<Node> resultSet = new HashSet<>();
                resultSet.add(usedVal1);
                resultSet.add(usedVal2);

                yield resultSet;
            }

            default -> new HashSet<>();
        };
    }

    public Set<Node> defines(Node node) {
        return switch (node) {
            case BinaryOperationNode b -> Set.of(b);
            case ConstIntNode c -> Set.of(c);
            case Phi p -> Set.of(p);
            default -> Set.of();
        };
    }
}
