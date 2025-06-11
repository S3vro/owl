package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NodeCollector {

  private final IrGraph graph;
  private final List<Block> blocks = new ArrayList<>();

  public NodeCollector(IrGraph graph) {
    this.graph = graph;
  }

  public List<Block> collect() {
    Set<Node> visited = new HashSet<>();
      visited.add(graph.endBlock());
      markSideEffectPhis(graph.endBlock(),  visited);
      visited.clear();


    visited.add(graph.endBlock());
    scan(graph.endBlock(), visited);

    blocks.forEach(this::sort);

    return blocks;
  }
  private void sort(Block b) {
    for (int i = 0; i < b.nodes().size(); i++) {
        for (int j = 0; j < b.nodes().size(); j++) {
          Node n1 = b.nodes().get(i);
          Node n2 = b.nodes().get(j);

          if (n1.predecessors().contains(n2) && i < j) {
            //throw new IllegalArgumentException("Reordering Problem:" + n1 + " < " + n2);
          }
        }
    }
  }

  private void markSideEffectPhis(Node node, Set<Node> visited) {
    if (node instanceof Phi phi) {
      if (isSideEffectPhi(phi, new HashSet<>(Set.of(phi)))) {
        phi.setSideEffectPhi();
      }
    }

    for (Node pred : node.predecessors()) {
      if (visited.add(pred)) {
        markSideEffectPhis(pred, visited);
      }
    }

    if (visited.add(node.block()))
      markSideEffectPhis(node.block(), visited);
  }

  private boolean isSideEffectPhi(Node node , Set<Node> visited) {
    if (node instanceof ProjNode proj) {
      return proj.projectionInfo() == ProjNode.SimpleProjectionInfo.SIDE_EFFECT;
    }

    boolean isSideEffectPhi = false;
    for (Node pred : node.predecessors()) {
      if (visited.add(pred)) {
        if (isSideEffectPhi(pred, visited)) {
          isSideEffectPhi = true;
        }
      }
    }

    return isSideEffectPhi;
  }

  private void scan(Node node, Set<Node> visited) {

    //The order is important. Visit non sideeffect first!
    for (Node predecessor : node.predecessors()) {
      if (!(predecessor instanceof ProjNode projNode) || projNode.projectionInfo() != ProjNode.SimpleProjectionInfo.SIDE_EFFECT) {
      if (visited.add(predecessor))
        scan(predecessor, visited);
      }
    }

    if (visited.add(node.block()))
      scan(node.block(), visited);

    for (Node predecessor : node.predecessors()) {
      if (visited.add(predecessor)) {
        scan(predecessor, visited);
      }
    }

    if (node instanceof Block block) {
      this.blocks.add(block);
    }

    if (!ignore(node)) {
      node.block().addNode(node);
    }

    if (node instanceof Phi phi && !phi.isSideEffectPhi()) {
      for (int i = 0; i < phi.predecessors().size(); i++) {
        Block block = phi.block().predecessor(i).block();
        block.addPhi(phi, i);
      }
    }
  }

  private boolean ignore(Node node) {
    return node instanceof ProjNode || node instanceof StartNode || node instanceof Block || node instanceof Phi;
  }
}
