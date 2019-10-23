package crossword;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.mit.eecs.parserlib.UnableToParseException;

/**
 * A crossword extravaganza game server
 */
public class CrosswordServer implements Runnable, Closeable {
    
    // Abstraction Function:
    //  - AF(serverSocket, puzzles, matches, clientConnections) = A crossword extyravaganza server hosted on server socket 
    //    serverSocket. Players are able to select between ongoing matches with one player in them, stored in matches where the 
    //    matches ID is the key and the Match object is the value. Players may also select from all available valid puzzles stored
    //    in the puzzles field. Each player is its own thread, stored in clientConnections.

    // Representation Invariant:
    //  - All players must have unique id's
    //  - All matches must have unique id's
    //  - A player may only be in one match at a time

    // Safety From Rep Exposure:
    //  - All fields are private
    //  - All fields are final
    //  - The serverSocket field is immutable
    //  - The puzzles field is mutable but contains immutable Puzzle objects
    //  - The matches and clientConnections fields are not shared with the Client
    //  - Fields are not modified, and communication with the client occurs only through immutable data types
    //  - The puzzle field is shared with the client, but it is an immutable object

    // Thread Safety Argument:
    //  - All fields are private
    //  - All fields are final
    //  - The serverSocket field is immutable
    //  - The puzzles field is mutable but contains immutable Puzzle objects
    //  - The matches and clientConnections fields are not shared with the Client
    //  - The mutable matches and clientConnections fields have threadsafe wrappers and all methods that modify them require a 
    //    lock
    
    private static final int PORT = 4949;
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    private final ServerSocket serverSocket;
    private final List<Puzzle> puzzles;
    private final ConcurrentMap<String, Match> matches = new ConcurrentHashMap<>();
    private final Set<ClientConnection> clientConnections = new HashSet<>();

    /**
     * Creates a new CrosswordServer using Puzzles in puzzleFolder
     *
     * @param puzzleFolder The folder containing all .puzzle files for the CrosswordServer to use
     * @throws IOException if an error occurs opening the server socket
     */
    public CrosswordServer(File puzzleFolder) throws IOException {
        this.serverSocket = new ServerSocket(PORT);
        this.puzzles = getValidPuzzles(puzzleFolder); // Load all valid puzzles
        checkRep();
    }

    /**
     * Asserts the rep invariant
     */
    private void checkRep() {
        assert(matches.entrySet().size() == matches.size());
    }

    /**
     * Run the server, listening for and handling client connections. Never returns normally.
     *
     * @throws IOException if an error occurs waiting for a connection
     */
    @Override
    public void run() {
        checkRep();
        System.err.println("Server listening on " + serverSocket.getLocalSocketAddress());
        isRunning.set(true);
        while (isRunning.get()) { // Block until a client connects
            try {
                Socket socket = serverSocket.accept();
                final ClientConnection clientConnection = new ClientConnection(socket, this); // Treat each new connection to the serverSocket (new player) as a separate thread
                clientConnections.add(clientConnection);
                clientConnection.start();
            } catch (IOException e) {
                break;
            }
        }
    }

    /**
     * Closes the socket
     */
    @Override
    public void close() throws IOException {
        checkRep();
        isRunning.set(false);
        serverSocket.close();
    }

