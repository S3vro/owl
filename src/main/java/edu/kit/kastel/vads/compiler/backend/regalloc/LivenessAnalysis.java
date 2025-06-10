package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

import java.util.*;

public class LivenessAnalysis {

    private final Map<Node, Set<Node>> liveAt = new HashMap<>();
    private final Map<Node, Set<Node>> liveOut = new HashMap<>();


    public Map<Node, Set<Node>> getLiveOut() {
        return this.liveOut;
    }

    public  Map<Node, Set<Node>> getLiveAt(IrGraph graph) {
        this.calcLive(this.graphInSequence(graph));
        this.prettyPrint(graph);
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
        if (visited.add(node.block()))
            scan(node.block(), visited, nodesInSequence);


        if (relevant(node)) nodesInSequence.add(node);
    }

    private void calcLive(List<Node> graphSequence) {
        for (int i = graphSequence.size() - 1; i >= 0; i--) {
            Node node = graphSequence.get(i);

            // K_1 RULE
            this.liveAt.put(node, this.uses(node));

            // K_2 RULE

            if (i < graphSequence.size() - 1) {
                Set<Node> liveAtSucc = new HashSet<>(this.liveAt.get(graphSequence.get(i + 1)));
                this.liveOut.computeIfAbsent(node, _ -> new HashSet<>()).addAll(new HashSet<>(liveAtSucc));
                liveAtSucc.removeAll(this.defines(node));
                this.liveAt.get(node).addAll(liveAtSucc);
            }
        }
    }

    private static boolean relevant(Node node) {
        if (node instanceof Phi phi) return !phi.isSideEffectPhi();
        return !(node instanceof ProjNode || node instanceof StartNode || node instanceof Block);
    }

    public void prettyPrint(IrGraph graph) {
        for (Node node : this.graphInSequence(graph)) {
            System.out.println(String.format("%s | %s", node.toString(), liveAt.get(node)));
        }
    }

    public Set<Node> succ(Node node, Node succ) {
        return switch(node) {
            case ReturnNode _, Phi _, BinaryOperationNode _ -> Set.of(succ);
            case IfNode ifNode -> Set.of(succ, ifNode.getThenBlock(), ifNode.getElseBlock());
            default -> Set.of();
        };
    }

    public Set<Node> uses(Node node) {

        return switch (node) {
            case ReturnNode r -> {
                Node usedVal = predecessorSkipProj(r, ReturnNode.RESULT);
                yield new HashSet<>(Set.of(usedVal));
            }

            case IfNode i -> {
                yield new HashSet<>(Set.of(i.getCondition()));
            }

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
