package me.ryzenfromfire.chesslin

import me.ryzenfromfire.chesslin.ChessBoard.Position
import me.ryzenfromfire.chesslin.ChessBoard.File.*
import me.ryzenfromfire.chesslin.ChessPiece.PieceType
import me.ryzenfromfire.chesslin.ChessPiece.PieceType.*
import me.ryzenfromfire.chesslin.ChessGame.Player
import me.ryzenfromfire.chesslin.ChessGame.Player.*

class ChessMove(
    val game: ChessGame,
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
        if (!start.valid || !end.valid) valid = false

        // Check if moves have gone out of sync
        if (num % 2 == 0 && piece.player != BLACK) {
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

        // En Passant Logic
        if (piece.type == PAWN && // If this piece is a pawn
            this.capture.type == PAWN && // If the piece captured is a pawn
            game.lastMove.piece.type != PAWN && // If the last piece moved was NOT a pawn
            game.lastMove.end.rank != this.end.rank
        ) {
            // Then if the location where the last move ended
            // and the place where this move is capturing
            // are not the same, then the move is not valid.
            valid = false
        }
    }

    companion object {
        fun castleValid(piece: ChessPiece, start: Position, end: Position): Boolean {
            return (
                start.rank == end.rank &&
                piece.type == KING &&
                start.file == E &&
                (end.file == B || end.file == G) &&
                ((start.rank == 1 && piece.player == WHITE) || (start.rank == 8 && piece.player == BLACK))
            )
        }

        fun getEnPassantPosition(piece: ChessPiece, pos: Position, lastMove: ChessMove): Position {
            val file = pos.file
            val rank = pos.rank

            if (!lastMove.valid || lastMove.piece.type != PAWN) return Position.NULL

            // Check left side
            if ((piece.player == WHITE && file != A) || (piece.player == BLACK && file != H)) {

            }

            // Check right side
            if ((piece.player == BLACK && file != A) || (piece.player == WHITE && file != H)) {

            }

            return Position.NULL
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