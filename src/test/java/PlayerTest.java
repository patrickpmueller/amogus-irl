import com.pellacanimuller.amogus_irl.game.Task;
import com.pellacanimuller.amogus_irl.game.players.Crewmate;
import com.pellacanimuller.amogus_irl.game.players.Healer;
import com.pellacanimuller.amogus_irl.game.players.Impostor;
import com.pellacanimuller.amogus_irl.game.players.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class PlayerTest {
    @Mock
    private Runnable shouldBeCalled;

    @Test
    public void testCrewmateTasks() {
        MockitoAnnotations.openMocks(this);

        Player player = new Player("player1");
        Set<Task> tasks = new HashSet<>();
        Task task1 = new Task("task1");
        Task task2 = new Task("task2");
        tasks.add(task1);
        tasks.add(task2);
        Crewmate crewmate = new Crewmate(player, tasks);

        assertEquals(2, crewmate.tasks.size());
        assertTrue(crewmate.tasks.contains(task1));
        assertTrue(crewmate.tasks.contains(task2));

        Task task3 = new Task("task3");
        crewmate.completeTask(task3, () -> fail("Task Set not empty but finished task set callback was called"));
        assertEquals(2, crewmate.tasks.size());
        assertTrue(crewmate.tasks.contains(task1));
        assertTrue(crewmate.tasks.contains(task2));

        crewmate.completeTask(task1, () -> fail("Task set not empty but finished task set callback was called"));
        assertEquals(1, crewmate.tasks.size());
        assertTrue(crewmate.tasks.contains(task2));

        crewmate.completeTask(task2, shouldBeCalled);
        assertEquals(0, crewmate.tasks.size());
        verify(shouldBeCalled, times(1)).run();

        crewmate.incompleteTask(task3);
        assertEquals(1, crewmate.tasks.size());
        assertTrue(crewmate.tasks.contains(task3));
    }

    @Test
    public void testCrewmateCopy() {
        Player player = new Player("player1");
        Set<Task> tasks = new HashSet<>();
        tasks.add(new Task("task1"));
        Crewmate original = new Crewmate(player, tasks);
        Crewmate copy = (Crewmate) original.copy();

        assertEquals(original.id, copy.id);
        assertEquals(original.tasks, copy.tasks);
    }

    @Test
    public void testImpostorCopy() {
        Player player = new Player("player1");
        Set<Task> tasks = new HashSet<>();
        tasks.add(new Task("task1"));
        Impostor original = new Impostor(player, tasks);
        Impostor copy = (Impostor) original.copy();

        assertEquals(original.id, copy.id);
        assertEquals(original.mockTasks, copy.mockTasks);
    }

    @Test
    public void testPlayerEquals() {
        Player p1 = new Player("p1");
        Player p1a = new Player("p1");
        Player p2 = new Player("p2");

        Crewmate p1b = new Crewmate(p1, new HashSet<>());
        Impostor p2a = new Impostor(p2, new HashSet<>());
        Healer p3 = new Healer(new Player("p3"), new HashSet<>());

        assertEquals(p1, p1a);
        assertEquals(p1, p1b);
        assertNotEquals(p1, p2);
        assertEquals(p2, p2a);
        assertNotEquals(p3, p1);
        assertNotEquals(p3, p2);

        assertEquals(p1.hashCode(), p1a.hashCode());
        assertEquals(p1.hashCode(), p1b.hashCode());
        assertNotEquals(p1.hashCode(), p2.hashCode());
        assertEquals(p2.hashCode(), p2a.hashCode());
        assertNotEquals(p3.hashCode(), p1.hashCode());
        assertNotEquals(p3.hashCode(), p2.hashCode());
    }
}
