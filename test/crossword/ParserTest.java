package crossword;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.mit.eecs.parserlib.Parser;
import edu.mit.eecs.parserlib.UnableToParseException;

/**
 * Tests for the PuzzleParser
 */
public class ParserTest {
    
    /**
     * Testing strategy for PuzzleParser
     * 
     * For parse(), partition on:
     *  - The example puzzle in the handout
     *  - Valid description = yes, no
     *  - Newline characters in description = yes, no
     *  - Newline characters in entries
     *  - Valid rows/ columns = yes, no
     *  - Puzzle is consistent = yes, no
     */    
    
    private static String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }
    
    @Test
    public void testAssertionsEnabled() {
        assertThrows(AssertionError.class, () -> { assert false; }, "make sure assertions are enabled with VM argument '-ea'");
    }
    
    // Covers: ParserLib is version 3.1.x
    @Test
    public void testParserLibVersion() {
        assertThat("parserlib.jar needs to be version 3.1.x", Parser.VERSION, startsWith("3.1"));
    }
    
    // Covers: The example puzzle in the handout, valid description = yes, newline characters in description = no, newline 
    //         characters in entries = no, valid rows/ columns = yes, puzzle is consistent = yes
    @Test
    public void testExample() throws UnableToParseException, IOException {
        Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/example.puzzle"));
        assertEquals("Simple Puzzle", puzzle.getName());
        assertEquals("A trivial puzzle designed to show how puzzles work", puzzle.getDescription());
        final List<Entry> expected = new ArrayList<Entry>();
        expected.add(new Entry("cat", "feline companion", Entry.DOWN, 0, 1));
        expected.add(new Entry("mat", "lounging place for feline companion", Entry.ACROSS, 1, 0));
        assertEquals(expected, puzzle.getEntries());
    }
    
    // Covers: valid description = yes, newline characters in description = no, newline characters in entries = no, valid 
    //         rows/ columns = yes, puzzle is consistent = no
    @Test
    public void testInvalid() throws UnableToParseException, IOException {
        assertThrows(UnableToParseException.class, () -> {PuzzleParser.parse(readFile("puzzles/invalid.puzzle"));});
    }
    
    // Covers: valid description = no, newline characters in description = yes, newline characters in entries = no, valid 
    //         rows/ columns = yes, puzzle is consistent = yes
    @Test
    public void testPattern() throws UnableToParseException, IOException {
        Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/desc_patterns.puzzle"));
        assertEquals("foo\n\r\t\\bar", puzzle.getDescription());
    }
    
    // Covers: valid description = yes, newline characters in description = no, newline characters in entries = yes, valid 
    //         rows/ columns = yes, puzzle is consistent = yes
    @Test
    public void testEntryPattern() throws UnableToParseException, IOException {
        Puzzle puzzle = PuzzleParser.parse(readFile("puzzles/entry_patterns.puzzle"));
        assertEquals("foo\n\r\t\\bar", puzzle.getEntry(0).getClue());
    }
    
    // Covers: valid description = yes, newline characters in description = no, newline characters in entries = no, valid 
    //         rows/ columns = no, puzzle is consistent = yes
    @Test
    public void testInvalidRows() throws UnableToParseException, IOException {
        assertThrows(UnableToParseException.class, () -> {PuzzleParser.parse(readFile("puzzles/wrong_rows.puzzle"));});
    }
    
    // Covers: valid description = yes, newline characters in description = no, newline characters in entries = no, valid 
    //         rows/ columns = no, puzzle is consistent = yes
    @Test
    public void testInvalidColumns() throws UnableToParseException, IOException {
        assertThrows(UnableToParseException.class, () -> {PuzzleParser.parse(readFile("puzzles/wrong_columns.puzzle"));});
    }
    
}