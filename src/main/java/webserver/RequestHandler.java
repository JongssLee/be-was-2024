package webserver;

import java.io.*;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        logger.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder request = new StringBuilder();

            String line = reader.readLine();
            if (line != null && !line.isEmpty()) {
                request.append(line).append("\n");
                String[] tokens = line.split(" ");
                String url = tokens[1];
                logger.debug("Requested URL: " + url);

                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    request.append(line).append("\n");
                }
                logger.debug("HTTP Request Content:\n" + request.toString());

                DataOutputStream dos = new DataOutputStream(out);
                if ("/".equals(url)) {
                    response200Header(dos, "<html><body><h1>Hello World!</h1></body></html>".length(), "text/html");
                    responseBody(dos, "<html><body><h1>Hello World!</h1></body></html>".getBytes());
                } else {
                    File file = new File("src/main/resources/static" + url);

                    if (file.exists() && !file.isDirectory()) {
                        byte[] body = readFileToByteArray(file);
                        String contentType = getContentType(url);
                        response200Header(dos, body.length, contentType);
                        responseBody(dos, body);
                    } else {
                        response404Header(dos);
                    }
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private byte[] readFileToByteArray(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return data;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }
    }
    private String getContentType(String url) {
        if (url.endsWith(".html")) {
            return "text/html";
        } else if (url.endsWith(".css")) {
            return "text/css";
        } else if (url.endsWith(".js")) {
            return "application/javascript";
        } else if (url.endsWith(".ico")) {
            return "image/x-icon";
        } else if (url.endsWith(".png")) {
            return "image/png";
        } else if (url.endsWith(".jpg") || url.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (url.endsWith(".svg")) {
            return "image/svg+xml";
        } else {
            return "application/octet-stream";
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String contentType) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: " + contentType + ";charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void response404Header(DataOutputStream dos) {
        try {
            String body = "<html><body><h1>404 Not Found</h1></body></html>";
            byte[] bodyBytes = body.getBytes();
            dos.writeBytes("HTTP/1.1 404 Not Found \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + bodyBytes.length + "\r\n");
            dos.writeBytes("\r\n");
            dos.write(bodyBytes, 0, bodyBytes.length);
            dos.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
