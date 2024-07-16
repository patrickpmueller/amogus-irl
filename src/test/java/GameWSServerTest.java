import com.pellacanimuller.amogus_irl.game.Game;
import com.pellacanimuller.amogus_irl.game.Meeting;
import com.pellacanimuller.amogus_irl.game.players.Healer;
import com.pellacanimuller.amogus_irl.game.players.Player;
import com.pellacanimuller.amogus_irl.net.GameWSServer;
import com.pellacanimuller.amogus_irl.util.TomlSettingsManager;
import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
public class GameWSServerTest {

    @Mock
    private Game gameMock;

    @Mock
    private Meeting meetingMock;

    @Mock
    private WebSocket webSocketMock;

    private GameWSServer gameWSServer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize GameWSServer with gameMock
        gameWSServer = spy(new GameWSServer(new InetSocketAddress(8081), gameMock));

        // Setup game settings
        Map<String, Object> settings = new HashMap<>();
        settings.put("roles.impostors", 1);
        settings.put("roles.crewmates", 1);
        settings.put("roles.healers", 1);
        settings.put("tasks.total", 4);
        settings.put("tasks.perPlayer", 2);
        settings.put("maxPlayers", 5);
        settings.put("meetings.duration", 1000);
        gameMock.updateSettings(settings);
    }

    @Test
    public void testOnMessage_setup() {
        Player player = new Player("player");
        when(webSocketMock.getAttachment()).thenReturn(player);
        gameMock.alive = new ArrayList<>();
        gameMock.players = new ArrayList<>();

        String setupPlayer = "[{\"action\": \"setup\", \"playerID\": \"player\"}]";
        String setupEmpty = "[{\"action\": \"setup\", \"playerID\": \"\"}]";
        gameWSServer.onMessage(webSocketMock, setupPlayer);
        gameWSServer.onMessage(webSocketMock, setupEmpty);
        verify(gameMock, times(1)).addPlayer("player");

        String startGame = "[{\"action\": \"startGame\"}]";
        gameWSServer.onMessage(webSocketMock, startGame);
        verify(gameMock, times(1)).startGame();
    }

    @Test
    public void testOnMessage_meeting() {
        String voteSkip = "[{\"action\": \"vote\", \"target\": \"skip\"}]";
        String votePlayer = "[{\"action\": \"vote\", \"target\": \"player2\"}]";
        Player player = new Player("player");
        Player player2 = new Player("player2");
        when(webSocketMock.getAttachment()).thenReturn(player);
        when(gameMock.getPlayer(anyString())).thenCallRealMethod();
        gameMock.alive = new ArrayList<>();
        gameMock.alive.add(player);
        gameMock.alive.add(player2);
        gameMock.players = new ArrayList<>();
        gameMock.players.add(player);
        gameMock.players.add(player2);

        gameWSServer.onMessage(webSocketMock, voteSkip);
        gameWSServer.onMessage(webSocketMock, votePlayer);
        verify(meetingMock, times(0)).vote(any(Player.class), any(Player.class));

        gameMock.currentMeeting = meetingMock;

        gameWSServer.onMessage(webSocketMock, voteSkip);
        gameWSServer.onMessage(webSocketMock, voteSkip);
        verify(meetingMock, times(2)).vote(eq(null), eq(player));

        gameWSServer.onMessage(webSocketMock, votePlayer);
        gameWSServer.onMessage(webSocketMock, votePlayer);
        verify(meetingMock, times(2)).vote(eq(player2), eq(player));

        String meeting = "[{\"action\": \"meeting\", \"death\": \"player\"}]";
        gameWSServer.onMessage(webSocketMock, meeting);
        verify(gameMock, times(1)).startMeeting(any(Player.class), anyString());
    }

    @Test
    public void testOnMessage_tasks() {
        Player player = new Player("player");
        when(webSocketMock.getAttachment()).thenReturn(player);
        gameMock.alive = new ArrayList<>();
        gameMock.alive.add(player);

        String taskCompleted = "[{\"action\": \"taskCompleted\", \"taskID\": \"task1\"}]";
        gameWSServer.onMessage(webSocketMock, taskCompleted);
        verify(gameMock, times(1)).completeTask(any(Player.class), anyString());

        String taskUncompleted = "[{\"action\": \"taskUncompleted\", \"taskID\": \"task1\"}]";
        gameWSServer.onMessage(webSocketMock, taskUncompleted);
        verify(gameMock, times(1)).incompleteTask(any(Player.class), anyString());
    }

    @Test
    public void testOnMessage_playerLives() {
        Player player = new Player("player");
        Healer healer = new Healer(new Player("healer"), new HashSet<>());
        gameMock.alive = new ArrayList<>();
        gameMock.alive.add(healer);
        gameMock.players = new ArrayList<>();
        gameMock.players.add(player);
        gameMock.players.add(healer);
        String heal = "[{\"action\": \"heal\", \"playerID\": \"player\"}]";
        when(gameMock.getPlayer(anyString())).thenCallRealMethod();

        when(webSocketMock.getAttachment()).thenReturn(healer);
        gameWSServer.onMessage(webSocketMock, heal);
        when(webSocketMock.getAttachment()).thenReturn(player);
        gameWSServer.onMessage(webSocketMock, heal);
        verify(gameMock, times(1)).healPlayer(eq(player), eq(healer));
    }

    @Test
    public void testOnMessage_changeSettings() {
        try (MockedStatic<TomlSettingsManager> settingsManager = Mockito.mockStatic(TomlSettingsManager.class)) {
            String changeSettings = "[{\"action\": \"changeSettings\", \"meetings.duration\": \"1000\"}]";
            gameWSServer.onMessage(webSocketMock, changeSettings);

            settingsManager.verify(() -> TomlSettingsManager.changeSettingsFromJson(any(), any()), times(1));
        }
    }

    @Test
    public void testBroadcastInfo() {
        WebSocket conn1 = mock(WebSocket.class);
        Player player1 = new Player("player1");
        when(conn1.getAttachment()).thenReturn(player1);

        WebSocket conn2 = mock(WebSocket.class);
        Player player2 = new Player("player2");
        when(conn2.getAttachment()).thenReturn(player2);

        WebSocket conn2a = mock(WebSocket.class);
        when(conn2a.getAttachment()).thenReturn(player2);

        List<WebSocket> connections = Stream.of(conn1, conn2, conn2a).collect(Collectors.toList());

        // Use reflection to set the private field directly
        try {
            Field connectionsField = WebSocketServer.class.getDeclaredField("connections");
            connectionsField.setAccessible(true);
            connectionsField.set(gameWSServer, connections);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            fail("Failed to set connections field via reflection");
        }

        // Mock TomlSettingsManager.readSettingsAsJson
        try (MockedStatic<TomlSettingsManager> settingsManager = Mockito.mockStatic(TomlSettingsManager.class)) {
            settingsManager.when(TomlSettingsManager::readSettingsAsJson).thenReturn("{\"setting1\":\"value1\"}");

            // Capture the broadcast message
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            doNothing().when(gameWSServer).broadcast(captor.capture());

            // Call the method under test
            gameWSServer.broadcastInfo();

            // Verify the broadcast method was called
            verify(gameWSServer).broadcast(anyString());

            // Verify the captured message
            String expectedMessage = "[{\"type\": \"playerlist\",\"data\": [\"player1\",\"player2\"]}," +
                    "{\"type\": \"settings\", \"data\": {\"setting1\":\"value1\"}}]";
            assertEquals(expectedMessage, captor.getValue());
        }
    }
}
