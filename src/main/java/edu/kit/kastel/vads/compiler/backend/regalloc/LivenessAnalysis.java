package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

import java.util.*;

public class LivenessAnalysis {

    private final Map<Node, Set<Integer>> liveAt = new HashMap<>();
    private final Map<Node, Integer> varibaleId = new HashMap<>();

    public Map<Node, Integer> getVaribaleId() {
        return varibaleId;
    }

    public  Map<Node, Set<Integer>> getLiveAt(IrGraph graph) {
        this.generateIds(graph);

        this.calcLive(this.graphInSequence(graph));

        return this.liveAt;
    }

    public List<Node> graphInSequence(IrGraph graph) {
        List<Node> nodesInSequence = new ArrayList<>();

        Set<Node> visited = new HashSet<>();
        visited.add(graph.endBlock());
        scan(graph.endBlock(), visited, nodesInSequence);

        return nodesInSequence;
    }

    private void scan(Node node, Set<Node> visited, List<Node> nodesInSequence) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited, nodesInSequence);
            }
        }

        if (relevant(node)) nodesInSequence.add(node);
    }

    private void calcLive(List<Node> graphSequence) {
        for (int i = graphSequence.size() - 1; i >= 0; i--) {
            Node node = graphSequence.get(i);

            // K_1 RULE
            this.liveAt.put(node, this.uses(node));

            // K_2 RULE

            if (i < graphSequence.size() - 1) {
                Set<Integer> liveAtSucc = new HashSet<>(this.liveAt.get(graphSequence.get(i + 1)));
                liveAtSucc.removeAll(this.defines(node));
                this.liveAt.get(node).addAll(liveAtSucc);
            }
        }
    }

    private void generateIds(IrGraph graph) {

        List<Node> ordered = graphInSequence(graph);
        for (int i = 0; i < ordered.size(); i++) {
            if (!(ordered.get(i) instanceof ReturnNode)) {
                this.varibaleId.put(ordered.get(i), i);
            }
        }
    }

    private static boolean relevant(Node node) {
        return !(node instanceof ProjNode || node instanceof StartNode || node instanceof Block);
    }

    public void prettyPrint(IrGraph graph) {
        for (Node node : this.graphInSequence(graph)) {
            System.out.println(String.format("%d>%s | %s", varibaleId.get(node), node.toString(), liveAt.get(node)));
        }
    }

    public Set<Integer> uses(Node node) {
        
        return switch (node) {
            case ReturnNode r -> {
                Node usedVal = predecessorSkipProj(r, ReturnNode.RESULT);
                yield new HashSet<>(Set.of(this.varibaleId.get(usedVal)));
            }

            case BinaryOperationNode b -> {
                Node usedVal1 = predecessorSkipProj(b, BinaryOperationNode.LEFT);
                Node usedVal2 = predecessorSkipProj(b, BinaryOperationNode.RIGHT);
                Set<Integer> used = new HashSet<>();

                used.add(this.varibaleId.get(usedVal1));
                used.add(this.varibaleId.get(usedVal2));

                yield used;
            }

            default -> new HashSet<>();
        };
    }

    public Set<Integer> defines(Node node) {
        return switch (node) {
            case BinaryOperationNode b -> Set.of(this.varibaleId.get(b));
            case ConstIntNode c -> Set.of(this.varibaleId.get(c));
            default -> Set.of();
        };
    }
}
