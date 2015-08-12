package game2048;

import ucb.util.CommandArgs;

import game2048.gui.Game;
import static game2048.Main.Side.*;

/** The main class for the 2048 game.
 *  @author Ryan Riddle
 */
public class Main {

    /** Size of the board: number of rows and of columns. */
    static final int SIZE = 4;
    /** Number of squares on the board. */
    static final int SQUARES = SIZE * SIZE;

    /** Goal tile number, game ends when goal is reached. */
    static final int GOAL = 2048;

    /** Symbolic names for the four sides of a board. */
    static enum Side { NORTH, EAST, SOUTH, WEST };

    /** The main program.  ARGS may contain the options --seed=NUM,
     *  (random seed); --log (record moves and random tiles
     *  selected.); --testing (take random tiles and moves from
     *  standard input); and --no-display. */
    public static void main(String... args) {
        CommandArgs options =
            new CommandArgs("--seed=(\\d+) --log --testing --no-display",
                            args);
        if (!options.ok()) {
            System.err.println("Usage: java game2048.Main [ --seed=NUM ] "
                               + "[ --log ] [ --testing ] [ --no-display ]");
            System.exit(1);
        }

        Main game = new Main(options);

        while (game.play()) {
            /* No action */
        }
        System.exit(0);
    }

    /** A new Main object using OPTIONS as options (as for main). */
    Main(CommandArgs options) {
        boolean log = options.contains("--log"),
            display = !options.contains("--no-display");
        long seed = !options.contains("--seed") ? 0 : options.getLong("--seed");
        _testing = options.contains("--testing");
        _game = new Game("2048", SIZE, seed, log, display, _testing);
    }

