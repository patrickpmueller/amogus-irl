package com.pellacanimuller.amogus_irl;

import com.pellacanimuller.amogus_irl.game.Game;
import com.pellacanimuller.amogus_irl.net.GameWSServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.Scanner;

public class Main {
    private final static Logger log = LogManager.getLogger(Game.class);

	public static void main(String[] args) throws InterruptedException {
        // STARTUP PROCEDURES:
        // 1. create game , set to lobby
        // 2. Create WS server
        // 3. Wait and react to game_start call
        // 4. Activate listeners, setup roles and tasks
        // 5. game loop
        Game game = new Game();
        InetSocketAddress address = new InetSocketAddress("localhost", 8080);
        GameWSServer gameserver = new GameWSServer(address, game);
        gameserver.start();

        Scanner in = new Scanner(System.in);
        while (in.hasNextLine()) {
            String cmd = in.nextLine();
            cmd = cmd.strip();
            switch (cmd) {
                case "exit", "stop" -> {
                    gameserver.stop();
                    log.info("Stopping Server");
                    Thread.sleep(4000);
                    return;
                }
            }
        }
    }
}
