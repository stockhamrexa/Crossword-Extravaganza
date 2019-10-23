package crossword;

import java.awt.Dimension;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import crossword.Entry.WordDirection;

/**
 * A mutable and threadsafe crossword extravaganza Match. Implements serializable to allow Match objects to be
 * sent over a socket.
 */
public class Match implements Serializable {

    // Abstraction Function:
    //  - AF(puzzle, playerOneId, playerTwoId, playerOneScore, playerTwoScore, guesses, owner, confirmed) = A crossword 
    //    extravaganza match played on crossword puzzle puzzle. The match has two players represented by playerOneId and 
    //    playerTwoId. These ID's are unique, and the playerTwoId starts as null until a second player joins the Match. The Match
    //    is not mutable until playerTwoId != null. Each player has a score that starts at zero and can increase or decrease 
    //    over the course of the Match. Players make guesses in the crossword puzzle, and the status of these guesses are stored
    //    in guesses, owner, and confirmed where the unique ID of the Entry object they are guessing is the key. The value in the
    //    guesses Map is the word the player guessed, the value in the owner Map is the players ID, and the value in the confirmed
    //    Map is whether or not the players guess has been confirmed to be correct through a challenge.

    // Representation Invariant:
    //  - PlayerOneId != playerTwoId
    //  - If isStarted() == false then guesses, confirmed, and owner should have a size of zero
    //  - The length of guesses, owner, and confirmed are all equal
    //  - All values in owner must equal playerOneId or playerTwoId

    // Safety From Rep Exposure:
    //  - All fields are private
    //  - The puzzle, playerOneId, guesses, owner, and confirmed fields are final
    //  - All fields except guesses, owner, and confirmed are immutable
    //  - The puzzle field is shared with the client, but it is an immutable object
    //  - The playerTwoId field is not final but is not shared directly with the Client and is only modified
    //    by the Match class, maintaining the rep invariant.
    //  - The playerOneScore and playerTwoScores are not final but are not shared directly with the Client. They are only modified
    //    by the Match class, maintaining the rep invariant.
    //  - The guesses, owner, and confirmed fields are mutable but contain immutable objects. Defensive copies are made before 
    //    they are shared with the Client
    
    // Thread Safety Argument:
    //  - All fields are private
    //  - The puzzle, playerOneId, guesses, owner, and confirmed fields are final
    //  - All fields except guesses, owner, and confirmed are immutable
    //  - The puzzle field is shared with the client, but it is an immutable object
    //  - Thread safe by monitor pattern: all accesses to this clients rep are guarded by this objects lock
    //  - The guesses, owner, and confirmed fields all have threadsafe wrappers
    //  - Instances of this mutable ADT are shared only with threadsafe clients, which modify them using Match's
    //    threadsafe methods

    private final Puzzle puzzle;
    private final String playerOneId;
    private String playerTwoId;
    private Integer playerOneScore = 0; 
    private Integer playerTwoScore = 0;
    
    private final Map<Integer, String> guesses = Collections.synchronizedMap(new HashMap<Integer, String>()); 
    private final Map<Integer, String> owner = Collections.synchronizedMap(new HashMap<Integer, String>()); 
    private final Map<Integer, Boolean> confirmed = Collections.synchronizedMap(new HashMap<Integer, Boolean>()); 
    
    /**
     * Creates a new crossword extravaganza Match. Only one player is required to create a match, but two are required to play.
     *
     * @param puzzle The Puzzle that the crossword extravaganza match will be played on
     * @param playerOneId The unique id representing player one
     */
    public Match(Puzzle puzzle, String playerOneId) {
        this.puzzle = puzzle;
        this.playerOneId = playerOneId;
        this.playerTwoId = null;
        checkRep();
    }

