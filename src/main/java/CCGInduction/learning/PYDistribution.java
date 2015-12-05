package CCGInduction.learning;

import CCGInduction.Configuration;
import CCGInduction.utils.Math.Log;
import CCGInduction.models.Model;
import CCGInduction.utils.Logger;
import CCGInduction.utils.Math.LogDouble;
import CCGInduction.utils.Math.Sample;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distribution object used for variational inference.  Distribution can have
 * a uniform base distribution or can inherit from another PYDistribution
 * @author bisk1
 *
 */
public class PYDistribution extends Distribution {
  private static final long serialVersionUID = 1232014;
  /** Specify alpha Regimen for DP ( {0,1,2,3} ) with PY set to 0 */
  private double alphaPower = -1;
  /** Specify discount for PY ( 0 < d < 1 ) with DP set to 0 */
  private double discount = Log.ZERO;
  private boolean truncate = false;

  /** Base distribution we inherit from and smooth too */
  public PYDistribution BaseDistribution = null;
  /** Number of outcomes in the base distribution for a given conditioning
   * variable
   */
  private final ConcurrentHashMap<CondOutcomePair,Integer> support = new ConcurrentHashMap<>();

  /**
   * Constructor for stick weights which requires additional concentration
   * parameters when used with draws
   * 
   * @param mod Model
   * @param id Name
   * @param a Alpha regimen
   * @param d Discount (logspace)
   * @param trunc Truncate
   */
  public PYDistribution(Model<?> mod, String id, double a, double d, boolean trunc) {
    super(mod, id);
    alphaPower = a;
    discount = d;
    truncate = trunc;
  }

  /**
   * MLE distribution
   * @param modelRef Model
   * @param id Human readable name of distribution
   */
  public PYDistribution(Model<?> modelRef, String id){
    super(modelRef,id);
  }

  PYDistribution(PYDistribution other, PYDistribution base) {
    super(other);
    alphaPower       = other.alphaPower;
    discount         = other.discount;
    truncate         = other.truncate;
    BaseDistribution = base;
    support.putAll(other.support);
  }

  public PYDistribution(PYDistribution other) {
    this(other, other.BaseDistribution == null ? null : other.BaseDistribution.copy());
  }

  public PYDistribution copy() {
    return new PYDistribution(this);
  }
  public PYDistribution copy(PYDistribution base) {
    return new PYDistribution(this, base);
  }

  /**
   * Initialize distribution via base measure
   * @param beta base measure
   */
  public void Init(PYDistribution beta) {
    initialized = true;
    BaseDistribution = beta;

    HashMap<CondOutcomePair,LogDouble> oldCounts = new HashMap<>();
    oldCounts.putAll(CountsNew);
    CountsNew.clear();
    Counts.clear();

    for (CondOutcomePair cond : conditioning_contexts.keySet()) {
      CondOutcomePair conditioningVariable = this.model.backoff(cond, this);

      Outcomes base = new Outcomes(conditioning_contexts.get(cond).size());
      support.put(cond, beta.support.get(this.model.backoff(cond, this)));
      for (CondOutcomePair pair : conditioning_contexts.get(cond).keySet()) {
        if (oldCounts.containsKey(pair)) {
          oldCounts.get(pair).add(Configuration.smallRule);
          Counts.put(pair, new LogDouble(oldCounts.get(pair).value()));
        } else {
          Counts.put(pair, new LogDouble(Configuration.smallRule));
        }
        conditioning_contexts.get(cond).put(pair, true);
        CountsNew.put(pair, new LogDouble(Log.ZERO));

        base.add(pair, BaseDistribution.P(conditioningVariable, pair.outcome));
      }
      if(truncate) {
        base.normalize();
      }
      for(int i = 0; i < base.length(); ++i) {
        Probabilities.put(base.pairs[i], base.vals[i]);
      }
      CountsNewEdited.put(cond,false);
    }
    CountsNewGlobalEdited = false;
    Logger.logOnly(String.format("%-20s %-30s %6d x n array" + " with base %-20s\n",
        this.identifier, "variationally initialized as", conditioning_contexts.size(), beta.identifier));

  }

