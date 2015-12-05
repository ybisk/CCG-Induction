package CCGInduction.parser;

import CCGInduction.grammar.Binary;
import CCGInduction.grammar.Grammar;
import CCGInduction.grammar.Rule_Type;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/** 
 * @author bisk1
 */
public class Cell<G extends Grammar> implements Externalizable {
  private static final long serialVersionUID = 11112010;
  public transient boolean Fine = false;
  private ConcurrentHashMap<Long,ConcurrentHashMap<ChartItem<G>, ChartItem<G>>> cats = new ConcurrentHashMap<>();
  public int X;
  public int Y;
  public Chart<G> chart;

  Cell(Chart<G> c, int xy) {
    this.X = xy;
    this.Y = xy;
    this.chart = c;
  }

  Cell(Chart<G> c, int x, int y) {
    this.X = x;
    this.Y = y;
    this.chart = c;
  }

  /**
   * Default constructor
   */
  public Cell() {}

  final Collection<Long> cats() {
    return cats.keySet();
  }

  final Collection<ChartItem<G>> values(long cat) {
    return this.cats.get(cat).values();
  }

  public final Collection<ChartItem<G>> values() {
    HashSet<ChartItem<G>> values = new HashSet<>();
    for (ConcurrentHashMap<ChartItem<G>, ChartItem<G>> map : this.cats.values()) {
      values.addAll(map.values());
    }
    return values;
  }

  public ChartItem<G> addCat(ChartItem<G> ci) {
    return addCatHelper(ci);
  }

  private ChartItem<G> addCatHelper(ChartItem<G> newC) {
    if (newC == null)
      throw new Parser.FailedParsingAssertion("Adding Null chartitem");
    ConcurrentHashMap<ChartItem<G>, ChartItem<G>> temp;
    if ((temp = cats.get(newC.Category)) != null) {
      ChartItem<G> cat;
      if ((cat = temp.get(newC)) != null) {
        return cat;
      }
      temp.put(newC, newC);
      return newC;
    }
    temp = new ConcurrentHashMap<>();
    temp.put(newC, newC);
    cats.put(newC.Category, temp);
    return newC;
  }

  public ChartItem<G> addCat(Binary v, ChartItem<G> B, ChartItem<G> C) {
    ChartItem<G> newC;
    if(v.Type == Rule_Type.FW_PUNCT) {
      newC = addCatHelper(new ChartItem<>(v.A, C.type(), C.arity(), Punctuation.FW, this));
    } else if (v.Type == Rule_Type.BW_PUNCT) {
      newC = addCatHelper(new ChartItem<>(v.A, B.type(), B.arity(), Punctuation.BW, this));
    } else if (v.Type == Rule_Type.FW_CONJOIN) {
      newC = addCatHelper(new ChartItem<>(v.A, C.type(), C.arity(), Punctuation.None, this));
    } else if (v.Type == Rule_Type.BW_CONJOIN && Rule_Type.TR(B.type())) {
      newC = addCatHelper(new ChartItem<>(v.A, B.type(), B.arity(), Punctuation.None, this));
    } else {
      newC = addCatHelper(new ChartItem<>(v.A, v.Type, v.arity, Punctuation.None, this));
    }

    // BackPointers
    if (newC.addChild(v, B, C)) {
      // Number of parses
      newC.parses += (B.parses * C.parses);
    }
    return newC;
  }

  @Override
  public String toString() {
    String ret = "Cell [" + X + ", " + Y + "]\n";
    ret += "Cats:\n";
    if (cats != null) {
      for (ConcurrentHashMap<ChartItem<G>, ChartItem<G>> map : cats.values()) {
        for (ChartItem<G> ci : map.values()) {
          ret += " \n" + ci;
        }
      }
    }
    return ret;
  }

  public void removeUnusedCats() {
    ArrayList<ChartItem<G>> AL;
    for (ConcurrentHashMap<ChartItem<G>, ChartItem<G>> map : cats.values()) {
      AL = new ArrayList<>(map.keySet());
      AL.stream().filter(ci -> !ci.used).forEach(map::remove);
    }
  }

  public boolean isEmpty() {
    return cats.isEmpty();
  }

  void addAllCats(Collection<ChartItem<G>> newCats) {
    newCats.forEach(this::addCatHelper);
  }

  public void addAllCats(HashMap<ChartItem<G>, ChartItem<G>> newCats) {
    addAllCats(newCats.values());
  }

  public boolean contains(long cat) {
    return cats.containsKey(cat);
  }

  public ChartItem<G> getCat(ChartItem<G> c) {
    if (cats.containsKey(c.Category)) {
      return cats.get(c.Category).get(c);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    X = in.readShort();
    Y = in.readShort();
    if (X == -1 || Y == -1) {
      throw new AssertionError("Invalid cell index: -1");
    }
    cats = (ConcurrentHashMap<Long, ConcurrentHashMap<ChartItem<G>, ChartItem<G>>>) in.readObject();
    if (cats != null) {
      for (ConcurrentHashMap<ChartItem<G>, ChartItem<G>> map : cats.values()) {
        for (ChartItem<G> cat : map.values()) {
          if (X == Y) {
            recurse(cat);
          } else {
            cat.cell = this;
            cat.X = X;
            cat.Y = Y;
          }
        }
      }
    }
  }

  private void recurse(ChartItem<G> cat) {
    cat.cell = this;
    cat.X = X;
    cat.Y = Y;
    if (!cat.children.isEmpty()) {
      for (BackPointer<G> t : cat.children) {
        recurse(t.leftChild());
      }
    }
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeShort(X);
    out.writeShort(Y);
    out.writeObject(cats);
  }
}
