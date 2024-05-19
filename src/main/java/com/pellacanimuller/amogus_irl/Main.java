package com.pellacanimuller.amogus_irl;

import com.pellacanimuller.amogus_irl.game.Game;
import com.pellacanimuller.amogus_irl.net.GameWSServer;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class Main {
	public static void main(String[] args) throws UnknownHostException, IOException {
        Game game = new Game(8);
        InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("localhost"), 80);
        GameWSServer gameserver = new GameWSServer(address, game);
        gameserver.start();
		HttpServer httpserver = HttpServer.create(new InetSocketAddress(80), 0);
		httpserver.createContext("/");
		httpserver.start();	

    }
}
