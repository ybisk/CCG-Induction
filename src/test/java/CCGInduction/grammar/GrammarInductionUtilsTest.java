package CCGInduction.grammar;

import CCGInduction.Configuration;
import CCGInduction.ccg.InducedCAT;
import junit.framework.TestCase;

import java.util.concurrent.ConcurrentHashMap;

public class GrammarInductionUtilsTest extends TestCase {

  private final InducedCAT N = new InducedCAT(InducedCAT.N);
  private final InducedCAT NfN = N.forward(N);
  private final InducedCAT NbN = N.backward(N);
  private final InducedCAT NfNfNfN = NfN.forward(NfN);
  private final InducedCAT NbNbNbN = NbN.backward(NbN);
  private final InducedCAT NfNfNfN_f_NfNfNfN = NfNfNfN.forward(NfNfNfN);
  private final InducedCAT NbNbNbN_b_NbNbNbN = NbNbNbN.backward(NbNbNbN);
  private final InducedCAT S = new InducedCAT(InducedCAT.S);
  private final InducedCAT SfN = S.forward(N);
  private final InducedCAT SbN = S.backward(N);
  private final InducedCAT SfNfSfN = SfN.forward(SfN);
  private final InducedCAT SbNbSbN = SbN.backward(SbN);

  public void testOKArgument() throws Exception {
    // Arguments cannot have features
    InducedCAT Nconj = new InducedCAT(InducedCAT.N);
    Nconj.addFeat(InducedCAT.conj);
    assertFalse(OKArgument(N, Nconj));

    // Max arity
    Configuration.maxArity = 2;
    assertFalse(OKArgument(SfNfSfN, N));
    Configuration.maxArity = 3;

    // Modifiers
    NfN.modifier = true;
    assertFalse(OKArgument(NfN, NfN));
    assertFalse(OKArgument(N, N));

    // N is not allowed to take arguments
    assertFalse(OKArgument(N, S));
    assertTrue(OKArgument(S, N));

    // Conj canot be an argument
    InducedCAT conj = new InducedCAT(InducedCAT.conj);
    assertFalse(OKArgument(N, conj));

    // Atomics can only take atomics
    Configuration.complexArgs = true;
    assertFalse(OKArgument(N, SfN));
    assertTrue(OKArgument(SfN, N));
    // Can't take a complex argument if you have one
    assertFalse(OKArgument(SfNfSfN, SfN));

    // Max modifier arity applies to categories that contain modifiers
    assertTrue(OKArgument(NbNbNbN, N));
    NbNbNbN.modifier = true;
    NbNbNbN.Arg.modifier = true;
    NbNbNbN.Res.modifier = true;
    assertFalse(OKArgument(NbNbNbN, N));

    // Control verbs require base's arg to be atomic
    InducedCAT SbNfN = SbN.forward(N);
    assertTrue(OKArgument(SbN, SfN));
    assertTrue(OKArgument(SbN, SbN));
    assertTrue(OKArgument(SbNfN,SbN));
    Configuration.maxArity = 4;
    assertFalse(OKArgument(SbNbSbN, SbNbSbN));

    // Complex Args flag
    Configuration.complexArgs = false;
    assertFalse(OKArgument(SbN, SbN));
    Configuration.complexArgs = true;
    assertTrue(OKArgument(SbN, SbN));

    // Modifiers cannot be arguments
    NbN.modifier = true;
    assertFalse(OKArgument(SbN,NbN));
  }

  public void testRight_Modify() throws Exception {
    ConcurrentHashMap<InducedCAT, Boolean> inducedCategories = new ConcurrentHashMap<>(5);
    InducedCAT rightCategory = N;

    NfN.modifier = true;
    Right_Modify(inducedCategories, rightCategory);
    assertTrue(contains(inducedCategories, NfN));

    NfNfNfN.modifier = true;
    Right_Modify(inducedCategories, NfN);
    assertTrue(contains(inducedCategories, NfNfNfN));

    // (S/N)/(S/N) should not be introduced as a modifier
    SfNfSfN.modifier = true;
    Right_Modify(inducedCategories, SfN);
    assertFalse(contains(inducedCategories, SfNfSfN));

    // Too big for Max modifier arity
    Configuration.maxModArity = 2;
    NfNfNfN_f_NfNfNfN.modifier = true;
    Right_Modify(inducedCategories, NfNfNfN);
    assertFalse(contains(inducedCategories, NfNfNfN_f_NfNfNfN));

    // OK Max modifier arity
    Configuration.maxModArity = 3;
    NfNfNfN_f_NfNfNfN.modifier = true;
    Right_Modify(inducedCategories, NfNfNfN);
    assertTrue(contains(inducedCategories, NfNfNfN_f_NfNfNfN));
  }

