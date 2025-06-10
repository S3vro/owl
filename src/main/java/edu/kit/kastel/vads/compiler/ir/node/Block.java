package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.ir.IrGraph;

public final class Block extends Node {

    private final String label;
    private boolean ignoreTopoSort = false;

    public Block(IrGraph graph, String label) {
        super(graph);

        this.label = label;
    }

    public boolean getIgnoreTopoSort() {
        return ignoreTopoSort;
    }

    public void setIgnoreTopoSort(boolean ignoreTopoSort) {
        this.ignoreTopoSort = ignoreTopoSort;
    }

    public String getLabel() {
        return label;
    }

    public String info() {
        return label;
    }

}
