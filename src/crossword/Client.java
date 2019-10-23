package crossword;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;

/**
 * Represents a Client playing a crossword game
 */
public class Client {
    
    private static final int PORT = 4949;
    private static enum GameState {START, CHOOSE, WAIT, PLAY, SCORE}
    private static final int CANVAS_WIDTH = 1200;
    private static final int CANVAS_HEIGHT = 900;
    
    private final String host;
    private String id;
    private GameState state = GameState.START;
    private Map<String, Puzzle> puzzles = new HashMap<>();
    private CrosswordWindow window;
    
    // Abstraction Function: 
    //  - AF(host, canvas, state, puzzles, window) = A client connected to host:4949, with a canvas to display the game. State is
    //    the state of the game, and puzzles are all of the valid puzzles the Client may play on. Window is the 
    //    CrosswordWindow used to display the Match.
    
    // Representation Invariant:
    //  - If puzzles.size() == 0, state must be START or CHOOSE
    
    // Safety From Rep Exposure:
    //  - All fields are private
    //  - The host field is final
    //  - The host and id fields are immutable
    //  - All fields are contained within the class and are not shared with the Server
    
    // Thread Safety Argument:
    //  - Not thread-safe. Each Client instance should only be used from one thread at a time.
     
    /**
     * Create a new client connecting to host on port PORT
     * @param host the host to connect to
     */
    public Client(String host) {
        this.host = host;
        checkRep();
    }
    
    /**
     * Asserts the rep invariant
     */
    private void checkRep() {
        if (puzzles.size() == 0) {
            assert(state.equals(GameState.START) | state.equals(GameState.CHOOSE));
        }
    }
    
    /**
     * Start a text-protocol game client using the given arguments. The command line argument should be in the format 
     * "java -cp <path> crossword.Client <host> [port]" where path is the location of all of your class files,
     * host corresponds to the path of the server to connect to, and port corresponds to the port. 
     * If host is "localhost", you will connect to a server on your local machine.
     * 
     * @param args The command line arguments as defined above
     * @throws IOException if an error occurs communicating with the server
     * @throws ClassNotFoundException 
     */
    public static void main(String[] args) throws IOException {
        final Queue<String> arguments = new LinkedList<>(List.of(args));
        final String host;
        
        try {
            host = arguments.remove();
        } 
        catch (NoSuchElementException e) {
            throw new IllegalArgumentException("Missing a host", e);
        }
        new Client(host).connect(); // create and connect to a client
    }
    
