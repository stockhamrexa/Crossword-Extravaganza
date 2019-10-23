package crossword;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for Puzzle equality
 *
 */
public class PuzzleTest {
    
    // Testing strategy:
    // For equals(), partition on:
    //  A single puzzle (check reflexivity)
    //  Two puzzles that differ in name
    //  Two puzzles that differ in description
    //  Two puzzles that differ in entries
    //  Two puzzles that differ in entry order
    // For isValidPuzzle(), partition on:
    //  A puzzle with non-overlapping entry(s)
    //  A puzzle with valid overlapping entries in opposite directions
    //  A puzzle with duplicate names
    //  A puzzle with invalid overlapping entries in opposite directions
    //  A puzzle with invalid overlapping entries in the same direction that share 2 letters
    //  A puzzle with invalid overlapping entries in the same direction
    
    private static Puzzle puzzle1 = new Puzzle("foo", "bar", List.of(new Entry("foo", "bar", Entry.ACROSS, 0, 0)));
    private static Puzzle puzzle2 = new Puzzle("bar", "bar", List.of(new Entry("foo", "bar", Entry.ACROSS, 0, 0)));
    private static Puzzle puzzle3 = new Puzzle("foo", "foo", List.of(new Entry("foo", "bar", Entry.ACROSS, 0, 0)));
    private static Puzzle puzzle4 = new Puzzle("foo", "bar", List.of(new Entry("foo", "bar", Entry.ACROSS, 0, 0), new Entry("bar", "baz", Entry.ACROSS, 1, 0)));
    private static Puzzle puzzle5 = new Puzzle("foo", "bar", List.of(new Entry("bar", "baz", Entry.ACROSS, 1, 0), new Entry("foo", "bar", Entry.ACROSS, 0, 0)));
    
    @Test
    public void testAssertionsEnabled() {
        assertThrows(AssertionError.class, () -> { assert false; }, "make sure assertions are enabled with VM argument '-ea'");
    }
    
    @Test
    //covers: A single puzzle (check reflexivity)
    public void testEqualsReflexive() {
        assertEquals(puzzle1, puzzle1, "expected puzzle to be equal to itself");
    }
    
    @Test
    //covers: Two puzzles that differ in name
    public void testEqualsName() {
        assertNotEquals(puzzle2, puzzle1, "expected differently named puzzles to be unequal");
    }
    
    @Test
    //covers: Two puzzles that differ in description
    public void testEqualsDescription() {
        assertNotEquals(puzzle3, puzzle1, "expected puzzles with different descriptions to be unequal");
    }
    
    @Test
    //covers: Two puzzles that differ in entries
    public void testEqualsEntries() {
        assertNotEquals(puzzle4, puzzle1, "expected puzzles with different entries to be unequal");
    }
    
    @Test
    //covers: Two puzzles that differ in entry order
    public void testEqualsEntryOrder() {
        assertNotEquals(puzzle4, puzzle5, "expected puzzles with different entries to be unequal");
    }
    
    @Test
    //covers: A puzzle with non-overlapping entries
    public void testValidNoOverlap() {
        assertTrue(puzzle4.isValidPuzzle());
    }
    
    @Test
    //covers: A puzzle with valid overlapping entries in opposite directions
    public void testValidOverlap() {
        Puzzle puzzle = new Puzzle("foo", "bar", List.of(new Entry("cat", "cat", Entry.ACROSS, 1, 0), new Entry("mat", "mat", Entry.DOWN, 0, 1)));
        assertTrue(puzzle.isValidPuzzle());
    }
    
    @Test
    //covers: A puzzle with invalid overlapping entries in the same direction that share 2 letters
    public void testInvalidOverlapShared() {
        Puzzle puzzle = new Puzzle("foo", "bar", List.of(new Entry("foo", "bar", Entry.ACROSS, 0, 0), new Entry("oof", "bar", Entry.ACROSS, 0, 1)));
        assertFalse(puzzle.isValidPuzzle());
    }
    
    @Test
    //covers: A puzzle with duplicate names
    public void testInvalidNames() {
        Puzzle puzzle = new Puzzle("foo", "bar", List.of(new Entry("foo", "bar", Entry.ACROSS, 1, 0), new Entry("foo", "bar", Entry.DOWN, 0, 1)));
        assertFalse(puzzle.isValidPuzzle());
    }
    
    @Test
    //covers: A puzzle with invalid overlapping entries in opposite directions
    public void testInvalidOverlap() {
        Puzzle puzzle = new Puzzle("foo", "bar", List.of(new Entry("foo", "bar", Entry.ACROSS, 1, 0), new Entry("bar", "foo", Entry.DOWN, 0, 1)));
        assertFalse(puzzle.isValidPuzzle());
    }
    
    @Test
    //covers: A puzzle with invalid overlapping entries in the same direction
    public void testInvalidOverlapSame() {
        Puzzle puzzle = new Puzzle("foo", "bar", List.of(new Entry("foo", "bar", Entry.ACROSS, 0, 0), new Entry("bar", "foo", Entry.ACROSS, 0, 1)));
        assertFalse(puzzle.isValidPuzzle());
    }
}