  /* Variational without the base-measure (HMM) */
  public void updateVariationalNoBase() {
    for (CondOutcomePair cond : conditioning_contexts.keySet()) {
      Outcomes full = null;
      if (CountsNewEdited.get(cond)) {
        full = new Outcomes(conditioning_contexts.get(cond).size());
        for (CondOutcomePair pair : conditioning_contexts.get(cond).keySet()) {
          Counts.put(pair, CountsNew.get(pair));
          CountsNew.put(pair, new LogDouble(Log.ZERO));
          full.add(pair, Counts.get(pair).value());
        }
        CountsNewEdited.put(cond, false);
      }
      if (full != null) {
        // alpha(X)
        // Or treat hyper-parameter as a constant?
        double alpha = Math.log(alphaPower);

        // C_* + alpha(X)
        double divisor = Sample.digamma(Log.sloppy_add(full.sum(), Log.mul(Math.log(full.length()),alpha)));
        double counts;
        for (int var = 0; var < full.pairs.length; var++) {
          counts = full.vals[var];
          // TEMPORARY
          counts = Math.max(counts, Configuration.smallRule);

          // C_z - d + beta_z(alpha(X) + kd)
          counts = Log.sloppy_add(counts, alpha);
          // W_z = digamma(exp( )) - digamma(exp( ))
          full.vals[var] = Log.div(Sample.digamma(counts), divisor);
        }
        // make sure no weight < min_weight
        // FIXME:  Why isn't this fixable with 1
        double v;
        for (int var = 0; var < full.pairs.length; ++var) {
          v = full.vals[var];
          if (Log.One(v) || (full.pairs.length == 1 && v > 0 && v < 1E-10)) {
            v = Log.ONE;
          }
          Probabilities.put(full.pairs[var], v);

          if (v > Log.ONE) {
            System.err.println(model.prettyCond(full.pairs[var], this) + "\t"
                + model.prettyOutcome(full.pairs[var].outcome, this));
            throw new Log.MathException("Invalid Probability: " + "\t" + v + "\t" + Log.equal(v, Log.ONE));
          }
          if (Probabilities.get(full.pairs[var]) > Log.ONE) {
            throw new Log.MathException("Greater than 1: " + Math.exp(Probabilities.get(full.pairs[var])));
          }
        }
      }
      CountsNewGlobalEdited = false;
    }
  }

