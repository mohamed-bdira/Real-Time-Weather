package org.example;

public class Main {
    public static void main(String[] args)throws Exception {
        WeatherServer ws = new WeatherServer();
        ws.start();

        WeatherFetcherThread thread = new WeatherFetcherThread(ws);
        thread.start();
    }
}
