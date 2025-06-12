package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

    public List<Node> nodesWithPhis() {
        if (blockExit == null) return List.copyOf(nodes);
        List<Node> result = new ArrayList<>(List.copyOf(nodes));
        result.addAll(new ArrayList<>(this.phis.keySet()).reversed());

        return result;
    }

    public List<Node> nodesWithExitAndPhi() {
        if (blockExit == null) return List.copyOf(nodes);

        List<Node> result = new ArrayList<>(List.copyOf(nodes));
        result.addAll(new ArrayList<>(this.phis.keySet()).reversed());
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