    /**
     * Asserts the rep invariant
     */
    private void checkRep() {
        assert (!playerOneId.equals(playerTwoId));
        assert(guesses.size() == owner.size() & guesses.size() == confirmed.size());
        if (!isStarted()) {
            assert(guesses.size() == 0);
            assert(owner.size() == 0);
            assert(confirmed.size() == 0);
        }
        for (int entryId : owner.keySet()) {
            String playerId = owner.get(entryId);
            assert(playerId.equals(playerOneId) | playerId.equals(playerTwoId));
        }
    }
    
    /**
     * Attempts to add a second player to the Match
     * 
     * @param playerId The unique ID representing player two
     * @return True if player two successfully joined, else false
     */
    public synchronized boolean tryJoin(String playerId) {
        checkRep();
        if (this.playerTwoId == null) {
            this.playerTwoId = playerId;
            return true;
        }
        return false;
    }

    /**
     * @return True if the Match has started (two players have joined), else false
     */
    public synchronized boolean isStarted() {
        return playerTwoId != null;
    }

    /**
     * Get the puzzle this Match is using
     *
     * @return The Puzzle this Match is being played on
     */
    public synchronized Puzzle getPuzzle() {
        checkRep();
        return this.puzzle;
    }
    
    /**
     * Gets a Map of all guesses that have been made on the board so far
     * 
     * @return The guesses field
     */
    public synchronized Map<Integer, String> getGuesses() {
        checkRep();
        return this.guesses;
    }
    
    /**
     * Gets a Map of which player made each guess
     * 
     * @return The owner field
     */
    public synchronized Map<Integer, String> getOwners() {
        checkRep();
        return this.owner;
    }
    
    /**
     * Gets a Map of which guesses have been confirmed
     * 
     * @return The owner field
     */
    public synchronized Map<Integer, Boolean> getConfirmed() {
        checkRep();
        return this.confirmed;
    }

    /**
     * Get the score of the player with the given id. Returns the points the player accumulated from challenges plus the number of
     * correct words they have entered, correct or not.
     *
     * @param id The unique String representation of a player. Must be a valid id representing either playerOne or playerTwo,
     *           otherwise behavior is undefined
     * @return The score of the player with id id
     */
    public synchronized int getScore(String id) {
        checkRep();
        int score = 0;
        for (int entryId : guesses.keySet()) {
            if (owner.get(entryId).equals(id)) { // If it was a word entered by this player
                if (puzzle.getEntry(entryId).getWord().equals(guesses.get(entryId))) { // If the players guess was correct
                    score += 1;
                }
            }
        }
        if (playerOneId.equals(id)) {
            return playerOneScore + score;
        } 
        else {
            return playerTwoScore + score;
        }
    }
    
    /**
     * Updates the scores for the player represented by playerId by amount
     * 
     * @param playerId The ID of the player making the guess. Must be player one or player two
     * @param amount A given amount of points
     */
    private synchronized void updateScore(String playerId, int amount) {
        checkRep();
        if (playerOneId == playerId) {
            playerOneScore += amount;
        }
        else {
            playerTwoScore += amount;
        }
    }
    
    /**
     * Gets which player won the game
     * 
     * @return A string representing the final outcome of the game
     */
    public synchronized String getWinner() {
        checkRep();
        final int scoreOne = getScore(playerOneId);
        final int scoreTwo = getScore(playerTwoId);
        if (scoreOne > scoreTwo) {
            return "Final score: " + scoreOne + " - " + scoreTwo + " " + playerOneId + " won! " + playerTwoId + " lost.";
        }
        else if (scoreTwo > scoreOne) {
            return "Final score: " + scoreOne + " - " + scoreTwo + " " + playerTwoId + " won! " + playerOneId + " lost.";
        }
        else { 
            return "The match ended in a tie.";
        }
    }
    