  /**
   * Dirichlet Process Multinomial weights W_z^Y (y) = exp[digamma[C(z -> y) +
   * alpha^Y beta_z]] / exp[digamma[C(z->*) + alpha^Y]] Pitman-Yor Process
   * Multinomial weights W_z^Y (y) = exp[digamma[C(z -> y) - d + beta_z (alpha^Y
   * + k*d)]] / exp[digamma[C(z -> * ) + alpha^Y]] This reduces to the DP when d
   * = 0
   */
  public void updateVariational() {
    for (CondOutcomePair cond : conditioning_contexts.keySet()) {
      CondOutcomePair conditioningVariable = this.model.backoff(cond, this);

      Outcomes full = null;
      Outcomes beta = null;
      if (CountsNewEdited.get(cond)) {
        full = new Outcomes(conditioning_contexts.get(cond).size());
        beta = new Outcomes(conditioning_contexts.get(cond).size());
        for (CondOutcomePair pair : conditioning_contexts.get(cond).keySet()) {
          Counts.put(pair, CountsNew.get(pair));
          CountsNew.put(pair, new LogDouble(Log.ZERO));
          full.add(pair, Counts.get(pair).value());
          beta.add(pair, BaseDistribution.P(conditioningVariable, pair.outcome));
        }
        if(truncate) {
          beta.normalize();
        }
        CountsNewEdited.put(cond,false);
      }
      if (full != null) {
        // If C_i < d, C_i = d
        for (int i = 0; i < full.length(); i++) {
          if (full.vals[i] < discount) {
            full.vals[i] = Log.add(discount, Configuration.smallRule);
          }
        }

        // alpha(X)
        double alpha;
        if (Configuration.ALPHA_SCHEME) {
          if(truncate) {
            alpha = Math.log(Math.pow(full.length(),alphaPower));
          } else {
            alpha = Math.log(Math.pow(support.get(cond),alphaPower));
          }
        } else {
          // Or treat hyper-parameter as a constant?
          alpha = Math.log(alphaPower);
        }

        // k*d
        double kd = Math.log(full.pairs.length * Math.exp(discount));
        // alpha(X) + k*d
        double baseMeasure = Log.add(alpha, kd);
        // C_* + alpha(X)
        double divisor = Sample.digamma(Log.sloppy_add(full.sum(), alpha));
        double beta_z, counts;
        for (int var = 0; var < full.pairs.length; var++) {
          beta_z = beta.vals[var]; // beta_z
          if (discount < full.vals[var]) {
            counts = Log.subtract(full.vals[var], discount); // C_z - d
          } else {
            counts = Configuration.smallRule;
          }
          // TEMPORARY
          counts = Math.max(counts, Configuration.smallRule);

          // C_z - d + beta_z(alpha(X) + kd)
          counts = Log.sloppy_add(counts, Log.mul(beta_z, baseMeasure));
          // W_z = digamma(exp( )) - digamma(exp( ))
          full.vals[var] = Log.div(Sample.digamma(counts), divisor);
        }
        // make sure no weight < min_weight
        // FIXME:  Why isn't this fixable with 1
        if(truncate) {
          normalize(full);    // FIXME:  Remove
        }
        double v;
        for (int var = 0; var < full.pairs.length; ++var) {
          v = full.vals[var];
          if (Log.One(v) || (full.pairs.length == 1 && v > 0 && v < 1E-10)) {
            v = Log.ONE;
          }
          Probabilities.put(full.pairs[var], v);

          if (v > Log.ONE) {
            System.err.println(model.prettyCond(full.pairs[var], this) + "\t"
                + model.prettyOutcome(full.pairs[var].outcome, this));
            throw new Log.MathException("Invalid Probability: " + "\t" + v + "\t" + Log.equal(v, Log.ONE));
          }
          if (Probabilities.get(full.pairs[var]) > Log.ONE) {
            throw new Log.MathException("Greater than 1: " + Math.exp(Probabilities.get(full.pairs[var])));
          }
        }
      }
      CountsNewGlobalEdited = false;
    }
  }

  /**
   * Performance normal Distributional update
   */
  public void updateMLE() {
    super.update();
  }

