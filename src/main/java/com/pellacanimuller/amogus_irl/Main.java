package com.pellacanimuller.amogus_irl;

import com.pellacanimuller.amogus_irl.game.Game;
import com.pellacanimuller.amogus_irl.net.GameWSServer;

import java.net.InetSocketAddress;

public class Main {
	public static void main(String[] args) {
        // STARTUP PROCEDURES:
        // 1. create game , set to lobby
        // 2. Create WS server
        // 3. Wait and react to game_start call
        // 4. Activate listeners, setup roles and tasks
        // 5. game loop
        Game game = new Game(8);
        InetSocketAddress address = new InetSocketAddress("localhost", 8080);
        GameWSServer gameserver = new GameWSServer(address, game);
        gameserver.start();
    }
}
