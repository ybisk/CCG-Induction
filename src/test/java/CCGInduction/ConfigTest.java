package CCGInduction;

import junit.framework.TestCase;

public class ConfigTest extends TestCase {

  private String configurationFile;

  protected void setUp() {
    configurationFile = "config/sample-config.properties";
  }

  public void testConfigRead() {
    new Configuration(configurationFile);
    assertTrue(Configuration.testFile[0].equals("src/main/resources/english.example"));
  }
}
