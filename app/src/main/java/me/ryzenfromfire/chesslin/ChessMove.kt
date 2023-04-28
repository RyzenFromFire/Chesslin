package me.ryzenfromfire.chesslin

import me.ryzenfromfire.chesslin.ChessBoard.Position
import me.ryzenfromfire.chesslin.ChessBoard.File.*
import me.ryzenfromfire.chesslin.ChessPiece.Piece
import me.ryzenfromfire.chesslin.ChessPiece.Piece.*
import me.ryzenfromfire.chesslin.ChessGame.Player
import me.ryzenfromfire.chesslin.ChessGame.Player.*

class ChessMove(
    val game: ChessGame,
    val num: Int,
    val piece: ChessPiece,
    val start: Position,
    val end: Position,
    val capture: Piece = Piece.NONE,
    val promotion: Piece = Piece.NONE,
    val castle: Boolean = false,
    val check: Player = Player.NONE
) {
    var valid: Boolean = true
    private var notation: String = ""

    init {
        // If this is the case, then somehow the moves have gone out of sync
        if (num % 2 == 0 && piece.player != BLACK) {
            valid = false
        }

        if (!start.valid || !end.valid) valid = false

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
            if (
                start.rank == end.rank &&
                piece.piece == KING &&
                start.file == E &&
                ((start.rank == 1 && piece.player == WHITE) || (start.rank == 8 && piece.player == BLACK))
            ) {
                when (end.file) {
                    A -> notation = "${num}. O-O-O"
                    H -> notation = "${num}. O-O"
                    else -> valid = false
                }
            } else valid = false
        }

        // En Passant Logic
        if (piece.piece == PAWN && // If this piece is a pawn
            this.capture == PAWN && // If the piece captured is a pawn
            game.getLastMove().piece.piece != PAWN && // If the last piece moved was NOT a pawn
            game.getLastMove().end.rank != this.end.rank
        ) {
            // Then if the location where the last move ended
            // and the place where this move is capturing
            // are not the same, then the move is not valid.
            valid = false
        }
    }

    override fun toString(): String {
        if (!valid) return "ERROR"
        // Ex. White R on a1 to a4 with capture
        var str = "{${piece.player.str} ${piece.piece.str} on $start to $end"
        if (capture != Piece.NONE) str += " with capture"
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
        if (piece.piece != PAWN) {
            sb.append(piece.piece.str)
        }
        sb.append(start.toString())
        if (capture != Piece.NONE) sb.append("x")
        sb.append(end.toString())

        // Promotion Logic
        if (promotion != Piece.NONE) {
            if (promotion == KING || promotion == PAWN) valid = false
            else sb.append("=${promotion.str}")
        }

        if (check != Player.NONE) {
            sb.append("+")
        }

        return if (valid) sb.toString() else "ERROR"
    }
}