    /**
     * Find all valid puzzle files stored in puzzleFolder. Validity is defined in the group project overview
     *
     * @param puzzleFolder
     * @return A list of valid Puzzle objects
     */
    private List<Puzzle> getValidPuzzles(File puzzleFolder) throws IOException {
        checkRep();
        List<Puzzle> validPuzzles = new ArrayList<Puzzle>();
        Stream.of(puzzleFolder.listFiles())
            .filter(file -> file.getName().endsWith(".puzzle"))
            .map(file -> {
                try {
                    return Files.readString(file.toPath());
                } catch (IOException e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .map(string -> {
                try {
                    return PuzzleParser.parse(string);
                } catch (UnableToParseException e) {
                    return null;
                }
            }).filter(Objects::nonNull)
            .filter(Puzzle::isValidPuzzle)
            .collect(Collectors.toCollection(() -> validPuzzles));

        if (validPuzzles.size() == 0) {
            throw new IllegalArgumentException("No valid puzzles found");
        }

        return validPuzzles;
    }

    /**
     * Get the open matches names that have already been initiated and waiting for a second client to join
     *
     * @return a Map of open matches
     */
    public Map<String, String> getOpenMatches() {
        checkRep();
        return Collections.unmodifiableMap(matches.entrySet()
            .stream()
            .filter(e -> !e.getValue().isStarted())
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().getPuzzle().getName()))
            .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue))
        );
    }

    /**
     * Get a list of puzzles names
     * 
     * @return list of puzzles names
     */
    public List<Puzzle> getPuzzlesForClient() {
        checkRep();
        return puzzles.stream().map(Puzzle::prepareForTransfer).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Creates Match match for a player on a given Puzzle
     *
     * @param player represents the player's id
     * @param matchId represents the match's id
     * @param name represents the puzzle name 
     * @return true if a match has been created made
     */
    public boolean createMatch(String player, String matchId, String name) {
        checkRep();
        final Optional<Puzzle> puzzle = puzzles.stream().filter(p -> p.getName().equals(name)).findAny();
        for (String id : matches.keySet()) { // Must be a unique match ID
            if (id.equals(matchId)) {
                return false;
            }
        }
        if (puzzle.isPresent()) {
            matches.put(matchId, new Match(puzzle.get(), player));
            return true;
        }
        return false;
    }

    /**
     * Performs the join between a client and already existing Match 
     *
     * @param playerId represents id of the play to join the match
     * @param matchId  represents id of the match the player decides to join
     * @return True if a match has been successfully made
     */
    public Match joinMatch(String playerId, String matchId) {
        checkRep();
        final Match match = matches.get(matchId);
        if (match != null) {
            final boolean matchJoined = match.tryJoin(playerId);
            if (matchJoined) {
                notifyOtherPlayer(match);
                return match;
            }
        }
        return null;
    }

    /**
     * Updates the state of the Match for all players in the Match. When one player updates their state, such as exiting the game,
     * it ensures that the other player does as well.
     * 
     * @param match A Match object
     */
    public void updateState(Match match) {
        checkRep();
        synchronized (match) {
            final Match.MatchState state = new Match.MatchState(match);
            updateState(state.playerOneId, state);
            updateState(state.playerTwoId, state);
        }
    }

    /**
     * Updates the state of the Match for one player
     * 
     * @param playerId A unique playerId. 
     * @param state The new game state
     */
    private void updateState(String playerId, Match.MatchState state) {
        checkRep();
        findConnection(playerId).ifPresent(connection -> connection.sendStateUpdate(state));
    }

    /**
     * Transitions both players connected to the Match into the score state
     * 
     * @param match A Match object
     */
    public void sendScore(Match match) {
        checkRep();
        synchronized (match) {
            final Match.MatchState state = new Match.MatchState(match);
            for (ClientConnection client : clientConnections) {
                if (client.getClientId().equals(state.playerOneId) | client.getClientId().equals(state.playerTwoId)) {
                    client.sendScore();
                }
            } 
        }
    }

    /**
     * Starts the Match once both players have connected
     * 
     * @param match A Match object
     */
    private void notifyOtherPlayer(Match match) {
        checkRep();
        synchronized (match) {
            final Match.MatchState state = new Match.MatchState(match); // playerOneId is always the creator
            findConnection(state.playerOneId).ifPresent(connection -> connection.startMatch(match));
        }
    }

    /**
     * Checks to see that the Client is still connected to the Server
     * 
     * @param creatorId The unique ID for a player
     * @return True if the player is connected to the server, else false
     */
    private Optional<ClientConnection> findConnection(String creatorId) {
        checkRep();
        synchronized (clientConnections) {
            return clientConnections.stream()
                    .filter(clientConnection -> creatorId.equals(clientConnection.getClientId()))
                    .findFirst();
        }
    }

    /**
     * Checks if an ID has been taken by an existing player and makes sure the ID does not have spaces or tab characters
     *
     * @param id A unique player ID
     * @return True if the id is free
     */
    public boolean isIdFree(String id) {
        checkRep();
        synchronized (clientConnections) {
            if (!id.contains(" ") & !id.contains("\t")) { // The ID cannot contain spaces or tabs
                return clientConnections.stream().noneMatch(clientConnection -> id.equals(clientConnection.getClientId()));
            }
            return false;
        }
    }
    
    /**
     * Removes the player from the match and allows their ID to be free again
     * 
     * @param clientConnection The Client connected to the server
     */
    public void quitMatch(ClientConnection clientConnection) {
        checkRep();
        synchronized (matches) {
            for (String matchId : matches.keySet()) {
                Match.MatchState match = new Match.MatchState(matches.get(matchId));
                if (match.playerOneId.equals(clientConnection.getClientId())) {
                    matches.remove(matchId); // Remove the match once it is over
                }
            }
        }
        synchronized (clientConnections) {
            clientConnections.remove(clientConnection);
        }
    }
    
    /**
     * Ensures that both players in a match transition to the showScore state
     * 
     * @param match A Match object
     */
    public void bothShowScore(Match match) {
        checkRep();
        synchronized (matches) {
            final Match.MatchState state = new Match.MatchState(match); // playerOneId is always the creator
            findConnection(state.playerOneId).ifPresent(connection -> connection.setState(ClientState.SHOW_SCORE));
            findConnection(state.playerTwoId).ifPresent(connection -> connection.setState(ClientState.SHOW_SCORE));
        }
    }
    
    /**
     * Recalculates available Matches and alerts all players in the CHOOSE state
     */
    public void updateMatches() {
        checkRep();
        synchronized (clientConnections) {
            for (ClientConnection client : clientConnections) {
                if (client.getClientState().equals(ClientState.CHOOSE)) { // If the player is in the choose state, update the list of Matches
                    try {
                        client.updateMatches();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
}