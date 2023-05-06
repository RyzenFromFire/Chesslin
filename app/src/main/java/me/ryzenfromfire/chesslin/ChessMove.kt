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
    var check: Player = Player.NONE // the player who is checked as a result of the move
    var valid: Boolean = true
    val legal: Boolean
        get() {
            if (game == null) return false
            return (this.end in piece.getMovablePositions(game, start))
        }
    private var notation: String = ""

    init {
        if (game == null) valid = false else {
            if (!start.valid || !end.valid) valid = false

            // Check if moves have gone out of sync (unless it is a testing move)
            if (num % 2 == 0 && piece.player != BLACK && num != NUM_TEST_MOVE) {
                valid = false
            }

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
                if (game.castleValid(piece = piece, start = start, end = end)) {
                    when (end.file) {
                        C -> notation = "${num}. O-O-O"
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
        const val NUM_TEST_MOVE = -2
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