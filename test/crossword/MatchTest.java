package crossword;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import edu.mit.eecs.parserlib.UnableToParseException;

/**
 * Tests for the Match class
 */
public class MatchTest {

    /**
     * Testing Strategy for Match
     * 
     * For tryjoin(), partition on:
     *  - Players in Match = 1, 2
     *  
     * For isStarted(), partition on:
     *   - Players in match = 1, 2
     *   
     * For getScore(), partition on:
        - request by = playerOne, playerTwo
     *  - challenge made = yes, no
     *  - correct guesses = yes, no
     *  
     * For updateScore(), partition on:
     *  - score change = positive, negative
     *  
     * For getWinner(), partition on:
     *  - tie = yes, no
     *  - winner = playerOne, playerTwo
     *  
     * For entryExists(), partition on:
     *  - entryId exists = yes, no
     *  - entryId value = < 0, > puzzle.getEntries().size()
     *  
     * For correctLength(), partition on:
     *  - length matches entry = yes, no
     *  
     * For validGuess(), partition on:
     *  - guess made there by other player = yes, no
     *  - intersects another word = yes, no
     *  - intersects at same letter = yes, no
     *  - intersected word belongs to you = yes, no
     *  
     * For validChallenge(), partition on:
     *  - correct length = yes, no
     *  - has been guessed already = yes, no
     *  - guess is different = yes, no
     *  - word belongs to other player = yes, no
     *  
     * For tryWord(), partition on:
     *  - valid try = yes, no
     *  - word on board = yes, no
     *  - guess on your own word = yes, no
     *  
     * For challengeWord(), partition on:
     *  - valid challenge = yes, no
     *  - word originally correct = yes, no
     *  - word originally wrong and now correct = yes, no
     *  - both words incorrect = yes, no
     *  
     * For clearInconsistencies(), partition on:
     *  - after calling = tryWord(), challengeWord()
     *  - intersections on board = yes, no
     *  - word direction = across, down
     *  - number of intersections = 0, >0
     *  
     *  For isGameOver(), partition on:
     *  - Game over = yes, no
     *  - Overlapped words = yes
     */
    
