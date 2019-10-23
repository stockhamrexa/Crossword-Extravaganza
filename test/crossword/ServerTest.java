package crossword;

import static crossword.Message.MessageType.GAME_START;
import static crossword.Message.MessageType.MATCHES;
import static crossword.Message.MessageType.NEW_MATCH;
import static crossword.Message.MessageType.PUZZLES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerTest {

    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    private CrosswordServer server;
    private Future future;

    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    // Testing strategy:
    // For server.getOpenMatches(), partition on:
    //  A server with no open matches
    //  A server with one open match
    //  A server with one open and one in-progress match
    // For server.createMatch(), partition on:
    //  A matchId that already exists
    //  A puzzleId that does not exist
    //  A matchId that does not exist and a puzzleId that does
    // For server.joinMatch(), partition on:
    //  A matchId that doesn't exist
    //  A matchId that is open
    //  A matchId that is ongoing
    // For checking Server puzzle publishing, partition on:
    //  A directory with just one, consistent puzzle
    //  A directory with one consistent and one inconsistent puzzle
    //  A directory with just one, inconsistent puzzle
    //  A directory with a complex, consistent puzzle

    @BeforeEach
    void startServer() throws IOException, InterruptedException {
        server = new CrosswordServer(new File("puzzles"));
        future = executorService.submit(server);
        socket = new Socket("127.0.0.1", 4949);
        socket.setSoTimeout(5000);
        inputStream = new ObjectInputStream(socket.getInputStream());
        outputStream = new ObjectOutputStream(socket.getOutputStream());
    }

    @AfterEach
    void stop() throws IOException, InterruptedException {
        server.close();
        executorService.awaitTermination(2, TimeUnit.SECONDS);
        future.cancel(true);
        socket.close();
    }

    @AfterAll
    static void tearDown() {
        executorService.shutdownNow();
    }
    
    @Test
    //covers: A server with no open matches
    void testOpenNone() {
        Map<String, String> matches = server.getOpenMatches();
        assertEquals(0, matches.size(), "expected no open matches");
    }
    
    @Test
    //covers: A server with one open match
    void testOpenOne() {
        server.createMatch("0", "foo", "Easy");
        assertEquals(Map.of("foo", "Easy"), server.getOpenMatches(), "expected one open match");
    }
    
    @Test
    //covers: A server with one open and one ongoing match
    void testOpenMix() {
        assertTrue(server.createMatch("0", "foo", "Easy"), "expected to create match");
        final Match match1 = server.joinMatch("1", "foo");
        assertNotNull(match1, "expected match to be non-null");
        assertTrue(server.createMatch("2", "bar", "Easy"));
        assertEquals(Map.of("bar", "Easy"), server.getOpenMatches(), "expected one open match");
    }
    
    @Test
    //covers: A matchId that exists already
    void testCreateTaken() {
        server.createMatch("0", "foo", "Easy");
        assertFalse(server.createMatch("1", "foo", "Easy"), "expected match creation to fail");
    }
    
    @Test
    //covers: A puzzleId that does not exist
    void testCreateBadPuzzle() {
        assertFalse(server.createMatch("0", "foo", "anextremelylongpuzzlename"), "expected match creation to fail");
    }
    
    @Test
    //covers: A valid matchId and puzzleId
    void testCreateGood() {
        assertTrue(server.createMatch("0", "foo", "Easy"), "expected to create match");
    }
    
    @Test
    //covers: A matchId that doesn't exist
    void testJoinMissing() {
        assertNull(server.joinMatch("0", "foo"), "expected match to be null");
    }
    
    @Test
    //covers: A matchId that is open
    void testJoinOpen() {
        server.createMatch("0", "foo", "Easy");
        assertNotNull(server.joinMatch("1", "foo"), "expected to join");
    }
    
    @Test
    //covers: A matchId that is already ongoing
    void testJoinOngoing() {
        server.createMatch("0", "foo", "Easy");
        server.joinMatch("1", "foo");
        assertNull(server.joinMatch("2", "foo"), "expected match to be null");
    }

}