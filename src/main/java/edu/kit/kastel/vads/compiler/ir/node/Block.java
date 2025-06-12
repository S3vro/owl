package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import org.jspecify.annotations.Nullable;

public final class Block extends Node {

    private final String label;
    private List<Node> nodes = new ArrayList<>();
    private final HashMap<Phi, Integer> phis = new LinkedHashMap<>();
    private @Nullable ControlFlowNode blockExit = null;

    public Block(IrGraph graph, String label) {
        super(graph);

        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String info() {
        return label;
    }

    public List<Node> nodes() {
        return nodes;
    }


    public void addPhi(Phi phi, int index) {
        this.phis.put(phi, index);
    }

    public int phiIndex(Phi phi) {
        return this.phis.get(phi);
    }

    public void removeSuccessor(Node node, IrGraph irGraph) {
        List<Map.Entry<Node, Node>> edges = new ArrayList<>();

        for (Map.Entry<Node, SequencedSet<Node>> entry : irGraph.allSuccessors().entrySet()) {
            Node pred = entry.getKey();
            for (Node succ : entry.getValue()) {
                if (pred.block() == this &&
                  (succ instanceof Phi || succ instanceof Block)) {
                    edges.add(new AbstractMap.SimpleEntry<>(pred, succ));
                }
            }
        }

        for (Map.Entry<Node, Node> edge : edges) {
            Node pred = edge.getKey();
            Node succ = edge.getValue();
            succ.removePredecessor(pred);
        }
    }


    public void orderPhis(List<Phi> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < nodes.size(); j++) {
                if (nodes.get(i).predecessors().contains(nodes.get(j)) && i > j) {
                    Phi tmp = nodes.get(j);
                    nodes.set(j, nodes.get(i));
                    nodes.set(i, tmp);
                }
            }
        }
    }

    public List<Node> nodesWithPhis() {
        if (blockExit == null) return List.copyOf(nodes);
        List<Node> result = new ArrayList<>(List.copyOf(nodes));
        List<Phi> reversed = new ArrayList<>(this.phis.keySet()).reversed();
        orderPhis(reversed);
        result.addAll(reversed);

        return result;
    }

    public List<Node> nodesWithExitAndPhi() {
        if (blockExit == null) return List.copyOf(nodes);

        List<Node> result = new ArrayList<>(List.copyOf(nodes));
        List<Phi> reversed = new ArrayList<>(this.phis.keySet()).reversed();
        orderPhis(reversed);
        result.addAll(reversed);
        result.add(blockExit);

        return result;
    }

    public ControlFlowNode blockExit() {
        return blockExit;
    }

    public void addNode(Node node) {
        if (node instanceof ControlFlowNode exitNode) {
            if (this.blockExit instanceof ReturnNode)  {
                System.out.println(node);
                return;
            }
            if (blockExit != null) {
                // Return Nodes are stronger than jumps
                if (!(exitNode instanceof ReturnNode)) {
                    return;
                }
            }
            this.blockExit = exitNode;
            return;
        }

        this.nodes.add(node);
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }
}
