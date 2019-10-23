package crossword;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Represents a message sent over the socket. Each Message has a type and content
 */
public class Message implements Serializable {
    
    // Abstraction Function:
    //  - AF(type, inner): A message with type type, that has content stored in inner
    
    // Representation invariant:
    //  - inner must implement Serializable
    
    // Safety From Rep Exposure:
    //  - This is a container class. It has no mutators and all of its fields are final
    //  - Messages are shared with the Server and Client but are only read, not modified
    
    // Thread Safety Argument:
    // - Messages are thread-safe so long as their inner objects are thread-safe. Messages have no mutators, so the only danger 
    //   is in mutators on the inner object itself
    
    public static enum MessageType {SET_ID, PUZZLES, MATCHES, PICK_MATCH, NEW_MATCH, GAME_START, EXIT, TRY, CHALLENGE, GAME_STATE, ERROR, SCORE, WAIT, RESET} 
    public final MessageType type;
    public final Object inner;

    /**
     * Attempts to serialize a message to be sent over a socket
     */
    public static class Attempt implements Serializable{
        public int wordId;
        public String word;

        public Attempt(int wordId, String word) {
            this.wordId = wordId;
            this.word = word;
        }
    }

    /**
     * Create a new typed message
     * 
     * @param type the type of the message
     * @param obj the inner object to send
     */
    public Message(MessageType type, Object obj) {
        this.type = type;
        this.inner = obj;
        checkRep();
    }

    /**
     * Asserts the rep invariant
     */
    private void checkRep() {
        assert inner instanceof Serializable; 
    }
    
    /**
     * Creates a new message for setting the client's ID
     * 
     * @param id the id for the client
     * @return the new message
     */
    public static Message setId(final String id) { 
        return new Message(MessageType.SET_ID, id);
    }

    /**
     * Create a message for sending a Puzzle
     * 
     * @param puzzle the puzzle to send
     * @return the new message
     */
    public static Message sendPuzzles(List<Puzzle> puzzle) {
        return new Message(MessageType.PUZZLES, puzzle);
    }

    /**
     * Create a a message to send the puzzle state to the other client in the match
     * 
     * @param state The current state of the match
     * @return the new Message
     */
    public static Message sendMatchState(Match.MatchState state) {
        return new Message(MessageType.GAME_STATE, state);
    }

    /**
     * Create a message sending a Match
     * 
     * @param map matches to send
     * @return the new message
     */
    public static Message sendMatches(Map<String, String> map) {
        return new Message(MessageType.MATCHES, map);
    }

    /**
     * Create a message for playing an existing Match
     * 
     * @param matchId the id of the match to play
     * @return the new message
     */
    public static Message playMatch(String matchId) {
        return new Message(MessageType.PICK_MATCH, matchId);
    }

    /**
     * Create a message for creating a new Match
     * 
     * @param matchId the new match Id
     * @param puzzleId the new puzzle Id
     * @param description the puzzle description
     * @return the new message
     */
    public static Message newMatch(String matchId, String puzzleId, String description) {
        final List<Object> parts = List.of(matchId, puzzleId, description);
        return new Message(MessageType.NEW_MATCH, parts);
    }

    /**
     * Create a message for starting a new match
     * 
     * @param name The name of the Puzzle that was selected
     * @return the new message
     */
    public static Message gameStart(String name) {
        return new Message(MessageType.GAME_START, name);
    }

    /**
     * Create an exit message
     * 
     * @return the new message
     */
    public static Message exit() {
        return new Message(MessageType.EXIT, MessageType.EXIT);
    }

    /**
     * Create a message to try completing a word
     * 
     * @param wordId the id of the word
     * @param word the word to guess
     * @return the new Message
     */
    public static Message tryWord(int wordId, String word) {
        return new Message(MessageType.TRY, new Attempt(wordId, word));
    }

    /**
     * Create a message to challenge a word
     * 
     * @param wordId the id of the word
     * @param word the word to guess
     * @return the new Message
     */
    public static Message challengeWord(int wordId, String word) {
        return new Message(MessageType.CHALLENGE, new Attempt(wordId, word));
    }

    /**
     * Create an error message
     * 
     * @param error the message to be sent with the error
     * @return the new Message
     */
    public static Message error(String error) {
        return new Message(MessageType.ERROR, error);
    }

    /**
     * Create a score message
     * 
     * @param string the score for the game
     * @return the new message
     */
    public static Message score(String string) {
        return new Message(MessageType.SCORE, string);
    }
    
    /**
     * @return hash code value consistent with the equals() definition of structural equality, such that for all 
     *         e1,e2:Message, e1.equals(e2) implies e1.hashCode() == e2.hashCode()
     */
    @Override
    public int hashCode() {
        return type.hashCode() + inner.hashCode();
    }

    /**
     * @param that any object
     * @return true if and only if this and that are structurally-equal Messages
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof Message) {
            final Message that = (Message) other;
            return type == that.type && inner.equals(that.inner);
        }
        return false;
    }

    /**
     * @return a human readable representation of the Puzzle
     */
    @Override
    public String toString() {
        return "<" + type.toString() + ": " + inner.toString() + ">";
    }
}