    /**
     * Attempts to guess a word in the crossword puzzle as defined by the group project handout
     * 
     * @param playerId The ID of the player making the guess. Must be player one or player two
     * @param entryId The ID of the entry the player is guessing
     * @param word The word that the player is guessing
     * @return True if it was a valid guess attempt, else false
     */
    public synchronized boolean tryWord(String playerId, String entryId, String word) {
        checkRep();
        assert(playerId.equals(playerOneId) || playerId.equals(playerTwoId)); // Fail quickly if the ID is not valid
        if (entryExists(entryId)) { // If the entry is in the puzzle
            if (correctLength(entryId, word)) { // If the guess is the correct length
                if (validGuess(playerId, entryId, word)) { // If the guess is consistent
                    final int id = Integer.parseInt(entryId);
                    if (guesses.containsKey(id)) { // Remove the old guess (has to have been made by the player making this guess)
                        guesses.remove(id);
                        owner.remove(id);
                        confirmed.remove(id);
                    }
                    guesses.put(id, word);
                    owner.put(id, playerId);
                    confirmed.put(id,  false); // A guess is unconfirmed, even if its right
                    clearInconsistencies(entryId); // Clear out all words that conflict with this guess
                    return true;
                }
                return false;
            }
            return false;
        }
        else {
            return false;
        }
    }
    
    /**
     * Attempts to challenge a word in the crossword puzzle as defined in the group project handout
     * 
     * @param playerId The ID of the player making the guess. Must be player one or player two
     * @param entryId The ID of the entry the player is challenging
     * @param word The word that the player is guessing
     * @return True if it was a valid challenge attempt, else false
     */
    public synchronized boolean challengeWord(String playerId, String entryId, String word) {
        checkRep();
        assert(playerId.equals(playerOneId) | playerId.equals(playerTwoId)); // Fail quickly if the ID is not valid
        if (entryExists(entryId)) { // If the entry is in the puzzle
            if (validChallenge(playerId, entryId, word)) { // If the challenge can occur
                final int id = Integer.parseInt(entryId);
                final String correctWord = puzzle.getEntry(id).getWord();
                if (correctWord.equals(guesses.get(id))) { // If the word was originally correct
                    updateScore(playerId, -1);
                    confirmed.remove(id);
                    confirmed.put(id, true); // Confirms the original submission
                    return true;
                }
                else {
                    if (correctWord.equals(word)) { // If the challenger was correct
                        updateScore(playerId, 2);
                        guesses.remove(id); // Clears the original guess from the board
                        owner.remove(id);
                        confirmed.remove(id);
                        guesses.put(id, word);
                        owner.put(id, playerId);
                        confirmed.put(id, true); // Confirms the new guess
                        clearInconsistencies(entryId);
                        return true;
                    }
                    else {
                        updateScore(playerId, -1);
                        guesses.remove(id); // Clears the word from the board
                        owner.remove(id);
                        confirmed.remove(id);
                        return true;
                    }
                }
            }
            return false;
        }
        else {
            return false;
        }
    }
    
    /**
     * Checks to see if the entry exists in the puzzle
     * 
     * @param entryId A String representing the ID of the word being guessed
     * @return True if there is an entry in the Puzzle with id entryID, else false
     */
    public synchronized boolean entryExists(String entryId) {
        checkRep();
        final int id = Integer.parseInt(entryId);
        return id >= 0 & id < this.puzzle.getEntries().size();
    }
    
    /**
     * Check to see if the guess has the same length as the Entry
     * 
     * @param entryID A String representing the ID of the word being guessed
     * @param word The word the player is guessing
     * @return True if the word length is the same as the length of the word in entry, else false
     */
    public synchronized boolean correctLength(String entryId, String word) {
        checkRep();
        final int id = Integer.parseInt(entryId);
        final String entryWord = this.puzzle.getEntry(id).getWord();
        return word.length() == entryWord.length();
    }
    
