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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
  public byte[] createResponse(InputStream inStream) throws IOException {

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

          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          // extract path parameters
          query_pairs = splitQuery(request.replace("multiply?", ""));
          //Set init cars
          Integer num1;
          Integer num2;

          try {
            // if query is empty
            if (query_pairs.isEmpty() || query_pairs == null || !query_pairs.containsKey("num1") ||
              !query_pairs.containsKey("num2") || query_pairs.get("num1").isEmpty() || query_pairs.get("num2").isEmpty()) {

              // Generate response
              builder.append("HTTP/1.1 400 BAD REQUEST\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("You need to provide two numbers to multiply. You did not provide 2 numbers.");
            }

            // set vars if parameters are given
            else if (query_pairs.containsKey("num1") && query_pairs.containsKey("num2")) {
                num1 = Integer.parseInt(query_pairs.get("num1"));
                num2 = Integer.parseInt(query_pairs.get("num2"));
                // do math
                Integer result = num1 * num2;
                // Generate response
                builder.append("HTTP/1.1 200 OK\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("\n");
                builder.append("Result is: " + result);

            } else {
              throw new IllegalArgumentException("You must provide two numbers to multiply");
            }
          } catch (NumberFormatException e) {
            // If num1 or num2 is not a valid integer
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Invalid input: The parameters 'num1' and 'num2' must be integers.");
          }

        } else if (request.contains("github?")) {

          try {
            // pulls the query from the request and runs it with GitHub's REST API
            // check out https://docs.github.com/rest/reference/
            //
            // HINT: REST is organized by nesting topics. Figure out the biggest one first,
            //     then drill down to what you care about
            // "Owner's repo is named RepoName. Example: find RepoName's contributors" translates to
            //     "/repos/OWNERNAME/REPONAME/contributors"

            Map<String, String> query_pairs = new LinkedHashMap<String, String>();

            query_pairs = splitQuery2(request.replace("github?", ""));

            System.out.println(request);

            String query = query_pairs.get("query");
            System.out.println(query);

            String githubUrl = "https://api.github.com/" + query_pairs.get("query");
            String json = fetchURL(githubUrl);

            System.out.println(json);

            if (json == null || json.isEmpty()) { // failure
              builder.append("HTTP/1.1 400 BAD REQUEST\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Could not fetch json for url: " + githubUrl);

            } else { // success

              builder.append("HTTP/1.1 200 OK\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              // builder.append("Check the todos mentioned in the Java source file");

              // TODO: Parse the JSON returned by your fetch and create an appropriate
              // response based on what the assignment document asks for

              // Parse the String into a JSONObject
              JSONArray arr = new JSONArray(json);

              // Loop through the array and print out the name and url
              for (int i = 0; i < arr.length(); i++) {
                String fullName = arr.getJSONObject(i).getString("full_name");
                Long id = arr.getJSONObject(i).getLong("id");
                JSONObject owner = arr.getJSONObject(i).getJSONObject("owner");
                String login = owner.getString("login");

                builder.append("full_name: " + fullName + "\tid: " + id + "\towner/login: " + login + "\n<br />");
              }
            }
          } catch (Exception e) {
            builder.append("HTTP/1.1 400 BAD REQUEST\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Please check the formatting of your url. <br />");
            builder.append(e.getMessage());
            e.printStackTrace();
          }
        } else if (request.contains("birthday?")) {
          // This multiplies two numbers, there is NO error handling, so when
          // wrong data is given this just crashes

          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          // extract path parameters
          query_pairs = splitQuery(request.replace("birthday?", ""));
          //Set init cars
          Integer day;
          Integer month;

          try {
            // if query is empty
            if (query_pairs.isEmpty() || query_pairs == null || !query_pairs.containsKey("day") ||
              !query_pairs.containsKey("month") || query_pairs.get("day").isEmpty() || query_pairs.get("month").isEmpty()) {

              // Generate response
              builder.append("HTTP/1.1 400 BAD REQUEST\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("You need to provide two numbers, one for the integer representation of the month that you were born and the other for the day in which you were born.");
            }

            // set vars if parameters are given
            else if (query_pairs.containsKey("day") && query_pairs.containsKey("month")) {
                day = Integer.parseInt(query_pairs.get("day"));
                month = Integer.parseInt(query_pairs.get("month"));
                // do math
                LocalDate today = LocalDate.now();
                int currentYear = today.getYear();
                LocalDate birthday = LocalDate.of(currentYear, month, day);
                if (today.isAfter(birthday) || today.isEqual(birthday)) {
                  birthday = birthday.plusYears(1);
                }
                long daysUntilBirthday = today.until(birthday, ChronoUnit.DAYS);

                builder.append("HTTP/1.1 200 OK\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("\n");
                builder.append("Days until your next birthday: " + daysUntilBirthday);
            } else {
              throw new IllegalArgumentException("You must provide your birthday in the format 'day' and 'month' as integers.");
              }
            } catch (NumberFormatException e) {
              // If num1 or num2 is not a valid integer
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Invalid input");
          }
        } else if (request.contains("happyMadison?")) {

          Map<String, String> query_pairs = splitQuery3(request.replace("happyMadison?", ""));

          if (query_pairs.containsKey("movie") && query_pairs.containsKey("quote")) {
            String movie = query_pairs.get("movie");
            int quoteNo;

            try {
              quoteNo = Integer.parseInt(query_pairs.get("quote"));
            } catch (NumberFormatException e) {
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Invalid quote number");
              response = builder.toString().getBytes();
              return response;
            }

            String quote = "";
                  
            if (movie.equalsIgnoreCase("happy")) {
              switch(quoteNo) {
                case 1: 
                  quote = "Hey, why don't I just go eat some hay, make things out of clay, lay by the bay? I just may!";
                  builder.append("HTTP/1.1 200 OK\n");
                  builder.append("Content-Type: text/html; charset=utf-8\n");
                  builder.append("\n");
                  builder.append("<html><body>");
                  builder.append("<h1>Quote:</h1>");
                  builder.append("<p>" + quote + "</p>");
                  builder.append("</body></html>");
                  break;
                case 2:
                  quote = "Yeah, Right, And Grizzly Adams Had A Beard.";
                  builder.append("HTTP/1.1 200 OK\n");
                  builder.append("Content-Type: text/html; charset=utf-8\n");
                  builder.append("\n");
                  builder.append("<html><body>");
                  builder.append("<h1>Quote:</h1>");
                  builder.append("<p>" + quote + "</p>");
                  builder.append("</body></html>");
                  break;
                case 3:
                  quote = "My fingers hurt. Oh, well, now your back's gonna hurt, 'cause you just pulled landscaping duty. Anybody else's fingers hurt?... I didn't think so.";
                  builder.append("HTTP/1.1 200 OK\n");
                  builder.append("Content-Type: text/html; charset=utf-8\n");
                  builder.append("\n");
                  builder.append("<html><body>");
                  builder.append("<h1>Quote:</h1>");
                  builder.append("<p>" + quote + "</p>");
                  builder.append("</body></html>");
                  break;
                default:
                  builder.append("HTTP/1.1 400 Bad Request\n");
                  builder.append("Content-Type: text/html; charset=utf-8\n");
                  builder.append("\n");
                  builder.append("Invalid quote number. Try 1, 2, or 3.");
                  response = builder.toString().getBytes();
                  return response;
              }
            } else if (movie.equalsIgnoreCase("madison")) {
              switch(quoteNo) {
                case 1: 
                  quote = "If peeing your pants is cool, consider me Miles Davis.";
                  // Generate response
                  builder.append("HTTP/1.1 200 OK\n");
                  builder.append("Content-Type: text/html; charset=utf-8\n");
                  builder.append("\n");
                  builder.append("<html><body>");
                  builder.append("<h1>Quote:</h1>");
                  builder.append("<p>" + quote + "</p>");
                  builder.append("</body></html>");
                  break;
                case 2:
                  quote = "I award you no points, and may God have mercy on your soul.";
                  builder.append("HTTP/1.1 200 OK\n");
                  builder.append("Content-Type: text/html; charset=utf-8\n");
                  builder.append("\n");
                  builder.append("<html><body>");
                  builder.append("<h1>Quote:</h1>");
                  builder.append("<p>" + quote + "</p>");
                  builder.append("</body></html>");
                  break;
                case 3:
                  quote = "That Veronica Vaughn is one piece of ace, I know from experience dude. If you know what I mean.";
                  builder.append("HTTP/1.1 200 OK\n");
                  builder.append("Content-Type: text/html; charset=utf-8\n");
                  builder.append("\n");
                  builder.append("<html><body>");
                  builder.append("<h1>Quote:</h1>");
                  builder.append("<p>" + quote + "</p>");
                  builder.append("</body></html>");
                  break;
                default:
                  builder.append("HTTP/1.1 400 Bad Request\n");
                  builder.append("Content-Type: text/html; charset=utf-8\n");
                  builder.append("\n");
                  builder.append("Invalid quote number. Try 1, 2, or 3.");
                  response = builder.toString().getBytes();
                  return response;
              }
            }
            else {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Invalid movie. Try 'happy' or 'madison'.");
            response = builder.toString().getBytes();
            return response;
            }
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

    if (query.contains("&")) {
      // "q=hello+world%2Fme&bob=5"
      String[] pairs = query.split("&");
      // ["q=hello+world%2Fme", "bob=5"]
      for (String pair : pairs) {
        int idx = pair.indexOf("=");
        query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
            URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
      }
    }
    else if (query.contains("?") && !query.contains("&")){
      throw new IllegalArgumentException("Not all parameters given in the query.");
    }
    else if (query.contains("?") && !query.contains("num1=")){
      throw new IllegalArgumentException("Not all parameters given in the query.");
    }
    //{{"q", "hello world/me"}, {"bob","5"}}
    return query_pairs;
  }

  public static Map<String, String> splitQuery2(String urlString) throws UnsupportedEncodingException {

    Map<String, String> query_pairs = new LinkedHashMap<String, String>();

    System.out.println(urlString);

    String query = urlString;
    String[] pairs = query.split("&");

    for (String pair : pairs) {
        int idx = pair.indexOf("=");
        query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }

    return query_pairs;
  }

  public static Map<String, String> splitQuery3(String urlString) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<String, String>();

    // Print the URL to check if it's correct
    System.out.println("URL: " + urlString);

    // Remove the "happyMadison?" prefix
    String query = urlString.replace("happyMadison?", "");

    // Split the query string based on "&"
    String[] pairs = query.split("&");

    for (String pair : pairs) {
        int idx = pair.indexOf("=");
        // Extract key-value pairs and decode them
        String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
        String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
        // Put the key-value pair into the map
        System.out.println("Key: " + key + ", Value: " + value);
        query_pairs.put(key, value);
    }

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
