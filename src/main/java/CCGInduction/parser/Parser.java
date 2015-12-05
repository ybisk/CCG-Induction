package CCGInduction.parser;

import CCGInduction.grammar.Grammar;
import CCGInduction.models.Model;
import CCGInduction.utils.Mapper;

import java.util.ArrayList;

/**
 * Simple class that performs vanilla CYK on a chart (editing it in place)
 * @author bisk1
 * @param <G> 
 * @param <C> 
 */
public class Parser<G extends Grammar, C extends Chart<G>> extends Mapper<G,C> {
  
  private final ParserInterface<G> parser;

  /**
   * Instantiates simple class for simply calling Parse on charts
   * @param shared_charts Chart to parse
   * @param parser_type Parser to use
   * @param exceptions store caught exceptions
   */
  protected Parser(Model<G> shared_model, Charts<G, C> shared_charts, ParserInterface<G> parser_type,
                   ArrayList<Exception> exceptions) {
    super(shared_model, shared_charts, exceptions);
    this.parser = parser_type;
  }

  @Override
  protected void map(C chart) throws Exception {
    this.parser.parse(localModel, chart);
  }

  public static class FailedParsingAssertion extends AssertionError {
    private static final long serialVersionUID = 1048683132894877411L;

    public FailedParsingAssertion(String msg) {
      super(msg);
    }
  }
}