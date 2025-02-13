import com.pellacanimuller.amogus_irl.game.Game;
import com.pellacanimuller.amogus_irl.game.Task;
import com.pellacanimuller.amogus_irl.game.players.Crewmate;
import com.pellacanimuller.amogus_irl.game.players.Impostor;
import com.pellacanimuller.amogus_irl.game.players.Player;
import com.pellacanimuller.amogus_irl.net.GameWSServer;
import com.pellacanimuller.amogus_irl.util.TomlSettingsManager;
import org.java_websocket.WebSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameTest {

    @InjectMocks
    private Game game;

    @Mock
    private GameWSServer wsServer;

    private static MockedStatic<TomlSettingsManager> settingsManager;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        settingsManager = Mockito.mockStatic(TomlSettingsManager.class);

        Map<String, Object> settings = new HashMap<>();

        settings.put("roles.impostors", 1);
        settings.put("roles.crewmates", 1);
        settings.put("roles.healers", 1);
        settings.put("tasks.total", 3);
        settings.put("tasks.perPlayer", 2);
        settings.put("maxPlayers", 4);
        settings.put("meeting.duration", 1000);

        settingsManager.when(TomlSettingsManager::readSettingsAsMap).thenReturn(settings);

        settingsManager.when(() -> TomlSettingsManager.flattenMap(any(), any())).thenCallRealMethod();
        settingsManager.when(() -> TomlSettingsManager.flatten(any(), any(), any())).thenCallRealMethod();

        game = new Game();
        game.acknowledgeServerStarted(wsServer);
    }

    @AfterEach
    public void tearDown() {
        settingsManager.close();
        game = null;
        settingsManager = null;
    }

    private Set<Task> setupTasks() {
        Task task1 = new Task();
        task1.id = "task1";
        Task task2 = new Task();
        task2.id = "task2";
        game.tasks = new Task[]{task1, task2};

        Set<Task> tasks = new HashSet<>();
        tasks.add(task1);
        tasks.add(task2);
        return tasks;
    }

    @Test
    public void testGameRunning() {
        assertFalse(game.gameRunning());
        game.gameState = Game.GameState.INGAME;
        assertTrue(game.gameRunning());
    }

    @Test
    public void testEndMeeting() {
        Player winner = new Player("winner");
        Player player1 = new Player("player1");
        Player player2 = new Player("player2");
        game.alive = new ArrayList<>();
        game.alive.add(player1);
        game.alive.add(player2);
        game.alive.add(winner);

        game.endMeeting(winner);

        verify(wsServer).broadcast("[{\"type\": \"result\", \"data\": \"winner\"}]");
        assertNull(game.currentMeeting);
        assertFalse(game.alive.contains(winner));
        assertTrue(game.alive.contains(player1));
        assertTrue(game.alive.contains(player2));
    }

    @Test
    public void testStartGame_WrongNumberOfPlayers() {
        WebSocket conn = mock(WebSocket.class);
        when(wsServer.getConnectionByPlayer(any())).thenReturn(conn);

        setupTasks();
        Map<String, Object> settings = new HashMap<>();
        settings.put("roles.impostors", 1);
        settings.put("roles.crewmates", 1);
        settings.put("roles.healers", 1);
        game.updateSettings(settings);

        Player player1 = new Player("player1");
        Player player2 = new Player("player2");
        game.addExistingPlayer(player1);
        game.addExistingPlayer(player2);

        assertThrows(IllegalStateException.class, () -> game.startGame());
    }

    @Test
    public void testStartGame_IncorrectGameState() {
        Player player1 = new Player("player1");
        Player player2 = new Player("player2");
        Player player3 = new Player("player3");
        game.addExistingPlayer(player1);
        game.addExistingPlayer(player2);
        game.addExistingPlayer(player3);

        WebSocket conn = mock(WebSocket.class);
        when(wsServer.getConnectionByPlayer(any())).thenReturn(conn);

        game.gameState = Game.GameState.INGAME;
        assertThrows(IllegalStateException.class, () -> game.startGame());
    }

    @Test
    public void testStartGame_InvalidTaskSettings() {
        WebSocket conn = mock(WebSocket.class);
        when(wsServer.getConnectionByPlayer(any())).thenReturn(conn);

        setupTasks();
        Map<String, Object> settings = new HashMap<>();
        settings.put("tasks.total", 2);
        settings.put("tasks.perPlayer", 2);
        game.updateSettings(settings);

        Player player1 = new Player("player1");
        Player player2 = new Player("player2");
        Player player3 = new Player("player3");
        game.addExistingPlayer(player1);
        game.addExistingPlayer(player2);
        game.addExistingPlayer(player3);

        game.gameState = Game.GameState.LOBBY;
        settings.put("tasks.perPlayer", 4);
        game.updateSettings(settings);
        assertThrows(IllegalStateException.class, () -> game.startGame());
    }

    @Test
    public void testStartGame_SuccessfulStart() {
        WebSocket conn = mock(WebSocket.class);
        when(wsServer.getConnectionByPlayer(any())).thenReturn(conn);

        setupTasks();

        Player player1 = new Player("player1");
        Player player2 = new Player("player2");
        Player player3 = new Player("player3");
        game.addExistingPlayer(player1);
        game.addExistingPlayer(player2);
        game.addExistingPlayer(player3);

        game.startGame();

        verify(conn, times(3)).send("[{\"type\":\"tasks\", \"data\": [" + anyString() + "]}]");
        verify(conn, times(3)).setAttachment(any(Player.class));
        assertEquals(Game.GameState.INGAME, game.gameState);
    }

    @Test
    public void testAddPlayer() {
        game.addPlayer("player1");
        game.addPlayer("player2");
        game.addPlayer("");

        assertEquals(2, game.players.size());

        assertThrows(IllegalStateException.class, () -> game.addPlayer("player1"));

        game.addPlayer("player3");
        game.addPlayer("player4");
        assertEquals(4, game.players.size());

        assertThrows(IllegalStateException.class, () -> game.addPlayer("player5"));
    }

    @Test
    public void testRemovePlayer() {
        Player player1 = new Player("player1");
        Player player2 = new Player("player2");
        Player player3 = new Player("player3");
        game.alive = new ArrayList<>();
        game.alive.add(player1);
        game.alive.add(player2);
        game.alive.add(player3);

        game.addExistingPlayer(player1);
        game.addExistingPlayer(player2);
        game.addExistingPlayer(player3);
        game.removePlayer(player1);
        assertEquals(2, game.players.size());
        assertEquals(2, game.players.size());

        game.removePlayer(null);
        game.removePlayer(player2);
        game.removePlayer(player3);
        assertTrue(true);
    }

    @Test void testGetPlayer() {
        Player player1 = new Player("player1");
        Player player2 = new Player("player2");
        Player player3 = new Player("player3");
        game.addExistingPlayer(player1);
        game.addExistingPlayer(player2);
        game.addExistingPlayer(player3);
        assertEquals(player1, game.getPlayer("player1"));
        assertEquals(player2, game.getPlayer("player2"));
        assertEquals(player3, game.getPlayer("player3"));
        assertThrows(IndexOutOfBoundsException.class, () -> game.getPlayer("player4"));
    }

    @Test
    public void testStartMeeting_NotInGame() {
        Player starter = new Player("starter");
        String deathID = "death";
        game.gameState = Game.GameState.LOBBY;

        assertThrows(IllegalStateException.class, () -> game.startMeeting(starter, deathID));
    }

    @Test
    public void testStartMeeting_StarterNotAlive() {
        Player starter = new Player("starter");
        String deathID = "death";
        game.gameState = Game.GameState.INGAME;
        game.alive = new ArrayList<>();

        assertThrows(IllegalStateException.class, () -> game.startMeeting(starter, deathID));
    }

    @Test
    public void testStartMeeting_PlayerDoesNotExist() {
        Player starter = new Player("starter");
        String deathID = "death";
        game.gameState = Game.GameState.INGAME;
        game.alive = new ArrayList<>();
        game.alive.add(starter);

        assertThrows(IllegalStateException.class, () -> game.startMeeting(starter, deathID));
    }

    @Test
    public void testStartMeeting_StartEmergencyMeeting() {
        Player starter = new Player("starter");
        String deathID = "emergency";
        game.gameState = Game.GameState.INGAME;
        game.alive = new ArrayList<>();
        game.alive.add(starter);

        game.startMeeting(starter, deathID);

        assertEquals(Game.GameState.MEETING, game.gameState);
        assertNotNull(game.currentMeeting);
        verify(wsServer).broadcast("[{ \"type\": \"meeting\", \"data\": \"emergency\" }]");
    }

    @Test
    public void testStartMeeting_StartDeathReportMeeting() {
        Player starter = new Player("starter");
        String deathID = "deadPlayer";
        Player deadPlayer = new Player(deathID);
        game.gameState = Game.GameState.INGAME;
        game.alive = new ArrayList<>();
        game.alive.add(starter);
        game.players.add(deadPlayer);

        game.startMeeting(starter, deathID);

        assertEquals(Game.GameState.MEETING, game.gameState);
        assertNotNull(game.currentMeeting);
        verify(wsServer).broadcast("[{ \"type\": \"meeting\", \"data\": \"deadPlayer\" }]");
    }

    @Test
    public void testCompleteTask() {
        Set<Task> taskset = setupTasks();
        Map<String, Object> settings = new HashMap<>();
        settings.put("roles.impostors", 1);
        settings.put("roles.crewmates", 1);
        settings.put("roles.healers", 1);
        settings.put("tasks.total", 2);
        settings.put("tasks.perPlayer", 2);
        settings.put("maxPlayers", 3);
        game.updateSettings(settings);

        game.gameState = Game.GameState.LOBBY;
        assertThrows(IllegalStateException.class, () -> game.completeTask(null, "task1"));

        game.gameState = Game.GameState.INGAME;

        Crewmate crewmate = new Crewmate(new Player("player1"), new HashSet<>(taskset));
        game.completeTask(crewmate, "task1");
        assertEquals(crewmate.tasks.size(), 1);
        game.completeTask(crewmate, "task1");
        assertEquals(crewmate.tasks.size(), 1);

        game.incompleteTask(crewmate, "task1");
        assertEquals(crewmate.tasks.size(), 2);

        Impostor impostor = new Impostor(new Player("player2"), new HashSet<>(taskset));
        game.completeTask(impostor, "task1");
        assertEquals(impostor.mockTasks.size(), 2);
    }

    @Test
    public void endGame_tooFewCrewmates() {
        Crewmate crewmate = mock(Crewmate.class);
        Impostor impostor = mock(Impostor.class);
        game.gameState = Game.GameState.INGAME;

        game.alive = new ArrayList<>();
        game.alive.add(crewmate);
        game.alive.add(crewmate);
        game.alive.add(impostor);
        game.checkWinConditions();
        verify(wsServer, never()).resetGame(any(Game.class), eq(true));

        game.alive.remove(crewmate);
        game.checkWinConditions();
        verify(wsServer, times(1)).resetGame(any(Game.class), eq(true));
        verify(wsServer, times(1)).broadcast("[{\"type\": \"endGame\", \"data\": \"impostors\"}]");
    }

    @Test
    public void endGame_tooFewImpostors() {
        Crewmate crewmate = mock(Crewmate.class);
        Impostor impostor = mock(Impostor.class);
        game.gameState = Game.GameState.INGAME;

        game.alive = new ArrayList<>();
        game.alive.add(crewmate);
        game.alive.add(crewmate);
        game.alive.add(impostor);
        verify(wsServer, never()).resetGame(any(Game.class), eq(true));

        game.alive.remove(impostor);
        game.checkWinConditions();
        verify(wsServer, times(1)).resetGame(any(Game.class), eq(true));
        verify(wsServer, times(1)).broadcast("[{\"type\": \"endGame\", \"data\": \"crewmates\"}]");
    }
    @Test
    public void endGame_tasksDone() {
        Crewmate crewmate = mock(Crewmate.class);
        Impostor impostor = mock(Impostor.class);
        game.gameState = Game.GameState.INGAME;

        game.alive = new ArrayList<>();
        game.alive.add(crewmate);
        game.alive.add(crewmate);
        game.alive.add(impostor);

        game.finishedTaskSet();
        game.checkWinConditions();
        verify(wsServer, never()).resetGame(any(Game.class), eq(true));

        game.finishedTaskSet();
        game.checkWinConditions();
        verify(wsServer, times(1)).resetGame(any(Game.class), eq(true));
        verify(wsServer, times(1)).broadcast("[{\"type\": \"endGame\", \"data\": \"crewmates\"}]");
    }

    @Test
    public void testUpdateSettings() {
        Game myGame = new Game();
        Map<String, Object> settings = TomlSettingsManager.readSettingsAsMap();
        settings.put("meeting.duration", 1000);
        myGame.updateSettings(settings);

        assertEquals(1000, myGame.MEETING_DURATION);
    }
}
