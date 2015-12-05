package CCGInduction.evaluation;

/**
   * Datastructure for storing indices, category and argument
   */
class Edge {
    final int from;
    final int to;
    final int arg;
    final String cat;
    Edge(int head, int dep, String category, int slot, EdgeType edgeType) {
      switch (edgeType) {
        case PARG:
        case CANDC:
          // to   from  cat  arg
          to = dep;
          from = head;
          cat = category;
          arg = slot;
          break;
        case CONLL:
          // Will throw an exception if missing UPOS column
          to = dep;
          arg = -1;     // No Argument
          from = head;
          cat = category;    // Use dependency label as category
          break;
        default:
          throw new AssertionError("Invalid Edge Type: " + edgeType);
      }
    }

    public String toString() {
      if (arg != -1)
        return String.format("%2d  %2d  %-30s", from, to, cat);

      return String.format("%2d  %2d  %-30s  %2d", from, to, cat, arg);
    }
}