    /**
     * Connect to the server. This blocks until the Client is disconnected
     * 
     * @throws IOException if an error occurs during the game, or while connected to the server
     */
    public void connect() throws IOException {   
        checkRep();
        try (
            Socket socket = new Socket(host, PORT);
            ObjectOutputStream socketOut = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream socketIn = new ObjectInputStream(socket.getInputStream());
            BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in));
        ) { // try-with-resources (https://github.com/6031-sp19/ex23-echo/blob/master/src/echo/EchoServer.java#L31-L47)
            window = new CrosswordWindow(CANVAS_WIDTH, CANVAS_HEIGHT, (event) -> enterPressed(event, socketOut));
            window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            window.addWindowListener((WindowListener) new WindowAdapter() { // Trigger the exit command when the window is closed
                @Override
                public void windowClosing(WindowEvent event) {
                    try {
                        socketOut.writeObject(Message.exit());
                    } 
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    finally {
                        if (!state.equals(GameState.PLAY)) { // Dont close the window if we are in the play state
                            window.dispose();
                            System.exit(0);
                        }
                    }
                }
            });
            while (!socket.isClosed()) { // Loop until exited, and switch on different messages
                Message msg = (Message) socketIn.readObject();
                switch (msg.type) {
                case MATCHES: { // Mapping from match_ids to descriptions
                    if (state == GameState.CHOOSE) { // Be able to update matches in the CHOOSE state
                        handleMatches((Map<String, String>) msg.inner); 
                    }
                    if (state == GameState.SCORE) { // Reset everything in the Score state
                        handleReset((Map<String, String>) msg.inner);
                    }
                    break;
                }
                case PUZZLES: { // List of all available puzzles
                    handlePuzzles((List<Puzzle>) msg.inner);
                    break;
                }
                case GAME_START: { // Start of a game
                    System.out.println("New game");
                    handleStart((String) msg.inner);
                    break;
                }
                case GAME_STATE: { // Words update
                    System.out.println("State update");
                    handleUpdate((Match.MatchState) msg.inner);
                    break;
                }
                case WAIT: { // Check to see if a Match was started
                    System.out.println("State updated");
                    handleWait((Boolean) msg.inner);
                    break;
                }
                case ERROR: { // Display the error
                    handleError((String) msg.inner);
                    break;
                }
                case SCORE: {
                    System.out.println("State updated");
                    handleScore((String) msg.inner);
                    break;
                }
                case EXIT: {
                    socket.close();
                    break;
                }
                default: {
                    throw new IOException("Unhandled message type " + msg.type.toString());
                }
                }
            }
        } catch (ClassNotFoundException e) { // Re-raise as IOException
            throw new IOException(e.getLocalizedMessage());
        } catch (EOFException e) { // Connection was closed
            System.out.println("Connection closed.");
        } finally {
            if (window != null) {
                window.dispose();
            }
        }
    }
    
    /**
     * Handle the enter button being pressed
     * 
     * @param e An ActionEvent
     * @param out An ObjectOutputStream
     */
    private void enterPressed(ActionEvent e, ObjectOutputStream out) { 
        final String command = window.getText();
        window.clearText();
        try {
            sendCommand(command, out);
        } catch (ParseException e1) {
            window.setStatus(e1.getLocalizedMessage());
        } catch (IOException e1) {
            window.setStatus(e1.getLocalizedMessage());
        }
    }
    
    /**
     * Parses a command, sending it to the server if it is valid.
     * 
     * @param command the command to parse
     * @param out the output stream to send the message to
     * @throws ParseException if the command is invalid for the current state
     * @throws IOException if an error occurred sending the command
     */
    public void sendCommand(final String command, ObjectOutputStream out) throws ParseException, IOException {
        out.reset();
        Message msg = null;
        GameState newState = state;
        final Matcher m;
        switch(state) {
        case CHOOSE: { // Valid commands are PLAY <match_id>, NEW <match_id> <puzzle_id> <description>, EXIT
            final Matcher typeMatch = Pattern.compile("(PLAY|NEW|EXIT).*").matcher(command);
            if (typeMatch.matches()) {
                final String type = typeMatch.group(1);
                // Both PLAY and NEW set the state to WAIT
                if (type.equals("PLAY")) { // PLAY <match_id>
                    m = Pattern.compile("PLAY (\\w+)").matcher(command);
                    if (m.matches()) {
                        final String matchId = m.group(1);
                        msg = Message.playMatch(matchId);
                    } else {
                        window.setStatus("Invalid Match ID");
                        throw new ParseException("Invalid match id", 0);
                    }
                } else if (type.equals("NEW")) { // NEW <match_id> <puzzle_id> <description>
                    m = Pattern.compile("NEW (\\w+) (.+) (.+)").matcher(command);
                    if (m.matches()) {
                        final String matchId = m.group(1);
                        final String puzzleId = m.group(2);
                        final String description = m.group(3);
                        msg = Message.newMatch(matchId, puzzleId, description);
                    } else {
                        window.setStatus("Invalid NEW command");
                        throw new ParseException("Invalid NEW command", 0);
                    }
                } 
                else { // type.equals("EXIT")
                    msg = Message.exit();
                }
            } else {
                throw new ParseException("Invalid command", 0);
            }
            break;
        }
        case PLAY: { // Valid commands are TRY, CHALLENGE, and EXIT
            final Pattern typeP = Pattern.compile("(TRY|CHALLENGE|EXIT).*");
            final Matcher matchP = typeP.matcher(command);
            if (matchP.matches()) {
                final String type = matchP.group(1);
                if (type.equals("TRY")) { // TRY <id> <word>
                    m = Pattern.compile("TRY (\\d+) ([-a-z]+)").matcher(command);
                    if (m.matches()) {
                        final int id = Integer.parseInt(m.group(1));
                        final String word = m.group(2);
                        msg = Message.tryWord(id, word);
                        window.setStatus("Sent TRY " + m.group(1) + " " + word);
                    } else {
                        throw new ParseException("Invalid TRY command", 0);
                    }
                } else if (type.equals("CHALLENGE")) { // CHALLENGE <id> <word>
                    m = Pattern.compile("CHALLENGE (\\d+) ([-a-z]+)").matcher(command);
                    if (m.matches()) {
                        final int wordId = Integer.parseInt(m.group(1));
                        final String word = m.group(2);
                        msg = Message.challengeWord(wordId, word);
                        window.setStatus("Sent CHALLENGE " + m.group(1) + " " + word);
                    } else {
                        throw new ParseException("Invalid CHALLENGE command", 0);
                    }
                } else {
                    msg = Message.exit();
                }
            } else {
                throw new ParseException("Invalid command", 0);
            }
            break;
        }
        case SCORE: { // NEW MATCH or EXIT
            if (command.equals("NEW MATCH")) {
                state = GameState.CHOOSE;
                msg = new Message(Message.MessageType.RESET, ""); // Request all of the available matches be sent again
            } else if (command.equals("EXIT")) {
                msg = Message.exit();
            } else {
                throw new ParseException("Invalid command", 0);
            }
        }   
        break;
        case START: { // Valid commands are any string containing only a-z and dashes
            if (command.matches("[-a-z]+")) {
                msg = Message.setId(command);
                window.setName("Crossword - " + command); // Set the window title
                id = command; // Set the ID
            } 
            else if (command.equals("EXIT")) {
                msg = Message.exit();
            }
            else {
                throw new ParseException("Command must contain only a-z and dashes", 0);
            }
            break;
        }
        case WAIT: { // Valid command is EXIT
            if (command.equals("EXIT")) {
                msg = Message.exit();
            } else {
                throw new ParseException("Only EXIT command allowed in this state", 0);
            }
            break;
        }
        default: {
            msg = Message.exit();
            throw new ParseException("Invalid state", 0);
        }
        }
        if (msg != null) {
            out.writeObject(msg);
        }
        state = newState;
    }
    
    /**
     * Handles and displays an error message sent by the server. Depending on the type of error, the game may end prematurely
     * @param error the error message to display to the user
     */
    public void handleError(final String error) { 
        window.setStatus(error);
        checkRep();
    }
    
    /**
     * Handles and displays to the user a list of ongoing Matches. This method is only valid to use while in the CHOOSE state. 
     * It displays matches but does not transition to the PLAY state.
     * 
     * @param matches the map of matches to display
     */
    public void handleMatches(Map<String, String> matches) {
       assert state == GameState.CHOOSE; 
       window.setTitle("Select match or puzzle");
       window.setDescription("Enter PLAY <match_id>, NEW <match_id> <puzzle_id> <description>, or EXIT");
       final List<String[]> rows = new ArrayList<>();
       // Display all matches in the hints panel
       for (String id: matches.keySet()) {
           final String desc = matches.get(id);
           final String[] row = {id, desc};
           rows.add(row);
       }
       window.setRows(rows, "Matches");
       window.setStatus("CHOOSE state");
       checkRep();
    }
    
    /**
     * Handle puzzles sent by the server before the start of the game, and caches them This method is only valid in the START 
     * state. It transitions to the CHOOSE state
     * 
     * @param puzzles the list of all available puzzles
     */
    public void handlePuzzles(final List<Puzzle> puzzles) {
        assert state == GameState.START;
        window.displayPuzzles(puzzles);
        this.puzzles.clear();
        for (Puzzle puzzle: puzzles) {
            this.puzzles.put(puzzle.getName(), puzzle);
        }
        state = GameState.CHOOSE;
        checkRep();
    }
    
    /**
     * Updates the window to show the WAIT state. The WAIT state displays the Puzzle screen but does not allow input
     */
    public void doWait() {
        window.setTitle("Waiting...");
        window.setDescription("Enter EXIT");
        final StringBuilder infoString = new StringBuilder(); // Build the info box
        infoString.append("Blue IDs are words you own that have been confirmed.\n");
        infoString.append("Red IDs are words your opponent owns that have been confirmed.\n");
        infoString.append("Blue words are those you have entered.\n");
        infoString.append("Red words are those your opponent has entered.\n");
        window.setInfo(infoString.toString());
        window.hidePuzzles();
        final List<String[]> hints = new ArrayList<>();
        window.setRows(hints, "Hints");
        window.repaint();
        checkRep();
    }
    
    /**
     * Handle and display an update to the puzzle state. This method is only valid in the PLAY state
     * 
     * @param match the current state of the match
     */
    public void handleUpdate(Match.MatchState match) {
        checkRep();
        assert state == GameState.PLAY;
        System.out.println("Game state update");
        window.canvas.showMatch(match, id);
    }
    
    /**
     * Handle the game start message
     * 
     * This method is only valid in the WAIT state. It transitions to the PLAY state
     * @param name the name of the new game/puzzle
     */
    public void handleStart(final String name) {
        assert puzzles.containsKey(name); 
        window.setTitle("Game: " + name);
        window.setDescription("Enter TRY <word_id> <word>, CHALLENGE <word_id> <word>, or EXIT");
        window.setStatus("PLAY state");
        final StringBuilder infoString = new StringBuilder();
        // Build the info box
        infoString.append("Blue IDs are words you own that have been confirmed.\n");
        infoString.append("Red IDs are words your opponent owns that have been confirmed.\n");
        infoString.append("Blue words are those you have entered.\n");
        infoString.append("Red words are those your opponent has entered.\n");
        window.setInfo(infoString.toString());
        window.hidePuzzles();
        final Puzzle puzzle = puzzles.get(name);
        final List<String[]> hints = new ArrayList<>();
        final List<Entry> entries = puzzle.getEntries();
        for (int i=0; i<entries.size(); i++) {
            final String[] hint = {Integer.toString(i), entries.get(i).getClue()};
            hints.add(hint);
        }
        window.setRows(hints, "Hints");
        window.canvas.showPuzzle(puzzle);
        window.canvas.setVisible(true);
        window.repaint();
        state = GameState.PLAY;
        checkRep();
    }
    
    /**
     * Checks to see if the player is able to transition into the wait state
     * 
     * @param moveToWait A boolean that is true if we can go to the wait state, else false
     */
    private void handleWait(boolean moveToWait) {
        checkRep();
        if (state == GameState.CHOOSE) { // Must be in the choose state
            if (moveToWait) {
                state = GameState.WAIT;
                doWait();
            }
            else {
                window.setStatus("That Match ID is not valid, try again");
            }
        }
    }

    /**
     * Resets the window and returns the client to the choose state
     * 
     * @param matches A mapping from match names to puzzle names
     */
    private void handleReset(Map<String, String> matches) {
        checkRep();
        window.setTitle("Select match or puzzle");
        window.setDescription("Enter PLAY <match_id>, NEW <match_id> <puzzle_id> <description>, or EXIT");
        window.displayPuzzles(List.copyOf(this.puzzles.values()));
        final List<String[]> rows = new ArrayList<>();
        // Display all matches in the hints panel
        for (String id: matches.keySet()) {
            final String desc = matches.get(id);
            final String[] row = {id, desc};
            rows.add(row);
        }
        window.setRows(rows, "Matches");
        window.canvas.clear();
        window.setStatus("CHOOSE state");
        window.setInfo("");
        state = GameState.CHOOSE;
    }
    
    /**
     * Handles and displays the final score to the player.
     * This method is only valid while in the PLAY state. It transitions the Client to the SCORE state.
     * @param score
     */
    public void handleScore(String score) { 
        assert state == GameState.PLAY;
        state = GameState.SCORE;
        window.setTitle(score);
        window.setDescription("Enter either NEW MATCH or EXIT");
        checkRep();
    }
    
    /**
     * Gets the state of the Client
     * @return the state of the client
     */
    public GameState getState() {
        checkRep();
        return state;
    }
    
    /**
     * Get the CrosswordWindow
     * @return the CrosswordWindow used for display
     */
    public CrosswordWindow getWindow() {
        checkRep();
        return window;
    }
    
}