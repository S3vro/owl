package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.Main;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConditionalJumpNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstBoolNode;
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
import java.util.Objects;
import java.util.Set;

public class LivenessAnalysis {

    private final Map<LivenessKey, Set<Node>> liveAt = new HashMap<>();
    private final Map<LivenessKey, Set<Node>> liveOut = new HashMap<>();

    public Map<LivenessKey, Set<Node>> getLiveOut() {
        return this.liveOut;
    }

    public  Map<LivenessKey, Set<Node>> calculateLiveness(List<Block> blocks) {
        this.calcLive(blocks.reversed());
        this.prettyPrint(blocks);
        return this.liveAt;
    }

    private void prettyPrint(List<Block> blocks) {
        if (Main.DEBUG) {
            System.out.println("-------------LIVENESS-------------");
            for (Block block : blocks) {
                System.out.println(block.getLabel());
                for (Node node : block.nodesWithPhis()) {
                    System.out.println("\t" + node + " | " + liveAt.get(new LivenessKey(block, node)));
                }
                System.out.println(block.blockExit() + " | " + liveAt.get(new LivenessKey(block, block.blockExit())));
            }
        System.out.println("-------------LIVENESS-------------");
        }
    }


    private void calcLive(List<Block> blocks) {
        // K1 Rule
        for (Block block : blocks) {
            for (int i = block.nodesWithExitAndPhi().size() - 1; i >= 0; i--) {
                Node node = block.nodesWithExitAndPhi().get(i);
                this.liveAt.put(new LivenessKey(block, node), uses(new LivenessKey(block, node)));
            }
        }

        // K2 Rule

        for (Block block : blocks) {
            boolean changed = true;
            while (changed) {
                changed = false;
                for (int i = block.nodesWithExitAndPhi().size() - 1; i >= 0; i--) {
                    Node node = block.nodesWithExitAndPhi().get(i);

                    Node nextNode = i >= block.nodesWithExitAndPhi().size() - 1 ? null : block.nodesWithExitAndPhi().get(i + 1);

                    Set<Node> liveAtSuc = new HashSet<>();
                    for (LivenessKey succ : succ(node, nextNode, block)) {
                        liveAtSuc.addAll(this.liveAt.computeIfAbsent(succ, _ -> new HashSet<>()));
                        liveAtSuc.removeAll(defines(node));
                    }

                    /*
                    Set<Node> toRemove = this.liveAt.computeIfAbsent(new LivenessKey(block, node), _ -> new HashSet<>()).stream().filter(
                      live -> !uses(new LivenessKey(block, node)).contains(live) && !liveAtSuc.contains(live)
                    ).collect(Collectors.toSet());
                    if(this.liveAt.get(new LivenessKey(block, node)).removeAll(toRemove)) {
                        System.out.println("Removed Elems: " + toRemove + " because " + uses(new LivenessKey(block, node)) + " in " + node.block());
                    }
                    */

                    this.liveOut.put(new LivenessKey(block, node), liveAtSuc);
                    changed = this.liveAt.computeIfAbsent(new LivenessKey(block, node), _ -> new HashSet<>()).addAll(liveAtSuc);
                }
            }
        }

    }

    private Set<LivenessKey> succ(Node node, Node succ, Block block) {
        return switch(node) {
            case Phi _, BinaryOperationNode _ , ConstIntNode _, ConstBoolNode _ -> Set.of(new LivenessKey(block, succ));
            case JmpNode jmpNode -> Set.of(new LivenessKey(jmpNode.target(), jmpNode.target().nodesWithExitAndPhi().get(0)));
            case ConditionalJumpNode ifNode -> Set.of(
              new LivenessKey(ifNode.getThenBlock(),ifNode.getThenBlock().nodesWithExitAndPhi().get(0)),
              new LivenessKey(ifNode.getElseBlock(),ifNode.getElseBlock().nodesWithExitAndPhi().get(0))
            );
            case ReturnNode _ -> Set.of();
            default -> throw new UnsupportedOperationException(node + " is not handled");
        };
    }

    public Set<Node> uses(LivenessKey key) {

        return switch (key.node()) {
            case ReturnNode r -> {
                Node usedVal = predecessorSkipProj(r, ReturnNode.RESULT);
                yield new HashSet<>(Set.of(usedVal));
            }

            case ConditionalJumpNode i -> new HashSet<>(Set.of(i.getCondition()));

            case Phi p -> {
                Set<Node> used = new HashSet<>();
                if (p.isSideEffectPhi()) yield used;

                int index = key.block().phiIndex(p);
                used.add(predecessorSkipProj(p, index));
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
            case ConstIntNode c-> Set.of(c);
            case ConstBoolNode c-> Set.of(c);
            case Phi p -> Set.of(p);
            default -> Set.of();
        };
    }

    public record LivenessKey(Block block, Node node) {

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof LivenessKey that)) return false;
          return Objects.equals(node, that.node) && Objects.equals(block, that.block);
        }

        @Override
        public int hashCode() {
            return Objects.hash(block, node);
        }
    }
}