    /**
     * Checks to see if the guess is consistent with the current state of the puzzle as defined by the group project handout
     * 
     * @param playerId The ID of the player making the guess. Must be player one or player two
     * @param entryId The ID of the entry the player is challenging
     * @param word The word that the player is guessing
     * @return True if the guess is consistent, else false
     */
    public synchronized boolean validGuess(String playerId, String entryId, String word) {
        checkRep();
        final int id = Integer.parseInt(entryId);
        final Entry entryOne = puzzle.getEntry(id); 
        
        if (guesses.containsKey(id)) { // If there is already a guess made for this word
            if (!owner.get(id).equals(playerId)) { // If that guess was made by another player, you have to challenge not guess
                return false;
            }
        }
                
        if (entryOne.getDirection() == WordDirection.ACROSS) {
            List<Dimension> entryOnePoints = new ArrayList<Dimension>();
            for (int i = entryOne.getColumn(); i < entryOne.getLength() + entryOne.getColumn(); i++) {
                entryOnePoints.add(new Dimension(entryOne.getRow(), i));
            }
            for (int i : guesses.keySet()) {
                if (i!=id) { // Don't check an entry against itself
                    List<Dimension> entryTwoPoints = new ArrayList<Dimension>();
                    Entry entryTwo = puzzle.getEntry(i);
                    if (entryTwo.getDirection() == WordDirection.DOWN) {
                        for (int j = entryTwo.getRow(); j < entryTwo.getLength() + entryTwo.getRow(); j++) {
                            entryTwoPoints.add(new Dimension(j, entryTwo.getColumn()));
                        }                
                        for (Dimension point : entryOnePoints) {
                            if (entryTwoPoints.contains(point)) {
                                final int row = point.width;
                                final int column = point.height;
                                if (word.charAt(column - entryOne.getColumn()) != guesses.get(i).charAt(row - entryTwo.getRow())) { // If the two words do not intersect at the same point
                                    if (confirmed.get(i).equals(true) | !owner.get(i).equals(playerId)) {
                                        return false;
                                    }
                                }
                            }
                        }    
                    }
                } 
            }
            return true;
        }       
        else {
            List<Dimension> entryOnePoints = new ArrayList<Dimension>();
            for (int i = entryOne.getRow(); i < entryOne.getLength() + entryOne.getRow(); i++) {
                entryOnePoints.add(new Dimension(i, entryOne.getColumn()));
            }
            for (int i : guesses.keySet()) {
                if (i!=id) { // Don't check an entry against itself
                    List<Dimension> entryTwoPoints = new ArrayList<Dimension>();
                    Entry entryTwo = puzzle.getEntry(i);
                    if (entryTwo.getDirection() == WordDirection.ACROSS) {
                        for (int j = entryTwo.getColumn(); j < entryTwo.getLength() + entryTwo.getColumn(); j++) {
                            entryTwoPoints.add(new Dimension(entryTwo.getRow(), j));
                        }               
                        for (Dimension point : entryOnePoints) {
                            if (entryTwoPoints.contains(point)) {
                                final int row = point.width;
                                final int column = point.height;
                                if (word.charAt(row - entryOne.getRow()) != guesses.get(i).charAt(column - entryTwo.getColumn())) { // If the two words do not intersect at the same point
                                    if (confirmed.get(i).equals(true) | !owner.get(i).equals(playerId)) {
                                        return false;
                                    }
                                }
                            }
                        }    
                    }
                }
            }
            return true;
        }
    }
        
    /**
     * Checks to see if the challenge is consistent as defined by the group project handout
     * 
     * @param playerId The ID of the player making the guess. Must be player one or player two
     * @param entryId The ID of the entry the player is challenging
     * @param word The word that the player is guessing
     * @return True if the challenge can occur, else false
     */
    public synchronized boolean validChallenge(String playerId, String entryId, String word) {
        checkRep();
        if (correctLength(entryId, word)) {
            final int id = Integer.parseInt(entryId);
            if (guesses.containsKey(id)) { // If someone has already attempted to guess this word
                if (!confirmed.get(id)) { // If the word has not been confirmed yet
                    if (!guesses.get(id).equals(word)) { // If the current guess is different than the previous one
                        if (!owner.get(id).equals(playerId)) { // If the challenged word belongs to the other player
                            return true;
                        }
                        return false;
                    }
                    return false;
                }
                return false;
            }
            return false;
        }
        else {
            return false; 
        }
    }
    
