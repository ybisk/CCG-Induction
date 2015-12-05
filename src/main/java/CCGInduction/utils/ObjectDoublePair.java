package CCGInduction.utils;

/**
 * @author bisk1
 * Class for comparing objects which have a score/value which nees to be sorted
 * @param <T>   Type of the object/contents
 */
public class ObjectDoublePair<T> implements Comparable<Object> {
  private final T contents;
  private final double value;
  
  /**
   * Create an immutable object which sorts by the double's value
   * @param object Non-sortable object
   * @param value Value to sort by
   */
  public ObjectDoublePair(T object, double value) {
    contents = object;
    this.value = value;
  }
  
  /**
   * Returns the sorted content
   * @return  object
   */
  public T content() {
    return contents;
  }
  
  /**
   * Value of the object
   * @return value
   */
  public double value() {
    return value;
  }
  
  @SuppressWarnings("unchecked")
  public int compareTo(Object arg0) {
    return (int) Math.signum(((ObjectDoublePair<T>) arg0).value - value);
  }
}
