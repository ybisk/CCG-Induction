package CCGInduction.ccg;

import CCGInduction.experiments.Action;
import CCGInduction.grammar.Rule_Type;
import junit.framework.TestCase;

public class InducedCATTest extends TestCase {
  // Categories used by the tests below.
  private final InducedCAT N = new InducedCAT(InducedCAT.N);           // N
  private final InducedCAT S = new InducedCAT(InducedCAT.S);           // S
  private final InducedCAT PP = new InducedCAT(InducedCAT.PP);         // S
  private final InducedCAT SbN = S.backward(N);                        // S\N
  private final InducedCAT Modal = SbN.backward(SbN);                  // (S\N)\(S\N)
  private final InducedCAT SbNfP = SbN.forward(PP);                    // (S\N)/PP
  private final InducedCAT SbNfN = SbN.forward(N);                     // (S\N)/N
  private final InducedCAT SfS = S.forward(S);                         // S/S
  private final InducedCAT ControlVerb = SbN.forward(SbN).forward(N);  // ((S\N)/(S\N))/N

  /**
   * Test for writing categories to strings
   * @throws Exception
   */
  public void testToString() throws Exception {
    // Write feature
    SbN.addFeat(InducedCAT.conj);
    assertEquals("S\\N[conj]", SbN.toString());

    // Write internal feature
    SbNfN.Res.addFeat(InducedCAT.conj);
    assertEquals("(S\\N[conj])/N", SbNfN.toString());

    // Write X|X not a modifier
    assertEquals("S/S", SfS.toString());

    // Write X|X as a modifier
    SfS.modifier = true;
    assertEquals("S/.S", SfS.toString());
  }

  /**
   * Test for reading categories from strings
   * @throws Exception
   */
  public void testValueOf() throws Exception {
    // Read in modifier
    SfS.modifier = true;
    assertEquals(InducedCAT.valueOf("S/S"), SfS);

    // Read in a control verb (not a modifier)
    assertEquals(InducedCAT.valueOf("((S\\N)/(S\\N))/N"), ControlVerb);
    // Read in a modal (not a modifier)
    assertEquals(InducedCAT.valueOf("(S\\N)\\(S\\N)"), Modal);
    // FIXME:  In CCGbank these would be modifiers since they do not have features

    // Read in category with parens
    assertEquals(InducedCAT.valueOf("(S\\N)/N"), SbNfN);

    // PP atomic
    assertEquals(InducedCAT.valueOf("(S\\N)/PP"), SbNfP);

    // Unknown category
    assertNull(InducedCAT.valueOf("X/X"));

    // Unknown Slash
    assertNull(InducedCAT.valueOf("N|N"));

    // Read category with a feature
    InducedCAT SbNconj = S.backward(N);
    SbNconj.has_conj = true;
    assertEquals(SbNconj, InducedCAT.valueOf("S\\N[conj]"));

    InducedCAT SbNP = (new InducedCAT(InducedCAT.S)).backward(new InducedCAT(InducedCAT.NP));
    InducedCAT Sdcl_b_NP_f_Sdcl = SbNP.forward(new InducedCAT(InducedCAT.S));
    Sdcl_b_NP_f_Sdcl.Arg.addFeat(new CCGAtomic("dcl"));
    Sdcl_b_NP_f_Sdcl.Res.Res.addFeat(new CCGAtomic("dcl"));
    assertEquals(Sdcl_b_NP_f_Sdcl, InducedCAT.valueOf("(S[dcl]\\NP)/S[dcl]"));

    // Unknown feature
    InducedCAT tmp = InducedCAT.valueOf("S\\N[Y]");
    SbN.Arg.addFeat(new CCGAtomic("Y"));
    assertEquals(SbN, tmp);

    // Only conj can attach to a full non-atomic category
    SbN.Arg.CCGbank_feature = null;
    SbN.addFeat(new CCGAtomic("Y"));
    assertEquals(InducedCAT.valueOf(SbN.toString()).toString(), SbN.toString());  // Same string
    assertFalse(InducedCAT.valueOf(SbN.toString()).equals(SbN));                  // Different category
  }

  /**
   * Test for generalized composition
   * @throws Exception
   */
  public void testGenComp() throws Exception {
    int arity = 0;
    Rule_Type direction = Rule_Type.FW_COMPOSE;


    // B0 should return null
    SfS.modifier = true;
    assertNull(InducedCAT.GenComp(SfS, SbN, direction, 0, arity, Action.B0));

    // B1 Composition
    arity = 1;
    assertEquals(InducedCAT.GenComp(SfS, SbN, direction, 0, arity, Action.B1Mod), SbN);
    // Compose modifiers and get a modifier
    assertEquals(InducedCAT.GenComp(SfS, SfS, direction, 0, arity, Action.B1Mod), SfS);

    // B2 Composition
    arity = 2;
    assertEquals(InducedCAT.GenComp(SfS, SbNfN, direction, 0, arity, Action.B2Mod), SbNfN);
    // Complex argument
    InducedCAT SbNmod = SbN.forward(SbN);
    assertEquals(InducedCAT.GenComp(SbNmod, ControlVerb, direction, 0, arity, Action.B2ModTR), ControlVerb);
  }
}