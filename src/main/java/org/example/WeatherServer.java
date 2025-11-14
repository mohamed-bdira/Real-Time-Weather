package org.example;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WeatherServer {
    private final Set<OutputStream> clients = ConcurrentHashMap.newKeySet();

    public void start() throws Exception{
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        //serve the html
        server.createContext("/", exchange -> {
            InputStream html = getClass().getResourceAsStream("/index.html");
            if(html == null){
                exchange.sendResponseHeaders(404, 0);
                exchange.close();
                return;
            }
            exchange.sendResponseHeaders(200, 0);
            html.transferTo(exchange.getResponseBody());
            exchange.close();
        });
        
        //Websocket handshake endpoint
        server.createContext("/ws", this::handleWebsocket);

        server.start();
        System.out.println("Server's running on http://localhost:8000");
    }

    private void handleWebsocket(HttpExchange exchange) throws IOException{
        if(!"GET".equals(exchange.getRequestMethod()) || !exchange.getRequestHeaders().containsKey("Sec-WebSocket-Key")){
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        String key = exchange.getRequestHeaders().getFirst("Sec-WebSocket-Key");
        String accept = getWebSocketAcceptKey(key);

        byte[] response = (
                "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: " + accept + "\r\n\r\n"
        ).getBytes();

        OutputStream out = exchange.getResponseBody();
        out.write(response);
        out.flush();

        System.out.println("WebSocket client connected");
        clients.add(out);
    }

    //WebSocket Accept Key

    private String getWebSocketAcceptKey(String key){
        try{
            String concat = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            return Base64.getEncoder().encodeToString(sha1.digest(concat.getBytes()));
        }catch (Exception e) {return null;}
    }

    //Send text to all clients
    public void broadcast(String message){
        byte[]frame = createWebSocketFrame(message);

        clients.removeIf(client -> {
            try{
                client.write(frame);
                client.flush();
                return false;
            }catch(IOException e){
                return true;
            }
        });
    }

    //WebSocket text frame
    private byte[] createWebSocketFrame(String message){
        byte[] data = message.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(data.length + 2);

        buffer.put((byte) 0x81);
        buffer.put((byte) data.length);
        buffer.put(data);

        return buffer.array();
    }
}
