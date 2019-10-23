package crossword;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class ClientConnectionTest {
    private CrosswordServer server;
    private Future future;
    
    private Socket socket;
    private ObjectInputStream inp;
    private ObjectOutputStream out;
    private static ExecutorService executorService = Executors.newFixedThreadPool(3);
    
    private class ClientAction implements Callable<Object> {
        private final List<Message> messages;
        private Socket socket;
        
        public ClientAction(List<Message> messages) {
            this.messages = List.copyOf(messages);
        }

        @Override
        public Object call() throws Exception {
            socket = new Socket("127.0.0.1", 4949);
            socket.setSoTimeout(5000);
            final ObjectInputStream inp = new ObjectInputStream(socket.getInputStream());
            final ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            for (Message m: messages) {
                out.writeObject(m);
            }
            return inp.readObject();
        }
        
        public void close() {
            try {
                socket.close();
            } catch (Exception e) {
                assert true;
            }
        }
        
    }
    
    // Testing strategy:
    // Partition on:
    //  Setting a valid ID
    //  Setting an invalid ID
    //  Setting a taken ID
    //
    //  Creating a valid match
    //  Creating a match with an invalid puzzle
    //  Creating a match with an invalid name
    //
    //  Guess a word correctly
    //  Guess a word incorrectly
    
    @BeforeEach
    void setup() throws IOException { // Start the server
        server = new CrosswordServer(new File("puzzles"));
        future = executorService.submit(server);
        socket = new Socket("127.0.0.1", 4949);
        socket.setSoTimeout(5000);
        inp = new ObjectInputStream(socket.getInputStream());
        out = new ObjectOutputStream(socket.getOutputStream());
    }
    
    @AfterEach
    void stop() throws IOException, InterruptedException {
        server.close();
        executorService.awaitTermination(2, TimeUnit.SECONDS);
        future.cancel(true);
        socket.close();
    }
    
    private Message tryRead(final ObjectInputStream inp) throws ClassNotFoundException, IOException { // Try to read from the stream
        Object obj = inp.readObject();
        assertTrue(obj instanceof Message);
        return (Message) obj;
    }
    
    @Test
    // covers: Setting a valid ID
    public void testValidId() throws IOException, ClassNotFoundException {
        out.writeObject(Message.setId("foo"));
        final Message msg = tryRead(inp);
        assertNotEquals(Message.MessageType.ERROR, msg.type);
    }
    
    @Test
    // covers: Setting an invalid ID
    public void testInvalidId() throws IOException, ClassNotFoundException {
        out.writeObject(Message.setId("\t\t"));
        final Message msg = tryRead(inp);
        assertEquals(Message.MessageType.ERROR, msg.type);
    }
    
    @Test
    // covers: Setting a taken ID
    public void testTakenId() throws IOException, ClassNotFoundException, InterruptedException, ExecutionException, TimeoutException {
        final ClientAction action = new ClientAction(List.of(Message.setId("foo")));
        Future task = executorService.submit(action);
        try {
            task.get(5, TimeUnit.SECONDS);
            out.writeObject(Message.setId("foo"));
            final Message msg = tryRead(inp);
            assertEquals(Message.MessageType.ERROR, msg.type);
        } finally {
            action.close();
        }
    }
    
    @Test
    //covers: create valid match
    public void testCreateMatchValid() throws IOException, ClassNotFoundException {
        out.writeObject(Message.setId("0"));
        final Message puzzles = tryRead(inp);
        assertEquals(Message.MessageType.PUZZLES, puzzles.type);
        final Message matches = tryRead(inp);
        assertEquals(Message.MessageType.MATCHES, matches.type);
        out.writeObject(Message.newMatch("foo", "Easy", "hello world"));
        final Message newMatches = tryRead(inp);
        assertEquals(Message.MessageType.MATCHES, newMatches.type);
        final Message waitMsg = tryRead(inp);
        assertEquals(Message.MessageType.WAIT, waitMsg.type);
        assertTrue((boolean) waitMsg.inner);
    }
    
    @Test
    //covers: Create match with invalid puzzle
    public void testCreateMatchBadPuzzle() throws IOException, ClassNotFoundException {
        out.writeObject(Message.setId("0"));
        final Message puzzles = tryRead(inp);
        assertEquals(Message.MessageType.PUZZLES, puzzles.type);
        final Message matches = tryRead(inp);
        assertEquals(Message.MessageType.MATCHES, matches.type);
        out.writeObject(Message.newMatch("foo", "sdsdsdsddsdds", "hello world"));
        final Message newMatches = tryRead(inp);
        assertEquals(Message.MessageType.MATCHES, newMatches.type);
        final Message waitMsg = tryRead(inp);
        assertEquals(Message.MessageType.WAIT, waitMsg.type);
        assertFalse((boolean) waitMsg.inner);
    }
    
    @Test
    //covers: Create match with invalid name
    public void testCreateMatchBadName() throws IOException, ClassNotFoundException, InterruptedException, ExecutionException, TimeoutException {
        out.writeObject(Message.setId("0"));
        tryRead(inp);
        tryRead(inp);
        final ClientAction action = new ClientAction(List.of(Message.setId("1"), Message.newMatch("foo", "Easy", "foo")));
        final Future task = executorService.submit(action);
        try {
            task.get(5, TimeUnit.SECONDS);
            final Message newMatches = tryRead(inp);
            assertEquals(Message.MessageType.MATCHES, newMatches.type);
            out.writeObject(Message.newMatch("foo", "Easy", "foo"));
            tryRead(inp);
            final Message waitMsg = tryRead(inp);
            assertEquals(Message.MessageType.WAIT, waitMsg.type);
            assertFalse((boolean) waitMsg.inner);
        } finally {
            action.close();
        }
    }
    
    private void setupGame(final String matchId) throws IOException, ClassNotFoundException {
        out.writeObject(Message.setId("0"));
        tryRead(inp); // Puzzles
        tryRead(inp); // Matches
        server.createMatch("1", "foo", "Easy");
        out.writeObject(Message.playMatch(matchId));
    }
    
    @Test
    //covers: Guess a word correctly
    public void testTryCorrect() throws ClassNotFoundException, IOException {
        setupGame("foo");
        // Not in the CHOOSE state, so match updates are not sent by the server
        final Message start = tryRead(inp);
        assertEquals(Message.MessageType.GAME_START, start.type);
        assertEquals("Easy", (String) start.inner);
        out.writeObject(Message.tryWord(0, "table"));
        final Message msg = tryRead(inp);
        assertEquals(Message.MessageType.GAME_STATE, msg.type);
        final Match.MatchState state = (Match.MatchState) msg.inner;
        assertEquals(Map.of(0, "table"), state.guesses);
        assertEquals(Map.of(0, "0"), state.owner);
        assertEquals(Map.of(0, false), state.confirmed);
    }
}
