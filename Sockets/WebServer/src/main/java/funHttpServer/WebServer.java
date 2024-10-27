/*
Simple Web Server in Java which allows you to call 
localhost:9000/ and show you the root.html webpage from the www/root.html folder
You can also do some other simple GET requests:
1) /random shows you a random picture (well random from the set defined)
2) json shows you the response as JSON for /random instead the html page
3) /file/filename shows you the raw file (not as HTML)
4) /multiply?num1=3&num2=4 multiplies the two inputs and responses with the result
5) /github?query=users/amehlhase316/repos (or other GitHub repo owners) will lead to receiving
   JSON which will for now only be printed in the console. See the todo below

The reading of the request is done "manually", meaning no library that helps making things a 
little easier is used. This is done so you see exactly how to pars the request and 
write a response back
*/

package funHttpServer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.charset.Charset;
import org.json.*;


class WebServer {
  public static void main(String args[]) {
    WebServer server = new WebServer(9000);
  }

  /**
   * Main thread
   * @param port to listen on
   */
  public WebServer(int port) {
    ServerSocket server = null;
    Socket sock = null;
    InputStream in = null;
    OutputStream out = null;

    try {
      server = new ServerSocket(port);
      while (true) {
        sock = server.accept();
        out = sock.getOutputStream();
        in = sock.getInputStream();
        byte[] response = createResponse(in);
        out.write(response);
        out.flush();
        in.close();
        out.close();
        sock.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (sock != null) {
        try {
          server.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Used in the "/random" endpoint
   */
  private final static HashMap<String, String> _images = new HashMap<>() {
    {
      put("streets", "https://iili.io/JV1pSV.jpg");
      put("bread", "https://iili.io/Jj9MWG.jpg");
    }
  };

  private Random random = new Random();

  /**
   * Reads in socket stream and generates a response
   * @param inStream HTTP input stream from socket
   * @return the byte encoded HTTP response
   */
  public byte[] createResponse(InputStream inStream) {

    byte[] response = null;
    BufferedReader in = null;

    try {

      // Read from socket's input stream. Must use an
      // InputStreamReader to bridge from streams to a reader
      in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));

      // Get header and save the request from the GET line:
      // example GET format: GET /index.html HTTP/1.1

      String request = null;

      boolean done = false;
      while (!done) {
        String line = in.readLine();

        System.out.println("Received: " + line);

        // find end of header("\n\n")
        if (line == null || line.equals(""))
          done = true;
        // parse GET format ("GET <path> HTTP/1.1")
        else if (line.startsWith("GET")) {
          int firstSpace = line.indexOf(" ");
          int secondSpace = line.indexOf(" ", firstSpace + 1);

          // extract the request, basically everything after the GET up to HTTP/1.1
          request = line.substring(firstSpace + 2, secondSpace);
        }

      }
      System.out.println("FINISHED PARSING HEADER\n");

      // Generate an appropriate response to the user
      if (request == null) {
        response = "<html>Illegal request: no GET</html>".getBytes();
      } else {
        // create output buffer
        StringBuilder builder = new StringBuilder();
        // NOTE: output from buffer is at the end

        if (request.length() == 0) {
          // shows the default directory page

          // opens the root.html file
          String page = new String(readFileInBytes(new File("www/root.html")));
          // performs a template replacement in the page
          page = page.replace("${links}", buildFileList());

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(page);

        } else if (request.equalsIgnoreCase("json")) {
          // shows the JSON of a random image and sets the header name for that image

          // pick a index from the map
          int index = random.nextInt(_images.size());

          // pull out the information
          String header = (String) _images.keySet().toArray()[index];
          String url = _images.get(header);

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: application/json; charset=utf-8\n");
          builder.append("\n");
          builder.append("{");
          builder.append("\"header\":\"").append(header).append("\",");
          builder.append("\"image\":\"").append(url).append("\"");
          builder.append("}");

        } else if (request.equalsIgnoreCase("random")) {
          // opens the random image page

          // open the index.html
          File file = new File("www/index.html");

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(new String(readFileInBytes(file)));

        } else if (request.contains("file/")) {
          // tries to find the specified file and shows it or shows an error

          // take the path and clean it. try to open the file
          File file = new File(request.replace("file/", ""));

          // Generate response
          if (file.exists()) { // success
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Would theoretically be a file but removed this part, you do not have to do anything with it for the assignment");
          } else { // failure
            builder.append("HTTP/1.1 404 Not Found\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("File not found: " + file);
          }
        } else if (request.contains("multiply?")) {
          // This multiplies two numbers, there is NO error handling, so when
          // wrong data is given this just crashes

          try {
            Map<String, String> query_pairs = splitQuery(request.replace("multiply?", ""));

            // Check if both num1 and num2 parameters exist
            if (query_pairs.containsKey("num1") && query_pairs.containsKey("num2")) {
              try {
                // Parse num1 and num2 values
                Integer num1 = Integer.parseInt(query_pairs.get("num1"));
                Integer num2 = Integer.parseInt(query_pairs.get("num2"));

                // Perform multiplication
                Integer result = num1 * num2;

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append("Result is: " + result);

          // TODO: Include error handling here with a correct error code and
          // a response that makes sense
        } catch (NumberFormatException e) {
          // Handle invalid integer input
          builder.append("HTTP/1.1 400 Bad Request\n");
          builder.append("Content-Type: text/plain; charset=utf-8\n");
          builder.append("\n");
          builder.append("Error: Invalid input. Please provide valid integers for num1 and num2.");
        }
      } else {
        // Handle missing parameters
        builder.append("HTTP/1.1 400 Bad Request\n");
        builder.append("Content-Type: text/plain; charset=utf-8\n");
        builder.append("\n");
        builder.append("Error: Both num1 and num2 parameters are required.");
      }
    } catch (Exception e) {
      // Handle any other exception that may occur
      builder.append("HTTP/1.1 400 Bad Request\n");
      builder.append("Content-Type: text/plain; charset=utf-8\n");
      builder.append("\n");
      builder.append("Error: Invalid query format. Please provide valid parameters for multiplication.\n");
      builder.append("Example query: /multiply?num1=<num1>&num2=<num2>");
    }
  }

         else if (request.contains("github?")) {

          try {
            Map<String, String> query_pairs = splitQuery(request.replace("github?", ""));
            String jsonResponse = fetchURL("https://api.github.com/" + query_pairs.get("query"));

            // Check if JSON response is empty or null
            if (jsonResponse == null || jsonResponse.isEmpty()) {
              // Respond with appropriate error message and status code
              builder.append("HTTP/1.1 404 Not Found\n");
              builder.append("Content-Type: text/plain; charset=utf-8\n");
              builder.append("\n");
              builder.append("Error: No data found for the given query.");
            } else {
              try {
                // Parse the JSON response
                JSONArray jsonArray = new JSONArray(jsonResponse);

                // Build response
                builder.append("HTTP/1.1 200 OK\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("\n");
                builder.append("<html><head><title>GitHub Repositories</title></head><body><ul>");

                // Iterate through each repository in the JSON array
                for (int i = 0; i < jsonArray.length(); i++) {
                  JSONObject repo = jsonArray.getJSONObject(i);
                  String fullName = repo.getString("full_name");
                  int id = repo.getInt("id");
                  JSONObject owner = repo.getJSONObject("owner");
                  String ownerLogin = owner.getString("login");

                  // Append repository information to the response
                  builder.append("<li>Full Name: ").append(fullName).append("</li>");
                  builder.append("<li>ID: ").append(String.valueOf(id)).append("</li>");
                  builder.append("<li>Owner's Login: ").append(ownerLogin).append("</li><br>");
                }

                builder.append("</ul></body></html>");
              } catch (JSONException e) {
                // Handle JSON parsing error
                builder.append("HTTP/1.1 500 Internal Server Error\n");
                builder.append("Content-Type: text/plain; charset=utf-8\n");
                builder.append("\n");
                builder.append("Error: Failed to parse GitHub API response.");
              }
            }
          } catch (Exception e) {
            // Handle any other exception that may occur
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/plain; charset=utf-8\n");
            builder.append("\n");
            builder.append("Error: Invalid query format. Please provide a valid query parameter for GitHub API.");
          }
        }

        else if (request.contains("temperature?")) {
          // Temperature conversion request

          try {
            Map<String, String> query_pairs = splitQuery(request.replace("temperature?", ""));

            // Check if all required parameters exist
            if (query_pairs.containsKey("value") && query_pairs.containsKey("from") && query_pairs.containsKey("to")) {
              String valueString = query_pairs.get("value");
              String fromUnit = query_pairs.get("from").toUpperCase();
              String toUnit = query_pairs.get("to").toUpperCase();

              // Check if value is a valid number
              double value;
              try {
                value = Double.parseDouble(valueString);
              } catch (NumberFormatException e) {
                // Handle invalid value
                builder.append("HTTP/1.1 400 Bad Request\n");
                builder.append("Content-Type: text/plain; charset=utf-8\n");
                builder.append("\n");
                builder.append("Error: Invalid input value. Please provide a valid number for temperature.");
                response = builder.toString().getBytes();
                return response;
              }

              // Check if from and to units are valid
              if (!(Arrays.asList("C", "K", "F").contains(fromUnit) && Arrays.asList("C", "K", "F").contains(toUnit))) {
                // Handle invalid units
                builder.append("HTTP/1.1 400 Bad Request\n");
                builder.append("Content-Type: text/plain; charset=utf-8\n");
                builder.append("\n");
                builder.append("Error: Invalid units. Please use 'C', 'F', or 'K' for temperature units.");
                response = builder.toString().getBytes();
                return response;
              }

              double result = value;

              // Convert temperature
              if (fromUnit.equals("C")) {
                // Conversion logic for Celsius
                if (toUnit.equals("F")) {
                  result = (value * 9 / 5) + 32;
                } else if (toUnit.equals("K")) {
                  result = value + 273.15;
                }
              } else if (fromUnit.equals("F")) {
                // Conversion logic for Fahrenheit
                if (toUnit.equals("C")) {
                  result = (value - 32) * 5 / 9;
                } else if (toUnit.equals("K")) {
                  result = (value + 459.67) * 5 / 9;
                }
              } else if (fromUnit.equals("K")) {
                // Conversion logic for Kelvin
                if (toUnit.equals("C")) {
                  result = value - 273.15;
                } else if (toUnit.equals("F")) {
                  result = (value * 9 / 5) - 459.67;
                }
              }

              // Generate response
              builder.append("HTTP/1.1 200 OK\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Result is: ").append(result).append(" ").append(toUnit);
            } else {
              // Handle missing parameters
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/plain; charset=utf-8\n");
              builder.append("\n");
              builder.append("Error: All parameters (value, from, to) are required for temperature conversion.");
            }
          } catch (Exception e) {
            // Handle any other exception that may occur
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/plain; charset=utf-8\n");
            builder.append("\n");
            builder.append("Error: Invalid query format. Please use the following format for temperature conversion: /temperature?value=<value>&from=<unit>&to=<unit>");
          }
        }
        else if (request.contains("roll?")) {
          // Dice roll request

          try {
            Map<String, String> query_pairs = splitQuery(request.replace("roll?", ""));

            // Check if both parameters exist
            if (query_pairs.containsKey("sides") && query_pairs.containsKey("numDice")) {
              try {
                // Parse parameters
                int sides = Integer.parseInt(query_pairs.get("sides"));
                int numDice = Integer.parseInt(query_pairs.get("numDice"));

                // Check if sides and numDice are positive integers
                if (sides <= 0 || numDice <= 0) {
                  // Handle invalid input
                  builder.append("HTTP/1.1 400 Bad Request\n");
                  builder.append("Content-Type: text/plain; charset=utf-8\n");
                  builder.append("\n");
                  builder.append("Error: Sides and numDice must be positive integers.");
                  response = builder.toString().getBytes();
                  return response;
                }

                // Roll dice and calculate sum
                int sum = 0;
                StringBuilder rolls = new StringBuilder();
                Random random = new Random();
                for (int i = 0; i < numDice; i++) {
                  int roll = random.nextInt(sides) + 1;
                  sum += roll;
                  rolls.append("Dice ").append(i + 1).append(": ").append(roll).append("\n");
                }

                // Generate response
                builder.append("HTTP/1.1 200 OK\n");
                builder.append("Content-Type: text/plain; charset=utf-8\n");
                builder.append("\n");
                builder.append("Rolls:\n").append(rolls.toString()).append("\nTotal Sum: ").append(sum);
              } catch (NumberFormatException e) {
                // Handle invalid input format
                builder.append("HTTP/1.1 400 Bad Request\n");
                builder.append("Content-Type: text/plain; charset=utf-8\n");
                builder.append("\n");
                builder.append("Error: Invalid input format. Please provide valid integers for sides and numDice.");
              }
            } else {
              // Handle missing parameters
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/plain; charset=utf-8\n");
              builder.append("\n");
              builder.append("Error: Both sides and numDice parameters are required for dice roll.");
            }
          } catch (Exception e) {
            // Handle any other exception that may occur
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/plain; charset=utf-8\n");
            builder.append("\n");
            builder.append("Error: Invalid query format. Please use the following format for dice roll: /roll?sides=<sides>&numDice=<numDice>");
          }
        }

        else {
          // if the request is not recognized at all

          builder.append("HTTP/1.1 400 Bad Request\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append("I am not sure what you want me to do...");
        }

        // Output
        response = builder.toString().getBytes();
      }
    } catch (IOException e) {
      e.printStackTrace();
      response = ("<html>ERROR: " + e.getMessage() + "</html>").getBytes();
    }

    return response;
  }

  /**
   * Method to read in a query and split it up correctly
   * @param query parameters on path
   * @return Map of all parameters and their specific values
   * @throws UnsupportedEncodingException If the URLs aren't encoded with UTF-8
   */
  public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    // "q=hello+world%2Fme&bob=5"
    String[] pairs = query.split("&");
    // ["q=hello+world%2Fme", "bob=5"]
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
          URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }
    // {{"q", "hello world/me"}, {"bob","5"}}
    return query_pairs;
  }

  /**
   * Builds an HTML file list from the www directory
   * @return HTML string output of file list
   */
  public static String buildFileList() {
    ArrayList<String> filenames = new ArrayList<>();

    // Creating a File object for directory
    File directoryPath = new File("www/");
    filenames.addAll(Arrays.asList(directoryPath.list()));

    if (filenames.size() > 0) {
      StringBuilder builder = new StringBuilder();
      builder.append("<ul>\n");
      for (var filename : filenames) {
        builder.append("<li>" + filename + "</li>");
      }
      builder.append("</ul>\n");
      return builder.toString();
    } else {
      return "No files in directory";
    }
  }

  /**
   * Read bytes from a file and return them in the byte array. We read in blocks
   * of 512 bytes for efficiency.
   */
  public static byte[] readFileInBytes(File f) throws IOException {

    FileInputStream file = new FileInputStream(f);
    ByteArrayOutputStream data = new ByteArrayOutputStream(file.available());

    byte buffer[] = new byte[512];
    int numRead = file.read(buffer);
    while (numRead > 0) {
      data.write(buffer, 0, numRead);
      numRead = file.read(buffer);
    }
    file.close();

    byte[] result = data.toByteArray();
    data.close();

    return result;
  }

  /**
   *
   * a method to make a web request. Note that this method will block execution
   * for up to 20 seconds while the request is being satisfied. Better to use a
   * non-blocking request.
   * 
   * @param aUrl the String indicating the query url for the OMDb api search
   * @return the String result of the http request.
   *
   **/
  public String fetchURL(String aUrl) {
    StringBuilder sb = new StringBuilder();
    URLConnection conn = null;
    InputStreamReader in = null;
    try {
      URL url = new URL(aUrl);
      conn = url.openConnection();
      if (conn != null)
        conn.setReadTimeout(20 * 1000); // timeout in 20 seconds
      if (conn != null && conn.getInputStream() != null) {
        in = new InputStreamReader(conn.getInputStream(), Charset.defaultCharset());
        BufferedReader br = new BufferedReader(in);
        if (br != null) {
          int ch;
          // read the next character until end of reader
          while ((ch = br.read()) != -1) {
            sb.append((char) ch);
          }
          br.close();
        }
      }
      in.close();
    } catch (Exception ex) {
      System.out.println("Exception in url request:" + ex.getMessage());
    }
    return sb.toString();
  }
}
