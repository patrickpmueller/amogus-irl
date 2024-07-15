package com.pellacanimuller.amogus_irl;

import com.pellacanimuller.amogus_irl.game.Game;
import com.pellacanimuller.amogus_irl.net.GameWSServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

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

        ScheduledExecutorService startGameExecutor = Executors.newScheduledThreadPool(1);
        startGameExecutor.scheduleAtFixedRate(() -> {
                try {
                    gameserver.start();
                    startGameExecutor.shutdown();
                } catch (Throwable e) {
                    log.info("Port already in use, retrying in 3 seconds...");
                }
        }, 0, 3, TimeUnit.SECONDS);

        Scanner in = new Scanner(System.in);
        while (in.hasNextLine()) {
            String cmd = in.nextLine();
            cmd = cmd.strip();
            switch (cmd) {
                case "exit", "stop" -> {
                    log.info("Stopping Server");
                    gameserver.stop();
                    Executors.newScheduledThreadPool(1).schedule(() -> System.exit(0), 4, TimeUnit.SECONDS);
                }
                case "reset-soft", "soft-reset" -> gameserver.resetGame(new Game(), false);
                case "reset-hard", "hard-reset" -> gameserver.resetGame(new Game(), true);
            }
        }
    }
}