  /**
   * Perform +1 smoothing on stick distributions
   * 
   */
  public void initSticks() {
    initialized = true;
    // TODO: Should be doing:
    // divide (alpha + oldK*d)/(newK - oldK) and add to new sticks
    // oldCounts - d

    // Check if this is a first run or step in the curriculum
    HashMap<CondOutcomePair,LogDouble> oldCounts = new HashMap<>();
    HashMap<CondOutcomePair,LogDouble> oldCounts2 = new HashMap<>();
    double smooth;
    if (CountsNewGlobalEdited) {
      oldCounts.putAll(CountsNew); // Where prior counts are accumulated
      oldCounts2.putAll(Counts);
      smooth = Log.ZERO;
    } else {
      oldCounts.putAll(Counts); // Populated during update
      smooth = Log.ONE;
    }

    Probabilities.clear();
    Counts.clear();
    CountsNew.clear();
    for (CondOutcomePair context : conditioning_contexts.keySet()) {
      Outcomes full = new Outcomes(conditioning_contexts.get(context).size());
      support.put(context,full.length());
      for (CondOutcomePair pair : conditioning_contexts.get(context).keySet()) {
        // for (int j : usedContexts.get(i)) {
        // Full
        if (oldCounts2.containsKey(pair)
            && oldCounts.containsKey(pair)) {
          Counts.put(pair, new LogDouble(Log.add(Log.add(oldCounts.get(pair).value(), oldCounts2.get(pair).value()), smooth)));
        } else if (oldCounts.containsKey(pair)) {
          Counts.put(pair, new LogDouble(Log.add(oldCounts.get(pair).value(), smooth)));
        } else {
          Counts.put(pair, new LogDouble(smooth));
        }
        full.add(pair, Counts.get(pair).value());

        CountsNew.put(pair, new LogDouble(Log.ZERO));
        if (CountsNew.get(pair).value() > Log.ONE) {
          throw new Log.MathException("Initial counts value > 1");
        }
        Probabilities.put(pair, Log.ZERO);

      }
      normalize(full);
      for (int o = 0; o < full.pairs.length; o++) {
        Probabilities.put(full.pairs[o], full.vals[o]);
      }
      CountsNewEdited.put(context,false);
    }
    CountsNewGlobalEdited = false;
    Logger.logOnly(String.format("%-20s %-30s %6d x n array\n",
        this.identifier, "sticks initialized as", conditioning_contexts.size()));
  }

  @Override
  public Double P(CondOutcomePair cond_outcome) {
    Double val;
    if ((val = super.P(cond_outcome)) != null) {
      return val;
    }
    // TODO:  SANITY CHECK
    if(BaseDistribution != null) {
      CondOutcomePair cond = new CondOutcomePair(cond_outcome.conditioning_variables);

      // Unseen conditioning variable
      if (!conditioning_contexts.containsKey(cond)) {
        return BaseDistribution.P(new CondOutcomePair(cond_outcome.outcome,
            model.backoff(cond_outcome, this)));
      }
      double total = Log.ZERO;
      for(CondOutcomePair pair : conditioning_contexts.get(cond).keySet()){
        total = Log.add(total, Counts.get(pair).value());
      }
      double alpha;
      if (Configuration.ALPHA_SCHEME) {
        // Should we exponentiate?
        if (truncate) {
          alpha = Math.log(Math.pow(conditioning_contexts.get(cond).keySet().size(), alphaPower));
        } else {
          alpha = Math.log(Math.pow(support.get(cond), alphaPower));
        }
      } else {
        // Or treat hyper-parameter as a constant?
        alpha = Math.log(alphaPower);
      }

      total = Sample.digamma(Log.add(total, alpha));
      double count =  Sample.digamma(Log.add(Configuration.smallRule, Log.mul(alpha,
          BaseDistribution.P(new CondOutcomePair(cond_outcome.outcome,
              model.backoff(cond_outcome, this))))));
      return Log.div(count,total);
    }
    // New base-measure outcome.... small probability
    return Configuration.smallRule;
  }

