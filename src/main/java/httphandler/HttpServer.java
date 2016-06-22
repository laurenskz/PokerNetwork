package httphandler;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.mongodb.util.JSON;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONException;
import org.json.JSONObject;
import statistics.CardViewer;

/**
 * Created by Jules on 22/06/2016.
 */
public class HttpServer {
    public static void main(String[] args) throws Exception {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response;
            int status = 200;

            BufferedReader reader = new BufferedReader(new InputStreamReader(t.getRequestBody()));
            StringBuilder sb = new StringBuilder();

            String s;
            while ((s = reader.readLine()) != null) {
                sb.append(s);
                sb.append("\n");
            }

            OutputStream out = t.getResponseBody();

            if (t.getRequestMethod().equals("POST")) {
                try {
                    JSONObject input = new JSONObject(sb.toString());
                    JSONObject possibleCards = CardViewer.getPossibleCards(input);
                    response = possibleCards.toString();

                } catch (JSONException e) {
                    response = "Bad request, Invalid JSON: " + e.getMessage();
                    status = 400;
                }
            } else {
                response = "<html><h1>405</h1><p>Method not allowed</p></html>";
                status = 405;
            }

            t.sendResponseHeaders(status, response.getBytes().length);

            out.write(response.getBytes());
            out.close();
        }
    }
}
