package org.example;
import org.json.JSONObject;
import java.net.http.*;
import java.net.URI;
public class WeatherFetcherThread extends Thread {

    private final WeatherServer server;
    private final String apikey =  "ebaae67bda16486ba90110251251411";

    public WeatherFetcherThread(WeatherServer server) {
        this.server = server;
    }

    @Override
    public void run (){
        HttpClient client = HttpClient.newHttpClient();

        while(true){
            try{
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("https://api.weatherapi.com/v1/current.json?key=" + apikey + "&q=Sousse")).build();

                String response = client.send(req, HttpResponse.BodyHandlers.ofString()).body();
                JSONObject json = new JSONObject(response);
                String temp = json.getJSONObject("current").get("temp_c").toString();

                server.broadcast("current temp: " + temp);

                Thread.sleep(5000);

            }catch (Exception ignored){}
        }
    }
}