    /** Reset the score for the current game to 0 and clear the board. */
    void clear() {
        _score = 0;
        _count = 0;
        _game.clear();
        _game.setScore(_score, _maxScore);
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                _board[r][c] = 0;
            }
        }
    }

    /** Play one game of 2048, updating the maximum score. Return true
     *  iff play should continue with another game, or false to exit. */
    boolean play() {
        setRandomPiece();
        while (true) {
            _game.setScore(_score, _maxScore);
            _game.displayMoves();
            setRandomPiece();
            if (gameOver()) {
                if (_maxScore < _score) {
                    _maxScore = _score;
                    _game.setScore(_score, _maxScore);
                }
                _game.endGame();
            }

        GetMove:
            while (true) {
                String key = _game.readKey();
                switch (key) {
                case "Up": case "Down": case "Left": case "Right":
                    if (!gameOver() && tiltBoard(keyToSide(key))) {
                        break GetMove;
                    }
                    break;
                case "New Game":
                    if (_maxScore < _score) {
                        _maxScore = _score;
                        _game.setScore(_score, _maxScore);
                    }
                    clear();
                    play();
                    return true;
                case "Quit":
                    System.exit(0);
                    return false;
                default:
                    break;
                }
            }
        }
    }

    /** Return true iff the current game is over (no more moves
     *  possible). */
    boolean gameOver() {
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                int t = _board[r][c];
                if (t == 0 || (c > 0 && t == _board[r][c - 1])
                    || (r > 0 && t == _board[r - 1][c])) {
                    return false;
                }
            }
        } return true;
    }

    /** Add a tile to a random, empty position, choosing a value (2 or
     *  4) at random.  Has no effect if the board is currently full. */
    void setRandomPiece() {
        if (_count == SQUARES) {
            return;
        }
        while (true) {
            int[] temp = _game.getRandomTile();
            if (_board[temp[1]][temp[2]] == 0) {
                _game.addTile(temp[0], temp[1], temp[2]);
                _board[temp[1]][temp[2]] = temp[0];
                return;
            }
        }
    }

    /** Perform the result of tilting the board toward SIDE.
     *  Returns true iff the tilt changes the board. **/
    boolean tiltBoard(Side side) {
        /* As a suggestion (see the project text), you might try copying
         * the board to a local array, turning it so that edge SIDE faces
         * north.  That way, you can re-use the same logic for all
         * directions.  (As usual, you don't have to). */
        int[][] board = new int[SIZE][SIZE];
        boolean returnvalue = false;
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                board[r][c] =
                    _board[tiltRow(side, r, c)][tiltCol(side, r, c)];
            }
        }
        for (int c = 0; c < SIZE; c += 1) {
            int merged = 0;
            for (int r = 0; r < SIZE; r += 1) {
                if (board[r][c] != 0) {
                    int[] temp = wheretomove(board, r, c,
                        board[r][c], merged);
                    if (temp[1] == -1 && temp[0] != r) {
                        _game.moveTile(board[r][c],
                            tiltRow(side, r, c), tiltCol(side, r, c),
                            tiltRow(side, temp[0], c),
                            tiltCol(side, temp[0], c));
                        board[temp[0]][c] = board[r][c];
                        board[r][c] = 0;
                        returnvalue = true;
                    } else if (temp[1] == -2) {
                        int oldvalue = board[r][c];
                        int newvalue = oldvalue * 2;
                        merged = temp[0] + 1;
                        _score += newvalue;
                        _game.setScore(_score, _maxScore);
                        _game.mergeTile(oldvalue, newvalue,
                            tiltRow(side, r, c), tiltCol(side, r, c),
                            tiltRow(side, temp[0], c),
                            tiltCol(side, temp[0], c));
                        board[temp[0]][c] = newvalue;
                        board[r][c] = 0;
                        returnvalue = true;
                        if (newvalue == GOAL) {
                            if (_maxScore < _score) {
                                _maxScore = _score;
                                _score = 0;
                                _game.setScore(_score, _maxScore);
                            }
                            _game.endGame();
                        }
                    }
                }
            }
        }
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                _board[tiltRow(side, r, c)][tiltCol(side, r, c)]
                    = board[r][c];
            }
        }
        return returnvalue;
    }

    /** Sorry, I know this is ugly and unnecessary, but I couldn't
     * get around it. This function takes in a tilted board, a row,
     * a column, the value of the board at that row and column, and
     * a stop integer that prevents the board from merging past that
     * point. With these inputs the function finds if the tile at row
     * column should be merged or moved, and what row it should be
     * merged or moved to. It returns an intlist where the first int,
     * rv[0] is the row that the tile should be merged/moved to and the
     * second int, rv[1] says if the tile ar r should be moved or merged
     * with the row of rv[0]. Moved is represented by -1 and merge is
     * represented by -2.
     * @param board is the tilted board that it looks at
     * @param r is the row that the method is looking at on the board
     * @param c is the column that the methis is looking at
     * @param rcValue is the value of the board at row r and column c
     * @param stop is the maximum row that it will merge up to */
    int[] wheretomove(int[][] board, int r, int c, int rcValue, int stop) {
        int[] rv = new int[2];
        if (r - 1 < stop) {
            rv[0] = r;
            rv[1] = -1;
            return rv;
        }
        if (board[r - 1][c] == rcValue) {
            rv[0] = r - 1;
            rv[1] = -2;
            return rv;
        }
        if (board[r - 1][c] != 0) {
            rv[0] = r;
            rv[1] = -1;
            return rv;
        } else {
            return wheretomove(board, r - 1, c, rcValue, stop);
        }
    }


    /** Return the row number on a playing board that corresponds to row R
     *  and column C of a board turned so that row 0 is in direction SIDE (as
     *  specified by the definitions of NORTH, EAST, etc.).  So, if SIDE
     *  is NORTH, then tiltRow simply returns R (since in that case, the
     *  board is not turned).  If SIDE is WEST, then column 0 of the tilted
     *  board corresponds to row SIZE - 1 of the untilted board, and
     *  tiltRow returns SIZE - 1 - C. */
    int tiltRow(Side side, int r, int c) {
        switch (side) {
        case NORTH:
            return r;
        case EAST:
            return c;
        case SOUTH:
            return SIZE - 1 - r;
        case WEST:
            return SIZE - 1 - c;
        default:
            throw new IllegalArgumentException("Unknown direction");
        }
    }

    /** Return the column number on a playing board that corresponds to row
     *  R and column C of a board turned so that row 0 is in direction SIDE
     *  (as specified by the definitions of NORTH, EAST, etc.). So, if SIDE
     *  is NORTH, then tiltCol simply returns C (since in that case, the
     *  board is not turned).  If SIDE is WEST, then row 0 of the tilted
     *  board corresponds to column 0 of the untilted board, and tiltCol
     *  returns R. */
    int tiltCol(Side side, int r, int c) {
        switch (side) {
        case NORTH:
            return c;
        case EAST:
            return SIZE - 1 - r;
        case SOUTH:
            return SIZE - 1 - c;
        case WEST:
            return r;
        default:
            throw new IllegalArgumentException("Unknown direction");
        }
    }

    /** Return the side indicated by KEY ("Up", "Down", "Left",
     *  or "Right"). */
    Side keyToSide(String key) {
        switch (key) {
        case "Up":
            return NORTH;
        case "Down":
            return SOUTH;
        case "Left":
            return WEST;
        case "Right":
            return EAST;
        default:
            throw new IllegalArgumentException("unknown key designation");
        }
    }

    /** Represents the board: _board[r][c] is the tile value at row R,
     *  column C, or 0 if there is no tile there. */
    private final int[][] _board = new int[SIZE][SIZE];

    /** True iff --testing option selected. */
    private boolean _testing;
    /** THe current input source and output sink. */
    private Game _game;
    /** The score of the current game, and the maximum final score
     *  over all games in this session. */
    private int _score, _maxScore;
    /** Number of tiles on the board. */
    private int _count;
}
