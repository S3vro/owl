package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InferenceGraph {


    private Map<InferenceGraphNode, Set<InferenceGraphNode>> adjacencyList = new HashMap<>();

    public void generate(List<Block> blocks){
        LivenessAnalysis livenessAnalysis = new LivenessAnalysis();
        Map<Node, Set<Node>> live = livenessAnalysis.calculateLiveness(blocks);

        live.keySet().forEach(node -> {
            this.adjacencyList.put(new InferenceGraphNode(node), new HashSet<>());
        });

        for (Set<Node> liveTogether : live.values()) {
            for (Node a : liveTogether) {
                for (Node b : liveTogether) {
                    if (!a.equals(b)) {
                        this.connect(new InferenceGraphNode(a), new InferenceGraphNode(b));
                    }
                }
            }
        }

        // Add Edge to live out vars in case the variable is not used
        livenessAnalysis.getLiveOut().forEach((node, liveOut) -> {
            for (Node out : liveOut) {
                if (!out.equals(node)) {
                    this.connect(new InferenceGraphNode(node), new InferenceGraphNode(out));
                }
            }
        });
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
        private final Node node;
        private int weight;

        public InferenceGraphNode(Node node) {
            this.node = node;
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
                return this.node.equals(other.node) ;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return node.hashCode();
        }

        @Override
        public String toString() {
            return node.toString();
        }
    }

}
