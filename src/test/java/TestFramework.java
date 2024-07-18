import com.pellacanimuller.amogus_irl.Main;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestFramework {
    @Test
    void testFramework() {
        assertTrue(true);
    }

    @Test
    void testGameCompilation() {
        try {
            Main.main(new String[]{"test"});
        } catch (Exception e) {
            fail("Failed to compile game: " + e.getMessage());
        }
        assertTrue(true);
    }
}