    /**
     * Removes all inconsistencies in the Match after a guess or challenge is successfully made. Will never remove confirmed words
     * because this method can not be called if there is an inconsistent intersection with a confirmed word.
     * 
     * @param entryId The unique ID of the entry that a guess was just made for
     */
    private synchronized void clearInconsistencies(String entryId) {
        checkRep();
        final int id = Integer.parseInt(entryId);
        assert(guesses.containsKey(id)); // Fail quickly if the guess is not in guesses
        final Entry entryOne = puzzle.getEntry(id);

        if (entryOne.getDirection().equals(WordDirection.ACROSS)) {
            List<Dimension> entryOnePoints = new ArrayList<Dimension>();
            for (int i = entryOne.getColumn(); i < entryOne.getLength() + entryOne.getColumn(); i++) {
                entryOnePoints.add(new Dimension(entryOne.getRow(), i));
            }
            Set<Integer> entryIds = new HashSet<Integer>(guesses.keySet());
            for (int i : entryIds) {
                if (i!=id) { // Don't check an entry against itself
                    List<Dimension> entryTwoPoints = new ArrayList<Dimension>();
                    Entry entryTwo = puzzle.getEntry(i);
                    if (entryTwo.getDirection() == WordDirection.DOWN) {
                        for (int j = entryTwo.getRow(); j < entryTwo.getLength() + entryTwo.getRow(); j++) {
                            entryTwoPoints.add(new Dimension(j, entryTwo.getColumn()));
                        }                
                        for (Dimension point : entryOnePoints) {
                            if (entryTwoPoints.contains(point)) {
                                final int row = point.width;
                                final int column = point.height;

                                if (guesses.get(id).charAt(column - entryOne.getColumn()) != guesses.get(i).charAt(row - entryTwo.getRow())) { // If the two words do not intersect at the same point
                                    guesses.remove(i); // Removes the word the guess intersects with
                                    owner.remove(i);
                                    confirmed.remove(i);
                                }
                            }
                        }    
                    }
                } 
            }
        }
        else {
            List<Dimension> entryOnePoints = new ArrayList<Dimension>();
            for (int i = entryOne.getRow(); i < entryOne.getLength() + entryOne.getRow(); i++) {
                entryOnePoints.add(new Dimension(i, entryOne.getColumn()));
            }  
            Set<Integer> entryIds = new HashSet<Integer>(guesses.keySet());
            for (int i : entryIds) {
                if (i!=id) {
                    List<Dimension> entryTwoPoints = new ArrayList<Dimension>();
                    Entry entryTwo = puzzle.getEntry(i);
                    if (entryTwo.getDirection() == WordDirection.ACROSS) {
                        for (int j = entryTwo.getColumn(); j < entryTwo.getLength() + entryTwo.getColumn(); j++) {
                            entryTwoPoints.add(new Dimension(entryTwo.getRow(), j));
                        }               
                        for (Dimension point : entryOnePoints) {
                            if (entryTwoPoints.contains(point)) {
                                final int row = point.width;
                                final int column = point.height;
                                if (guesses.get(id).charAt(row - entryOne.getRow()) != guesses.get(i).charAt(column - entryTwo.getColumn())) { // If the two words do not intersect at the same point
                                    guesses.remove(i); // Removes the word the guess intersects with
                                    owner.remove(i);
                                    confirmed.remove(i);
                                }
                            }
                        }    
                    }
                }
            }
        }
    }
    
    /**
     * Checks to see whether the Match has ended or not by having all words entered correctly
     * 
     * @return True if the Match has ended, else false
     */
    public synchronized boolean isGameOver() {
        checkRep();
        final int numEntries = puzzle.getEntries().size(); // The total number of words in the puzzle
        int numCorrect = 0;
        for (int entryId : confirmed.keySet()) {
            if (confirmed.get(entryId).equals(true)) { // If the word has been confirmed
                numCorrect += 1;
            }
            else if (puzzle.getEntry(entryId).getWord().equals(guesses.get(entryId))) { // If the word is unconfirmed but is correct
                numCorrect += 1;
            }
        }
        if (numCorrect == numEntries) {
            return true;
        }
        else {
            return checkForOverlap();
        }
    }
    
