package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
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
    Node retNode = graph.endBlock().predecessors().stream().filter(n -> n instanceof ReturnNode).findAny().get();
    Node sideEffectPhi = retNode.predecessor(ReturnNode.SIDE_EFFECT);
    visited.add(sideEffectPhi);
    markSideEffectPhis(sideEffectPhi,  visited);

    visited.clear();
    visited.add(graph.endBlock());
    scan(graph.endBlock(), visited);

    return blocks;
  }

  private boolean markSideEffectPhis(Node node, Set<Node> visited) {
    if (node instanceof ProjNode proj) {
      return proj.projectionInfo() == ProjNode.SimpleProjectionInfo.SIDE_EFFECT;
    }

    boolean sideEffectPhi = false;
    for(Node preds : node.predecessors()) {
      if (visited.add(preds)) {
        sideEffectPhi = markSideEffectPhis(preds, visited);
      }
    }

    if (node.predecessors().stream().filter(n -> n instanceof Phi).anyMatch(phi -> ((Phi) phi).isSideEffectPhi())) {
      sideEffectPhi = true;
    }

    if (sideEffectPhi) {
      if (node instanceof Phi phi) {
        phi.setSideEffectPhi();
      }
    }
    return sideEffectPhi;
  }

  private void scan(Node node, Set<Node> visited) {

    for (Node predecessor : node.predecessors().reversed()) {
      if (visited.add(predecessor)) {
        scan(predecessor, visited);
      }
    }

    if (visited.add(node.block()))
      scan(node.block(), visited);


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
