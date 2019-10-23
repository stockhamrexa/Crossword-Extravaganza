package crossword;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import crossword.Entry.WordDirection;

/**
 * Tests for the Entry class
 */
public class EntryTest {
    
    /**
     * Testing strategy for Entry
     * 
     * For constructor, partition on:
     *  - Valid word = yes, no
     *  - Valid row = yes, no
     *  - Valid column = yes, no
     *  - Newline in clue = yes, no
     * 
     * For prepareForTransfer(), partition on:
     *  - Word length = 1, >1
     * 
     * For toString(), partition on: 
     *  - Direction = across, down
     * 
     */    
    
    @Test
    public void testAssertionsEnabled() {
        assertThrows(AssertionError.class, () -> { assert false; }, "make sure assertions are enabled with VM argument '-ea'");
    }
    
    // Covers: valid word = yes, valid row = yes, valid column = yes, newline in clue = no
    @Test
    public void testValidEntry() {
        Entry entry = new Entry("star", "twinkle twinkle", WordDirection.ACROSS, 1, 0);
        assertEquals("star", entry.getWord(), "Expected the word to be star");
        assertEquals("twinkle twinkle", entry.getClue(), "Expected a different clue");
        assertEquals(WordDirection.ACROSS, entry.getDirection(), "Expected the word to be across");
        assertEquals(1, entry.getRow(), "Expected a different row");
        assertEquals(0, entry.getColumn(), "Expected a different column");
    }
    
    // Covers: valid word = no, valid row = yes, valid column = yes, newline in clue = no
    @Test
    public void testWrongWord() {
        assertThrows(AssertionError.class, () -> {new Entry("star123", "twinkle twinkle", WordDirection.ACROSS, -4, 0);}, "No numbers allowed in word");
    }
    
    // Covers: valid word = yes, valid row = yes, valid column = yes, newline in clue = yes
    @Test
    public void testNewlines() {
        Entry entry = new Entry("star", "twinkle twinkle\\n", WordDirection.ACROSS, 1, 0);
        assertEquals("star", entry.getWord(), "Expected the word to be star");
        assertEquals("twinkle twinkle\n", entry.getClue(), "Expected the clue to have character codes reverted");
        assertEquals(WordDirection.ACROSS, entry.getDirection(), "Expected the word to be across");
        assertEquals(1, entry.getRow(), "Expected a different row");
        assertEquals(0, entry.getColumn(), "Expected a different column"); 
    }
    
    // Covers: valid word = yes, valid row = no, valid column = yes, newline in clue = no
    @Test
    public void testWrongRow() {
        assertThrows(AssertionError.class, () -> {new Entry("star", "twinkle twinkle", WordDirection.ACROSS, -4, 0);}, "Rows must be >= 0");
    }
    
    // Covers: valid word = yes, valid row = yes, valid column = no, newline in clue = no
    @Test
    public void testWrongColumn() {
        assertThrows(AssertionError.class, () -> {new Entry("star", "twinkle twinkle", WordDirection.ACROSS, 1, -1);}, "Columns must be >= 0");
    }
    
    // Covers: word length = 1
    @Test
    public void testShortTransfer() {
        Entry entry = new Entry("a", "first letter in the alphabet", WordDirection.ACROSS, 1, 0);
        Entry transfer = Entry.prepareForTransfer(entry);
        assertEquals("-", transfer.getWord(), "Expected the word to be hidden");
        assertEquals("first letter in the alphabet", transfer.getClue(), "Expected a different clue");
        assertEquals(WordDirection.ACROSS, transfer.getDirection(), "Expected the word to be across");
        assertEquals(1, transfer.getRow(), "Expected a different row");
        assertEquals(0, transfer.getColumn(), "Expected a different column");
    }
    
    // Covers: word length > 1
    @Test
    public void testLongTransfer() {
        Entry entry = new Entry("star", "twinkle twinkle", WordDirection.ACROSS, 1, 0);
        Entry transfer = Entry.prepareForTransfer(entry);
        assertEquals("----", transfer.getWord(), "Expected the word to be hidden");
        assertEquals("twinkle twinkle", transfer.getClue(), "Expected a different clue");
        assertEquals(WordDirection.ACROSS, transfer.getDirection(), "Expected the word to be across");
        assertEquals(1, transfer.getRow(), "Expected a different row");
        assertEquals(0, transfer.getColumn(), "Expected a different column");
    }
    
    // Covers: direction = across
    @Test
    public void testAcrossString() {
        Entry entry = new Entry("star", "twinkle twinkle", WordDirection.ACROSS, 1, 0);
        final String expected = "(star, twinkle twinkle, ACROSS, 1, 0)";
        assertEquals(expected, entry.toString(), "Expected a different string output");
    }
    
    // Covers: direction = down
    @Test
    public void testDownString() {
        Entry entry = new Entry("star", "twinkle twinkle", WordDirection.DOWN, 1, 0);
        final String expected = "(star, twinkle twinkle, DOWN, 1, 0)";
        assertEquals(expected, entry.toString(), "Expected a different string output");
    }
    
}