  /**
   * If there are new outcomes for a given context, provide them the probability
   * of an UNK take and then let them henceforth shrink/grow naturally
   * IMPORTANT: MUST CALL ON BASE MEASURES BEFORE INHERITING DISTRIBUTIONS
   */
  public void newSticks() {
    for (CondOutcomePair cond : conditioning_contexts.keySet()) {
      Outcomes full = new Outcomes(conditioning_contexts.get(cond).size());
      Outcomes beta = new Outcomes(conditioning_contexts.get(cond).size());

      double old_sum = Log.ZERO;
      if(BaseDistribution != null) {
        CondOutcomePair conditioningVariable = this.model.backoff(cond, this);
        for (CondOutcomePair pair : conditioning_contexts.get(cond).keySet()) {
          if(Counts.containsKey(pair) && Counts.get(pair).value() != Log.ZERO) {
            // Get your existing counts
            full.add(pair, Counts.get(pair).value());
            old_sum = Log.add(old_sum,Counts.get(pair).value());
          } else {
            // Give you smoothing mass
            full.add(pair,Configuration.smallRule);
          }
          beta.add(pair, BaseDistribution.P(conditioningVariable, pair.outcome));
        }
        if(truncate) {
          beta.normalize();
        }
        // If this is completely new    TODO: Verify correctness
        if(!support.containsKey(cond)) {
          support.put(cond,BaseDistribution.support.get(conditioningVariable));
        }
        // Else, rely on base measure
      } else {
        // We are the base measure.  Our "base measure" is uniform
        for (CondOutcomePair pair : conditioning_contexts.get(cond).keySet()) {
          if(Counts.containsKey(pair) && Counts.get(pair).value() != Log.ZERO) {
            full.add(pair, Counts.get(pair).value());
            old_sum = Log.add(old_sum,Counts.get(pair).value());
          } else {
            full.add(pair,Configuration.smallRule);
          }
          beta.add(pair, Configuration.smallRule);  // Uniform over fictitious 1/V
        }
        support.put(cond, full.length());
        //beta.normalize();
      }

      // Add smoothing mass for aggregate new
      old_sum = Log.add(old_sum,Configuration.smallRule);
      // alpha(X)
      double alpha = Math.log(alphaPower);
      // k*d      -- Pitman-Yor is not based on size of old distribution
      double kd = Math.log(full.pairs.length * Math.exp(discount));
      // alpha(X) + k*d
      double baseMeasure = Log.add(alpha, kd);
      // C_* + alpha(X)
      double divisor = Sample.digamma(Log.sloppy_add(old_sum, alpha));
      double beta_z;
      // For each new guy, give it a count and multinomial weight
      double counts;
      for(int i = 0; i < beta.vals.length; ++i){
        //if(full.vals[i] == Configuration.smallRule) {
        beta_z = beta.vals[i];
        // C_z - d + beta_z(alpha(X) + kd)
        counts = Log.sloppy_add(full.vals[i], Log.mul(beta_z, baseMeasure));
        // W_z = digamma(exp( )) - digamma(exp( ))
        CountsNew.put(full.pairs[i], new LogDouble(Log.ZERO));
        Counts.put(full.pairs[i], new LogDouble(counts));
        Probabilities.put(full.pairs[i], Math.max(Log.div(Sample.digamma(counts), divisor), Configuration.smallRule));
        //}
      }
    }
  }

  /**
   * Set's DP/PY parameters: alpha = T^exp and discount = d DP: d = 0 PY: exp =
   * 0
   * @param exp DP regimen
   * @param d PY discount (logspace)
   * @param trunc Should we truncate?
   */
  public void processParameters(double exp, double d, boolean trunc) {
    alphaPower = exp;
    discount = d;
    truncate = trunc;
  }

  /**
   * D(P||Q) = Sum_i ln(P(i)/Q(i)) P(i)
   * Computes KL(base || learned)
   * @param conditioningVariables Conditioning Variables
   * @return Divergence
   */
  public double KLDivergence(CondOutcomePair conditioningVariables) {
    if (BaseDistribution == null) {
      return -1;
    }
    CondOutcomePair base_cond = model.backoff(conditioningVariables, this);
    double KL = 0;
    double[] b = new double[conditioning_contexts.get(conditioningVariables).keySet().size()];
    double[] l = new double[b.length];
    int index = 0;
    double b_sum = 0;
    for (CondOutcomePair pair : conditioning_contexts.get(conditioningVariables).keySet()) {
      b[index] = Math.exp(BaseDistribution.P(base_cond, pair.outcome));
      b_sum += b[index];
      l[index] = Math.exp(Probabilities.get(pair));
      ++index;
    }
    double p,q;
    for (index = 0; index < b.length; ++index) {
      p = b[index]/b_sum;
      q = l[index];
      KL += Math.log(p/q)*p;
    }
    return KL;
  }

  public void merge(PYDistribution local) {
    super.merge(local);
    // Do not merge Support.  It is defined in init
  }
}
