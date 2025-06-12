package edu.kit.kastel.vads.compiler.ir.optimize;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import java.util.HashSet;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RemoveDeadBlocks {

  private Set<Block> deadBlock = new HashSet<>();
  private Set<Block> visited = new HashSet<>();

  public void remove(IrGraph graph) {
    Set<Node> allSuccs = new HashSet<>();
    graph.allSuccessors().values().forEach(allSuccs::addAll);

    Set<Node> blocks = allSuccs.stream().filter(block -> block instanceof Block).collect(Collectors.toSet());

    for (Node node : blocks) {
      dfs(node, graph);
    }

    for (Block block : deadBlock) {
      Set<Block> successors = new HashSet<>();

      for (Map.Entry<Node, SequencedSet<Node>> entry : graph.allSuccessors().entrySet()) {
        Node keyNode = entry.getKey();
        if (keyNode.block() == block) {
          for (Node succ : entry.getValue()) {
            successors.add(succ.block());
          }
        }
      }

      for (Block successor : successors) {
        block.removeSuccessor(successor, graph);
      }
    }
  }

  public void dfs(Node node, IrGraph graph) {
    if (node instanceof Block block) {
      if(visited.contains(block))
        return;

      visited.add(block);

      if (block.equals(graph.startBlock()))
        return;

      for (Node pred : block.predecessors()) {
        dfs(pred.block(), graph);
      }

      if (block.predecessors().stream().map(Node::block).allMatch(p -> deadBlock.contains(p))) {
        deadBlock.add(block);
      }
    }
  }
}
