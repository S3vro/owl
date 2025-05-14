package edu.kit.kastel.vads.compiler.backend;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*;

import java.util.*;
import java.util.stream.Collectors;

public class LivenessAnalysis {

    private final Map<Node, Set<Integer>> liveAt = new HashMap<>();
    private final Map<Node, Integer> varibaleId = new HashMap<>();

    private int id = 0;

    public Map<Node, Integer> getVaribaleId() {
        return varibaleId;
    }

    public  Map<Node, Set<Integer>> getLiveAt(IrGraph graph) {
        this.generateIds(graph);

        for (Node node : this.graphInSequence(graph)) {
            System.out.println(String.format("%d>%s", varibaleId.get(node), node.toString()));
        }

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
        for (int i = graphSequence.size()-1; i>=0; i--) {
            this.liveAt.put(graphSequence.get(i), new HashSet<>());
            switch(graphSequence.get(i)) {
                case ReturnNode r -> {
                    int nodeId = this.varibaleId.get(
                            r.predecessors().stream()
                                    .filter(LivenessAnalysis::relevant)
                                    .findFirst().get()
                    );
                    this.liveAt.computeIfAbsent(r, _ -> new HashSet<>()).add(nodeId);
                }
                case ConstIntNode c -> {
                    int nodeId = this.varibaleId.get(c);
                    Set<Integer> live = this.liveAt.get(graphSequence.get(i+1)).stream().filter(v -> v != nodeId)
                            .collect(Collectors.toSet());
                    this.liveAt.put(c, live);
                }
                case BinaryOperationNode b -> {

                    List<Node> pred = new ArrayList<>();

                    for (Node p: b.predecessors()) {
                        if (p instanceof StartNode || p instanceof Block) {
                            continue;
                        }
                        if(p instanceof  ProjNode proj) {
                            if (proj.projectionInfo() == ProjNode.SimpleProjectionInfo.RESULT) {
                                pred.add(p.predecessor(0));
                            }
                            continue;
                        }
                        pred.add(p);
                    }

                    if (!pred.isEmpty()) {
                        int nodeIdOp1 = this.varibaleId.get(pred.getFirst());
                        this.liveAt.computeIfAbsent(b, _ -> new HashSet<>()).add(nodeIdOp1);
                    }

                    if (pred.size() > 1) {
                        int nodeIdOp2 = this.varibaleId.get(pred.get(1));
                        this.liveAt.get(b).add(nodeIdOp2);
                    }


                    int nodeId = this.varibaleId.get(b);
                    Set<Integer> live = this.liveAt.get(graphSequence.get(i+1)).stream().filter(v -> v != nodeId)
                            .collect(Collectors.toSet());
                    this.liveAt.get(b).addAll(live);
                }
                default -> {}
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

        System.out.println("All collected Ids: " + varibaleId);
    }

    private void scanId(Node node, Set<Node> visited) {
        if (relevant(node) && !(node instanceof ReturnNode)) {
            varibaleId.put(node, id++);
        }

        for (Node predecessors : node.predecessors()) {
            if (visited.add(predecessors)) {
                scanId(predecessors, visited);
            }
        }
    }

    private static boolean relevant(Node node) {
        return !(node instanceof ProjNode || node instanceof StartNode || node instanceof Block);
    }
}
