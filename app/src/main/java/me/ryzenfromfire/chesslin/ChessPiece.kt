package me.ryzenfromfire.chesslin

import me.ryzenfromfire.chesslin.ChessGame.Player
import me.ryzenfromfire.chesslin.ChessBoard.Position

class ChessPiece(val piece: Piece = Piece.NONE, val player: Player = Player.NONE) {
    enum class Piece(val str: String, val fullStr: String, val points: Int) {
        NONE("", "", 0),
        PAWN("P", "Pawn", 1),
        KNIGHT("N", "Knight", 3),
        BISHOP("B", "Bishop", 3),
        ROOK("R", "Rook", 5),
        QUEEN("Q", "Queen", 9),
        KING("K", "King", 0);

        companion object {
            private val pieceAbbrevMap = Piece.values().associateBy { it.str }
            private val pieceFullNameMap = Piece.values().associateBy { it.fullStr }
            // Parses a piece as a string into a ChessPiece.Piece (ex. "p" -> PAWN)
            fun parseShort(abbreviation: String) = pieceAbbrevMap[abbreviation.uppercase().take(1)] ?: NONE
            fun parseLong(fullName: String) = pieceFullNameMap[fullName.lowercase().replaceFirstChar { it.uppercase() }] ?: NONE
        }
    }

    // TODO: Implement move checking
    // TODO: Can check if a move is blocked by checking game.board.get(position)

    fun getMovablePositions(game: ChessGame): MutableList<Position> {
        return when (this.piece) {
            Piece.PAWN -> getPawnPositions(game)
            Piece.ROOK -> getRookPositions(game)
            Piece.BISHOP -> getBishopPositions(game)
            Piece.KNIGHT -> getKnightPositions(game)
            Piece.QUEEN -> getQueenPositions(game)
            Piece.KING -> getKingPositions(game)
            else -> mutableListOf<Position>()
        }
    }

    // Global/static methods for checking positions given a game state
    private companion object {
        private fun getPawnPositions(game: ChessGame) : MutableList<Position> {
            // TODO: Can check if en passant is valid by creating a prototype ChessMove to each side and checking if it is valid
            return mutableListOf<Position>() // TODO: Implement
        }

        private fun getRookPositions(game: ChessGame) : MutableList<Position> {
            return mutableListOf<Position>() // TODO: Implement
        }

        private fun getBishopPositions(game: ChessGame) : MutableList<Position> {
            return mutableListOf<Position>() // TODO: Implement
        }

        private fun getKnightPositions(game: ChessGame) : MutableList<Position> {
            return mutableListOf<Position>() // TODO: Implement
        }

        private fun getKingPositions(game: ChessGame) : MutableList<Position> {
            return mutableListOf<Position>() // TODO: Implement
        }

        private fun getQueenPositions(game: ChessGame) : MutableList<Position> {
            val positions = getRookPositions(game)
            positions.addAll(getBishopPositions(game))
            return positions
        }
    }
}


