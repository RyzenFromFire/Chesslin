package me.ryzenfromfire.chesslin

import me.ryzenfromfire.chesslin.ChessBoard.Position
import me.ryzenfromfire.chesslin.ChessBoard.File.*
import me.ryzenfromfire.chesslin.ChessPiece.PieceType
import me.ryzenfromfire.chesslin.ChessPiece.PieceType.*
import me.ryzenfromfire.chesslin.ChessGame.Player
import me.ryzenfromfire.chesslin.ChessGame.Player.*

class ChessMove(
    val game: ChessGame?,
    val num: Int,
    val piece: ChessPiece,
    val start: Position,
    val end: Position,
    val capture: ChessPiece = ChessPiece(),
    val promotion: PieceType = PieceType.NONE,
    val castle: Boolean = false
) {
    val check: Player = Player.NONE // the player who is checked as a result of the move
    var valid: Boolean = true
    private var notation: String = ""

    init {
        if (game == null) valid = false else {
            if (!start.valid || !end.valid) valid = false

            // Check if moves have gone out of sync (unless it is a testing move)
            if (num % 2 == 0 && piece.player != BLACK && num != NUM_TEST_MOVE) {
                valid = false
            }

            // TODO: Implement checking for if the player whose turn it is is in check, and limit moves appropriately
            // TODO: probably implement another checking function in the companion object to do this

            // TODO: look at the game state to determine if the move leads to a check, and update game.inCheck appropriately

            // Game End Notation
            if (game.gameOver) {
                notation = when (game.winner) {
                    WHITE -> "1-0"
                    BLACK -> "0-1"
                    Player.NONE -> "½-½"
                }
            }

            // Castling Logic and Notation
            if (castle) {
                if (castleValid(piece = piece, start = start, end = end)) {
                    when (end.file) {
                        B -> notation = "${num}. O-O-O"
                        G -> notation = "${num}. O-O"
                        else -> valid = false // should be unreachable
                    }
                } else valid = false
            }

            // TODO: Invalidate move if it is not possible for the piece
        }
    }

    companion object {
        val NULL = ChessMove(null, -1, ChessPiece.NULL, Position.NULL, Position.NULL)
        val NUM_TEST_MOVE = -2

        // The following functions consider multiple pieces in the context of a ChessMove, so they are not in the ChessPiece class

        /**
         * Determines if a castling move is valid given the start and end positions
         * alongside a reference to the piece (assumed to be a king)
         */
        fun castleValid(piece: ChessPiece, start: Position, end: Position): Boolean {
            return (
                start.rank == end.rank &&
                piece.type == KING &&
                !piece.hasMoved &&
                start.file == E &&
                (end.file == B || end.file == G) &&
                ((start.rank == 1 && piece.player == WHITE) || (start.rank == 8 && piece.player == BLACK))
            )
        }

        /**
         * Given a Position (at which it is assumed there is a pawn)
         * and the game (from which the last move is retrieved),
         * return the target Position if en passant is possible.
         */
        fun getEnPassantTarget(game: ChessGame, pos: Position): Position {
            // Return null if pieces are not pawns or last move is invalid
            if (game.board.get(pos).type != PAWN ||
                !game.lastMove.valid ||
                game.lastMove.piece.type != PAWN
            ) return Position.NULL

            // Return appropriate position based on if the last move ended to the left or right
            val left = game.board.getRelativePosition(pos, -1, 0)
            val right = game.board.getRelativePosition(pos, 1, 0)
            return when (game.lastMove.end) {
                left -> game.board.getRelativePosition(pos, -1, 1)
                right -> game.board.getRelativePosition(pos, 1, 1)
                else -> Position.NULL
            }
        }
    }

    override fun toString(): String {
        if (!valid) return "ERROR"
        // Ex. White R on a1 to a4 with capture
        var str = "{${piece.player.str} ${piece.type.str} on $start to $end"
        if (capture != ChessPiece.NULL) str += " with capture"
        return str
    }

    // Long Algebraic Notation
    fun getLongNotation(): String {
        if (notation != "") {
            return notation
        }
        val sb = StringBuilder()
        sb.append("${num}. ")

        // Standard Move Logic
        if (piece.type != PAWN) {
            sb.append(piece.type.str)
        }
        sb.append(start.toString())
        if (capture != ChessPiece.NULL) sb.append("x")
        sb.append(end.toString())

        // Promotion Logic
        if (promotion != PieceType.NONE) {
            if (promotion == KING || promotion == PAWN) valid = false
            else sb.append("=${promotion.str}")
        }

        if (check != Player.NONE) {
            sb.append("+")
        }

        return if (valid) sb.toString() else "ERROR"
    }
}