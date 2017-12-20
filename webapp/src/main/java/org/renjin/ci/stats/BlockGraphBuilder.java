package org.renjin.ci.stats;

import org.renjin.ci.datastore.RenjinVersionTotals;

/**
 * Generates a simple SVG diagram to show package progress
 */
public class BlockGraphBuilder {

  private static final int ROWS = 10;
  private static final int COLUMNS = 50;

  private static final int SIZE = 5;
  private static final int PADDING = 2;

  private static final int BLOCK_HEIGHT = (ROWS * SIZE) + ((ROWS - 1) * PADDING);
  private static final int BLOCK_WIDTH = (COLUMNS * SIZE) + ((COLUMNS - 1) * PADDING);

  private RenjinVersionTotals totals;
  private double countPerBlock;

  public BlockGraphBuilder(RenjinVersionTotals totals) {
    this.totals = totals;
    this.countPerBlock = totals.getTotalCount() / (COLUMNS * ROWS);
  }

  public String draw() {
    StringBuilder svg = new StringBuilder();
    svg.append(String.format("<svg version=\"1.1\" baseProfile=\"full\" " +
        "width=\"100%%\" " +
        "viewBox=\"0 0 %d %d\" " +
        "xmlns=\"http://www.w3.org/2000/svg\">\n",
        BLOCK_WIDTH,
        BLOCK_HEIGHT));

    svg.append("<style>\n");
    svg.append(".grade-a { fill: #27AE61; }\n");
    svg.append(".grade-b { fill: #61C12D; }\n");
    svg.append(".grade-c { fill: #D4BF34; }\n");
    svg.append(".grade-f { fill: #E74C3C; }\n");
    svg.append("</style>\n");

    double cumulative = 0;

    for (int col = 0; col < COLUMNS; col++) {
      for (int row = 0; row < ROWS; row++) {
        int x = col * (SIZE+PADDING);
        int y = row * (SIZE+PADDING);
        svg.append(String.format("<rect class=\"%s\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"/>\n",
            classOf(cumulative),
            x, y,
            SIZE, SIZE));

        cumulative += countPerBlock;
      }
    }
    svg.append("</svg>");
    return svg.toString();
  }

  private String classOf(double cumulative) {
    if(cumulative < totals.getA()) {
      return "grade-a";
    } else if(cumulative < (totals.getA() + totals.getB())) {
      return "grade-b";
    } else if(cumulative < (totals.getA() + totals.getB() + totals.getC())) {
      return "grade-c";
    } else {
      return "grade-f";
    }
  }
}