  public void testLeft_Modify() throws Exception {
    ConcurrentHashMap<InducedCAT, Boolean> inducedCategories = new ConcurrentHashMap<>(5);
    InducedCAT rightCategory = N;

    NbN.modifier = true;
    Left_Modify(inducedCategories, rightCategory);
    assertTrue(contains(inducedCategories, NbN));

    NbNbNbN.modifier = true;
    Left_Modify(inducedCategories, NbN);
    assertTrue(contains(inducedCategories, NbNbNbN));

    // (S\N)\(S\N) should not be introduced as a modifier
    SbNbSbN.modifier = true;
    Left_Modify(inducedCategories, SbN);
    assertFalse(contains(inducedCategories, SbNbSbN));

    // Too big for Max modifier arity
    Configuration.maxModArity = 2;
    NbNbNbN_b_NbNbNbN.modifier = true;
    Left_Modify(inducedCategories, NbNbNbN);
    assertFalse(contains(inducedCategories, NbNbNbN_b_NbNbNbN));

    // OK Max modifier arity
    Configuration.maxModArity = 3;
    NbNbNbN_b_NbNbNbN.modifier = true;
    Left_Modify(inducedCategories, NbNbNbN);
    assertTrue(contains(inducedCategories, NbNbNbN_b_NbNbNbN));
  }

  public void testRight_Arg() throws Exception {
    ConcurrentHashMap<InducedCAT, Boolean> inducedCategories = new ConcurrentHashMap<>(5);

    // Allow (S\N)/N and (S\N)/S
    Right_Arg(inducedCategories, SbN, N);
    assertTrue(contains(inducedCategories, SbN.forward(N)));
    Right_Arg(inducedCategories, SbN, S);
    assertTrue(contains(inducedCategories, SbN.forward(S)));

    // Allow (N\N)/N
    NbN.modifier = true;
    Right_Arg(inducedCategories, NbN, N);
    assertTrue(contains(inducedCategories, NbN.forward(N)));
    InducedCAT SbS = S.backward(S);
    SbS.modifier = true;
    //Right_Arg(inducedCategories, SbS, S);
    //assertFalse(contains(inducedCategories, SbS.forward(S))); // Too many sentential arguments
    Right_Arg(inducedCategories, SbS, N);
    assertTrue(contains(inducedCategories, SbS.forward(N)));
  }

  public void testLeft_Arg() throws Exception {
    ConcurrentHashMap<InducedCAT, Boolean> inducedCategories = new ConcurrentHashMap<>(5);

    // Don't allow (S/N)\N or (S/N)\S
    Left_Arg(inducedCategories, N, SfN);
    assertFalse(contains(inducedCategories, SfN.backward(N)));
    Left_Arg(inducedCategories, S, SfN);
    assertFalse(contains(inducedCategories, SfN.backward(S)));


    // Allow (N/N)\N  if Config set
    Configuration.allowXbXbX = false;
    NfN.modifier = true;
    Left_Arg(inducedCategories, N, NfN);
    assertFalse(contains(inducedCategories, NfN.backward(N)));
    InducedCAT SfS = S.forward(S);
    SfS.modifier = true;
    Left_Arg(inducedCategories, S, SfS);
    assertFalse(contains(inducedCategories, SfS.backward(S)));

    Configuration.allowXbXbX = true;
    Left_Arg(inducedCategories, N, NfN);
    assertTrue(contains(inducedCategories, NfN.backward(N)));
    //Left_Arg(inducedCategories, S, SfS);
    //assertFalse(contains(inducedCategories, SfS.backward(S))); // Too many sentential arguments
    Left_Arg(inducedCategories, N, SfS);
    assertTrue(contains(inducedCategories, SfS.backward(N)));
  }

  static boolean contains(ConcurrentHashMap<InducedCAT, Boolean> inducedCategories,
                          InducedCAT query) {
    Boolean searched = inducedCategories.remove(query);
    if (searched == null || !inducedCategories.isEmpty()) {
      inducedCategories.clear();
      return false;
    }
    return true;
  }
}