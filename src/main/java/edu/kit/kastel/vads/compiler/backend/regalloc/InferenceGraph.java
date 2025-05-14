package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.backend.LivenessAnalysis;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.*;

public class InferenceGraph {


    private Map<InferenceGraphNode, Set<InferenceGraphNode>> adjacencyList = new HashMap<>();

    public Map<Node, Integer> generate(IrGraph graph){
        LivenessAnalysis livenessAnalysis = new LivenessAnalysis();
        Map<Node, Set<Integer>> live = livenessAnalysis.getLiveAt(graph);
        Map<Node, Integer> varId = livenessAnalysis.getVaribaleId();

        varId.forEach((_, integer) -> {
            this.adjacencyList.put(new InferenceGraphNode(integer), new HashSet<>());
        });

        live.values().forEach(liveTogether ->
                liveTogether.forEach(node ->
                        liveTogether.stream()
                                        .filter(node2 -> !node.equals(node2))
                                .forEach(node2 ->
                                        this.connect(new InferenceGraphNode(node),
                                                new InferenceGraphNode(node2)))));

        return varId;
    }

    public void connect(InferenceGraphNode a, InferenceGraphNode b) {
        this.adjacencyList.computeIfAbsent(a, _ -> new HashSet<>()).add(b);
        this.adjacencyList.computeIfAbsent(b, _ -> new HashSet<>()).add(a);
    }

    public boolean neighbour(InferenceGraphNode a, InferenceGraphNode b){
        return this.adjacencyList.getOrDefault(a, new HashSet<>()).contains(b);
    }

    public InferenceGraphNode[] maxCardinalitySearch() {
        InferenceGraphNode[] nodes = new InferenceGraphNode[this.adjacencyList.size()];
        Set<InferenceGraphNode> W = new HashSet<>(adjacencyList.keySet());
        for (int i = 0; i < nodes.length; i++) {
            InferenceGraphNode v = W.stream().max(Comparator.comparing(InferenceGraphNode::getWeight)).get();
            nodes[i] = v;
            for (InferenceGraphNode node : W) {
                if (neighbour(v, node)) {
                    node.setWeight(node.getWeight() + 1);
                }
            }
            W.remove(v);
        }

        return nodes;
    }

    public Map<InferenceGraphNode, Integer> greedyColor(InferenceGraphNode[] order) {
        Map<InferenceGraphNode, Integer> colorMap = new HashMap<>();

        for (InferenceGraphNode v : order) {
            Set<Integer> usedColors = new HashSet<>();

            for (InferenceGraphNode w : adjacencyList.get(v)) {
                if (colorMap.containsKey(w)) {
                    usedColors.add(colorMap.get(w));
                }

                int color = 0;
                while(usedColors.contains(color)) {
                    color++;
                }

                colorMap.put(v, color);
            }

            if (adjacencyList.get(v).isEmpty()) {
                colorMap.put(v, 0);
            }
        }
        return colorMap;
    }

    public static class InferenceGraphNode {
        private final int id;
        private int weight;

        public InferenceGraphNode(int id) {
            this.id = id;
            this.weight = 0;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof InferenceGraphNode other) {
                return this.id == other.id;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            return this.id + "";
        }
    }

}
