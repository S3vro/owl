package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.ir.IrGraph;

public final class Block extends Node {

    private final String label;

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

}
