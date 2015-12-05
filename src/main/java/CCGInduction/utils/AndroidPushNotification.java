package CCGInduction.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Stolen from
 * https://github.com/christos-c/bmmm
 */
public class AndroidPushNotification {

  /**
   * An API to send notifications to android devices via the Notify My Android app.
   * Code adapted from from Adriano Maia (adriano@usk.bz)
   */
    private static String API_KEY;
    private static final String DEFAULT_URL = "https://nma.usk.bz";
    private static final String APP_NAME = "HDPCCG";
    private static final String EVENT_NAME = "Experiment Complete";

    public AndroidPushNotification(String apiKeyFile) {
      try {
        BufferedReader in = TextFile.Reader(apiKeyFile);
        API_KEY = in.readLine();
      } catch (IOException e) {
        System.err.println("Cannot read API key file");
        API_KEY = null;
      }
    }

    public void notify(String message) {
      if (API_KEY == null) {
        System.err.println("API key not set");
        return;
      }
      try {
        // Verify that the API key is correct
        sendRequest(null);
        // Sending a notification
        System.out.println(sendRequest(message));
      } catch (IOException | SAXException | ParserConfigurationException e) {
        e.printStackTrace();
      }
    }

    /**
     * Sends a notification using NMA public API.
     *
     * @param description Long description or message body (Up to 10000 characters)
     * @return result
     */
    private static String sendRequest(String description) throws IOException,
        ParserConfigurationException, SAXException {
      URL url;
      if (description == null)
        url = new URL(DEFAULT_URL + "/publicapi/verify");
      else
        url = new URL(DEFAULT_URL + "/publicapi/notify");

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      connection.setUseCaches(false);
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

      // Setup the POST data
      StringBuilder data = new StringBuilder();
      addEncodedParameter(data, "apikey", API_KEY);
      if (description != null) {
        addEncodedParameter(data, "application", APP_NAME);
        addEncodedParameter(data, "event", EVENT_NAME);
        addEncodedParameter(data, "description", description);
        addEncodedParameter(data, "priority", Integer.toString(0));
      }

      // Buffers and Writers to send the data
      OutputStreamWriter writer;
      writer = new OutputStreamWriter(connection.getOutputStream());

      writer.write(data.toString());
      writer.flush();
      writer.close();
      BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

      if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
          response.append(line);
        }

        boolean msgSent = false;

        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource inStream = new InputSource();
        inStream.setCharacterStream(new StringReader(response.toString()));
        Document doc = db.parse(inStream);

        Element root = doc.getDocumentElement();

        if(root.getTagName().equals("nma")) {
          Node item = root.getFirstChild();
          String childName = item.getNodeName();
          if (childName.equals("success"))
            msgSent = true;
        }

        return (msgSent) ? "Message sent successfully" : "Message failed to send";
      }
      else {
        return "There was a problem contacting NMA Servers. " +
            "HTTP Response code different than 200(OK). " +
            "Try again or contact support@nma.bz if it persists.";
      }
    }

    /**
     * Dynamically adds a url-form-encoded key/value to a StringBuilder
     * @param sb StringBuilder buffer used to build the final url-form-encoded data
     * @param name Key name
     * @param value Value
     * @throws java.io.IOException
     */
    private static void addEncodedParameter(StringBuilder sb, String name, String value) throws IOException {
      if (sb.length() > 0) {
        sb.append("&");
      }
      sb.append(URLEncoder.encode(name, "UTF-8"));
      sb.append("=");
      if(value==null)
        throw new IOException("ERROR: " + name + " is null");
      else
        sb.append(URLEncoder.encode(value, "UTF-8"));
    }
}
