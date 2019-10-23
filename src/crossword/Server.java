package crossword;

import java.io.File;
import java.io.IOException;

/**
 * Game server runner
 */
public class Server {
    
    /**
     * Start a Crossword Extravaganza server. The command line argument should be in the format "java -cp <path> crossword.Server
     * <puzzle_folder>" where path is the location of all of your class files and puzzle_folder is the name of your folder
     * containing puzzles.
     * 
     * @param args The command line arguments should include only the folder where the puzzles are located. Must be a valid 
     * path to a folder.
     * @throws IOException If an error occurs parsing a file or starting the server
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("No puzzle folder provided");
        }
        File folder = new File(args[0]);
        if(!folder.exists()){
            throw new IllegalArgumentException("Incorrect path");
        }
        new CrosswordServer(folder).run();
    }
    
}