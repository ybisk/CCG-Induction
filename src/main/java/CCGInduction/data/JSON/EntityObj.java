package CCGInduction.data.JSON;

/**
 * Entities are indices, scores provided by an entity resolver and freebase IDs
 */
public class EntityObj {
  /* Index of word marked as entity */
  public final int index;
  /* Entity resolver's score */
  final double score;
  /* The freebase entity ID */
  final String entity;

  EntityObj(int entityIndex, String FBID, double resolverScore) {
    entity = FBID;
    index = entityIndex;
    score = resolverScore;
  }

  @Override
  public String toString() {
    return JSONFormat.gsonBuilder.disableHtmlEscaping().create().toJson(this);
  }

  @Override
  public boolean equals(Object o) {
    if (!EntityObj.class.isInstance(o))
      return false;
    EntityObj other = (EntityObj)o;
    return (index == other.index)
        && (score == other.score)
        && (entity == null ? other.entity == null : entity.equals(other.entity));
  }
}
