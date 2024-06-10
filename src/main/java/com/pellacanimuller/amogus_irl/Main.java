package com.pellacanimuller.amogus_irl;

import com.pellacanimuller.amogus_irl.game.Game;
import com.pellacanimuller.amogus_irl.net.GameWSServer;

import java.net.InetSocketAddress;

public class Main {
	public static void main(String[] args) {
        // STARTUP PROCEDURES:
        // 1. create game , set to lobby
        // 2. TODO Create WS server
        // 3. TODO Wait and react to game_start call
        // 4. TODO Activate listeners, setup tasks and roles
        // 5. TODO game loop
        Game game = new Game(8);
        InetSocketAddress address = new InetSocketAddress("localhost", 8080);
        GameWSServer gameserver = new GameWSServer(address, game);
        gameserver.start();
    }
}
