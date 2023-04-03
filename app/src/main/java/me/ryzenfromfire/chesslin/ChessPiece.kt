package me.ryzenfromfire.chesslin

import me.ryzenfromfire.chesslin.ChessGame.Player

class ChessPiece(val piece: Piece = Piece.NONE, val player: Player = Player.NONE) {
    enum class Piece(val str: String, val points: Int) {
        NONE("", 0),
        PAWN("P", 1),
        KNIGHT("N", 3),
        BISHOP("B", 3),
        ROOK("R", 5),
        QUEEN("Q", 9),
        KING("K", 0);

        companion object {
            private val pieceMap = Piece.values().associateBy { it.str }
            // Parses a piece as a string into a ChessPiece.Piece (ex. "p" -> PAWN)
            fun parse(piece: String) = pieceMap[piece.uppercase().take(1)]
        }
    }
}