    /**
     * Checks for entries that overlap such that every letter i is contained in another entry. Returns true if they 
     * have all been entered correctly.
     * 
     * @return True if all overlapping entries have been filled in correctly
     */
    public synchronized boolean checkForOverlap() {
        checkRep();
        Set<Dimension> entryPoints = new HashSet<Dimension>();
        Set<Dimension> puzzlePoints = new HashSet<Dimension>();
        for (Entry entryOne : puzzle.getEntries()) { // Loop through all points in the crossword Puzzle
            if (entryOne.getDirection().equals(WordDirection.ACROSS)) {
                for (int i = entryOne.getColumn(); i < entryOne.getLength() + entryOne.getColumn(); i++) {
                    entryPoints.add(new Dimension(entryOne.getRow(), i));
                }
            }
            else {
                for (int i = entryOne.getRow(); i < entryOne.getLength() + entryOne.getRow(); i++) {
                    entryPoints.add(new Dimension(i, entryOne.getColumn()));
                }
            }
        }
        for (int entryId : guesses.keySet()) { // Loop through all entered guesses
            Entry entryOne = puzzle.getEntries().get(entryId); 
            if (entryOne.getWord().equals(guesses.get(entryId))) { // If the guess is correct
                if (entryOne.getDirection().equals(WordDirection.ACROSS)) {
                    for (int i = entryOne.getColumn(); i < entryOne.getLength() + entryOne.getColumn(); i++) {
                        puzzlePoints.add(new Dimension(entryOne.getRow(), i));
                    }
                }
                else {
                    for (int i = entryOne.getRow(); i < entryOne.getLength() + entryOne.getRow(); i++) {
                        puzzlePoints.add(new Dimension(i, entryOne.getColumn()));
                    }
                }
            }
        }
        return puzzlePoints.equals(entryPoints);
    }

    /**
     * @return hash code value consistent with the equals() definition of structural equality, such that for all
     * e1,e2:Match, e1.equals(e2) implies e1.hashCode() == e2.hashCode()
     */
    @Override
    public int hashCode() {
        synchronized (this) {
            checkRep();
            return this.toString().hashCode();
        }
    }

    /**
     * @param that any object
     * @return true if and only if this and that are structurally-equal Matches
     */
    @Override
    public boolean equals(Object that) {
        synchronized (this) {
            checkRep();
            if (that instanceof Match) {
                return this.toString().equals(that.toString());
            } else {
                return false;
            }
        }
    }

    /**
     * @return a human readable representation of the Match
     */
    @Override
    public String toString() {
        synchronized (this) {
            checkRep();
            return "Player One (" + playerOneId + ") " + " vs Player Two (" + playerTwoId + ") " + "on " + this.puzzle.getName();
        } 
    }
    
    /**
     * A secure version of the Match object that can be sent over a socket. Does not have a puzzle field as Puzzle objects contain
     * answers which cannot be shared with the Client.
     */
    public static class MatchState implements Serializable { // Everything but the puzzle
        public final String playerOneId;
        public final String playerTwoId;
        public Integer playerOneScore;
        public Integer playerTwoScore;
        
        public final Map<Integer, String> guesses;
        public final Map<Integer, String> owner;
        public final Map<Integer, Boolean> confirmed;
        
        /**
         * Returns an object representing the match state
         * 
         * @param m the match to serialize
         */
        public MatchState(Match m) {
            synchronized (m) {
                playerOneId = m.playerOneId;
                playerTwoId = m.playerTwoId;
                playerOneScore = m.playerOneScore;
                playerTwoScore = m.playerTwoScore;
                guesses = Collections.unmodifiableMap(m.guesses);
                owner = Collections.unmodifiableMap(m.owner);
                confirmed = Collections.unmodifiableMap(m.confirmed);
            } 
        }
    }
    
}