    private static String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }
    
    // Covers: players in match = 1
    @Test
    public void testTryjoinValid() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        assertEquals(true, match.tryJoin(playerTwoId), "Expected the player to be able to join the Match");
    }
    
    // Covers: players in match = 2
    @Test
    public void testTryjoinInvalid() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        final String playerThreeId = "joe";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        assertEquals(false, match.tryJoin(playerThreeId), "Expected the plauyer to be unable to join the Match");
    }
    
    // Covers: players in match = 1
    @Test
    public void testStarted() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        assertEquals(true, match.isStarted(), "Expected the Match to be started");
    }
    
    // Covers: players in match = 2
    @Test
    public void testUnstarted() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        Match match = new Match(puzzle, playerOneId);
        assertEquals(false, match.isStarted(), "Expected the Match not to be started");
    }
    
    // Covers: request by = playerOne, challenge made = no, valid guesses = no
    @Test
    public void getStartingScore() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        assertEquals(0, match.getScore(playerOneId), "Expected the score to be 0");
    }
    
    // Covers: request by = playerOne, challenge made = no, valid guesses = yes
    @Test
    public void getPartialScore() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        assertEquals(1, match.getScore(playerOneId), "Expected the score to be 1");
        assertEquals(0, match.getScore(playerTwoId), "Expected the score to be 0");
    }
    
    // Covers: request by = playerTwo, challenge made = yes, valid guesses = yes
    @Test
    public void getChallengeScore() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "tools");
        match.tryWord(playerOneId, "4", "window");
        match.challengeWord(playerTwoId, "0", "table");
        assertEquals(3, match.getScore(playerTwoId), "Expected the score to be 3");
        assertEquals(1, match.getScore(playerOneId), "Expected the score to be 1");
    }
    
    // Covers: score changes = positive
    @Test
    public void testIncrementScore() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "tools");
        match.challengeWord(playerTwoId, "0", "table");
        assertEquals(3, match.getScore(playerTwoId), "Expected the score to be increased by two");
        assertEquals(0, match.getScore(playerOneId), "Expected the score to be 0");
    }
    
    // Covers: score changes = negative
    @Test
    public void testDecrementScore() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        match.challengeWord(playerTwoId, "0", "tools");
        assertEquals(-1, match.getScore(playerTwoId), "Expected the score to be decreased by 1");
        assertEquals(1, match.getScore(playerOneId), "Expected the score to be 0");
    }
    
    // Covers: tie = yes
    @Test
    public void testTie() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        assertEquals("The match ended in a tie.", match.getWinner(), "Expected a tie");
    }
    
    // Covers: tie = no, winner = playerOne
    @Test
    public void testOneWins() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        assertEquals("Final score: 1 - 0 bob won! jill lost.", match.getWinner(), "Expected bob to win");
    }
    
    // Covers: tie = no, winner = playerTwo
    @Test
    public void testTwoWins() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "tools");
        match.tryWord(playerOneId, "4", "window");
        match.challengeWord(playerTwoId, "0", "table");
        assertEquals("Final score: 1 - 3 jill won! bob lost.", match.getWinner(), "Expected jill to win");
    }
    
    // Covers: entryId exists = yes
    @Test
    public void testValidId() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        final String entryId = "1";
        assertEquals(true, match.entryExists(entryId), "Expected the entry to be in the Puzzle");
    }
    
    // Covers: entryId exists = no, entryId value < 0
    @Test
    public void testLowId() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        final String entryId = "-1";
        assertEquals(false, match.entryExists(entryId), "Expected the entry not to be in the Puzzle");
    }
    
    // Covers: entryId exists = no, entryId value > puzzle.getEntries().size()
    @Test
    public void testHighId() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        final String entryId = "7";
        assertEquals(false, match.entryExists(entryId), "Expected the entry not to be in the Puzzle");
    }
    
    // Covers: game over = no, overlap = no
    @Test
    public void testNotOver() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        assertEquals(false, match.isGameOver(), "Expected the game to be ongoing");  
    }
    
    // Covers: game over = yes, overlap = no
    @Test
    public void testGameOverNoOverlap() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/example.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "cat");
        match.tryWord(playerTwoId, "1", "mat");
        assertEquals(true, match.isGameOver(), "Expected the game to be over");  
    }
    
    // Covers: game over = yes, overlap = yes
    @Test
    public void testGameOverWithOverlap() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/overlap.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "score");
        match.tryWord(playerTwoId, "1", "mat");
        match.tryWord(playerOneId, "2", "store");
        assertEquals(true, match.isGameOver(), "Expected the game to be over");  
    }
    
    // Covers: length matches word = yes
    @Test
    public void testRightLength() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        assertEquals(true, match.correctLength("0", "store"), "That guess is the correct length");  
    }
    
    // Covers: length matches word = no
    public void testWrongLength() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        assertEquals(true, match.correctLength("0", "abc"), "That guess is the wrong length");  
    }
    
    // Covers: correct length = yes, has been guessed already = yes, guess is different = yes, word belongs to other player = yes
    @Test
    public void isValidChallenge() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        assertEquals(true, match.validChallenge(playerTwoId, "0", "tools"), "That is a valid challenge");  
    }
    
    // Covers: correct length = yes, has been guessed already = yes, guess is different = yes, word belongs to other player = no
    @Test
    public void challengeYourOwnWord() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        assertEquals(false, match.validChallenge(playerOneId, "0", "tools"), "That is not a valid challenge");  
    }
    
    // Covers: correct length = yes, has been guessed already = yes, guess is different = no, word belongs to other player = yes
    @Test
    public void challengeSameWord() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        assertEquals(false, match.validChallenge(playerTwoId, "0", "table"), "That is not a valid challenge");  
    }
    
    // Covers: correct length = yes, has been guessed already = no, guess is different = yes, word belongs to other player = yes
    @Test
    public void challengeEmptyEntry() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        assertEquals(false, match.validChallenge(playerTwoId, "0", "table"), "That is not a valid challenge");  
    }
    
    // Covers: correct length = no, has been guessed already = yes, guess is different = yes, word belongs to other player = yes
    @Test
    public void challengeWrongLength() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        assertEquals(false, match.validChallenge(playerTwoId, "0", "hat"), "That is not a valid challenge");  
    }

    // Covers: guess there by other player = no, intersects another word = no, intersects at same letter = no, intersected word 
    //         belongs to you = no
    @Test
    public void testValidGuess() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        assertEquals(true, match.validGuess(playerOneId, "0", "tools"), "That is a valid guess"); 
    }
    
    // Covers: guess there by other player = no, intersects another word = yes, intersects at same letter = no, intersected word 
    //         belongs to you = no
    @Test
    public void testRedoGuess() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        assertEquals(true, match.validGuess(playerOneId, "0", "tools"), "That is a valid guess"); 
    }
    
    // Covers: guess there by other player = yes, intersects another word = no, intersects at same letter = no, intersected word 
    //         belongs to you = no
    @Test
    public void testOccupiedGuess() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        assertEquals(false, match.validGuess(playerTwoId, "0", "tools"), "That is not a valid guess"); 
    }
    
    // Covers: guess there by other player = no, intersects another word = yes, intersects at same letter = no, intersected word 
    //         belongs to you = yes
    @Test
    public void testIntersectsYourWord() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        assertEquals(true, match.validGuess(playerOneId, "1", "cook"), "That is a valid guess"); 
    }
    
    // Covers: guess there by other player = no, intersects another word = yes, intersects at same letter = yes, intersected word 
    //         belongs to you = no
    @Test
    public void testIntersectsSameLetter() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        assertEquals(true, match.validGuess(playerOneId, "1", "book"), "That is a valid guess"); 
    }
    
    // Covers: guess there by other player = no, intersects another word = yes, intersects at same letter = yes, intersected word 
    //         belongs to you = no
    @Test
    public void testIntersectsWrongLetter() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        assertEquals(false, match.validGuess(playerTwoId, "1", "cook"), "That is not a valid guess"); 
    }
    
    // Covers: valid try = yes, word on board = yes, guess on your own word = no
    @Test
    public void testMakeValidGuess() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "tools");
        Match.MatchState result = new Match.MatchState(match);
        assertEquals("tools", result.guesses.get(0), "Expected the word tools to have been guessed"); 
        assertEquals(playerOneId, result.owner.get(0), "Expected the word to have been guessed by playerOne"); 
        assertEquals(false, result.confirmed.get(0), "Expected the word to be unconfirmed"); 
    }
    
    // Covers: valid try = yes, word on board = yes, guess on your own word = yes
    @Test
    public void testTryRedo() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "tools");
        match.tryWord(playerOneId, "0", "table");
        Match.MatchState result = new Match.MatchState(match);
        assertEquals("table", result.guesses.get(0), "Expected the word table to have been guessed"); 
        assertEquals(playerOneId, result.owner.get(0), "Expected the word to have been guessed by playerOne"); 
        assertEquals(false, result.confirmed.get(0), "Expected the word to be unconfirmed"); 
    }
    
    // Covers: valid try = no, word on board = no, guess on your own word = no
    @Test
    public void testMakeInvalidGuess() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "-1", "invalid");
        Match.MatchState result = new Match.MatchState(match);
        assertEquals(0, result.guesses.size(), "Expected no guesses"); 
        assertEquals(0, result.owner.size(), "Expected no guesses"); 
        assertEquals(0, result.confirmed.size(), "Expected no guesses"); 
    }
    
    // Covers: valid challenge = no
    @Test
    public void testInvalidChallenge() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        match.challengeWord(playerOneId, "0", "tools");
        Match.MatchState result = new Match.MatchState(match);
        assertEquals("table", result.guesses.get(0), "Expected the word table to have been guessed"); 
        assertEquals(playerOneId, result.owner.get(0), "Expected the word to have been guessed by playerOne"); 
        assertEquals(false, result.confirmed.get(0), "Expected the word to be unconfirmed"); 
        assertEquals(1, match.getScore(playerOneId), "Expected player one to have one point");
    }
    
    // Covers: valid challenge = yes, word originally correct = yes
    @Test
    public void testOriginallyCorrect() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        match.challengeWord(playerTwoId, "0", "tools");
        Match.MatchState result = new Match.MatchState(match);
        assertEquals("table", result.guesses.get(0), "Expected the word table to have been guessed"); 
        assertEquals(playerOneId, result.owner.get(0), "Expected the word to have been guessed by playerOne"); 
        assertEquals(true, result.confirmed.get(0), "Expected the word to be confirmed"); 
        assertEquals(1, match.getScore(playerOneId), "Expected player one to have one point");
        assertEquals(-1, match.getScore(playerTwoId), "Expected player two to have negative one point");
    }
    
    // Covers: valid challenge = yes, both originally wrong and now correct = yes
    @Test
    public void testCorrectNow() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "tools");
        match.challengeWord(playerTwoId, "0", "table");
        Match.MatchState result = new Match.MatchState(match);
        assertEquals("table", result.guesses.get(0), "Expected the word table to have been guessed"); 
        assertEquals(playerTwoId, result.owner.get(0), "Expected the word to have been guessed by playerOne"); 
        assertEquals(true, result.confirmed.get(0), "Expected the word to be confirmed"); 
        assertEquals(0, match.getScore(playerOneId), "Expected player one to have no points");
        assertEquals(3, match.getScore(playerTwoId), "Expected player two to have three points");
    }
    
    // Covers: valid challenge = yes, both word incorrect = yes
    @Test
    public void testBothIncorrect() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "tools");
        match.challengeWord(playerTwoId, "0", "stool");
        Match.MatchState result = new Match.MatchState(match);
        assertEquals(0, result.guesses.size(), "Expected no guesses"); 
        assertEquals(0, result.owner.size(), "Expected no guesses"); 
        assertEquals(0, result.confirmed.size(), "Expected no guesses"); 
        assertEquals(0, match.getScore(playerOneId), "Expected player one to have no points");
        assertEquals(-1, match.getScore(playerTwoId), "Expected player two to have negative one point");
    }
    
    // Covers: after calling = tryWord(), intersections on board = no, word direction = across, number of intersections = 0
    @Test
    public void testClearNoInconsistencies() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        match.tryWord(playerTwoId, "4", "window");
        Match.MatchState result = new Match.MatchState(match);
        assertEquals("table", result.guesses.get(0), "Expected the word table to have been guessed"); 
        assertEquals(playerOneId, result.owner.get(0), "Expected the word to have been guessed by playerOne"); 
        assertEquals(false, result.confirmed.get(0), "Expected the word to be confirmed"); 
        assertEquals("window", result.guesses.get(4), "Expected the word table to have been guessed"); 
        assertEquals(playerTwoId, result.owner.get(4), "Expected the word to have been guessed by playerOne"); 
        assertEquals(false, result.confirmed.get(4), "Expected the word to be unconfirmed"); 
        assertEquals(1, match.getScore(playerOneId), "Expected player one to have one point");
        assertEquals(1, match.getScore(playerTwoId), "Expected player two to have one point");
    }

    // Covers: after calling = tryWord(), intersections on board = yes, word direction = down, number of intersections = >0
    @Test
    public void testClearOneInconsistencies() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "0", "table");
        match.tryWord(playerOneId, "1", "cook");
        Match.MatchState result = new Match.MatchState(match);
        assertEquals(1, result.guesses.size(), "Expected one guess on the board"); 
        assertEquals(1, result.owner.size(), "Expected one guess on the board"); 
        assertEquals(1, result.confirmed.size(), "Expected one guess on the board"); 
        assertEquals("cook", result.guesses.get(1), "Expected the word cook to have been guessed"); 
        assertEquals(playerOneId, result.owner.get(1), "Expected the word to have been guessed by playerOne"); 
        assertEquals(false, result.confirmed.get(1), "Expected the word to be unconfirmed"); 
        assertEquals(0, match.getScore(playerOneId), "Expected player one to have no points");
        assertEquals(0, match.getScore(playerTwoId), "Expected player two to have no points");
    }
    
    // Covers: after calling = challengeWord(), intersections on board = yes, word direction = down, number of intersections = >0
    @Test
    public void testClearMultipleInconsistencies() throws UnableToParseException, IOException {
        final Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/complex.puzzle"));
        final String playerOneId = "bob";
        final String playerTwoId = "jill";
        Match match = new Match(puzzle, playerOneId);
        match.tryJoin(playerTwoId);
        match.tryWord(playerOneId, "4", "window");
        match.tryWord(playerOneId, "5", "sandwich");
        match.tryWord(playerOneId, "3", "aanadaa");
        match.challengeWord(playerTwoId, "3", "student");
        Match.MatchState result = new Match.MatchState(match);
        assertEquals(1, result.guesses.size(), "Expected one guess on the board"); 
        assertEquals(1, result.owner.size(), "Expected one guess on the board"); 
        assertEquals(1, result.confirmed.size(), "Expected one guess on the board"); 
        assertEquals("student", result.guesses.get(3), "Expected the word student to have been guessed"); 
        assertEquals(playerTwoId, result.owner.get(3), "Expected the word to have been guessed by playerOne"); 
        assertEquals(true, result.confirmed.get(3), "Expected the word to be unconfirmed"); 
        assertEquals(0, match.getScore(playerOneId), "Expected player one to have no points");
        assertEquals(3, match.getScore(playerTwoId), "Expected player two to have three points");
    }
   
}