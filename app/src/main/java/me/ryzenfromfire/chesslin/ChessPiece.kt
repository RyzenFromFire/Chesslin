package me.ryzenfromfire.chesslin

import me.ryzenfromfire.chesslin.ChessGame.Player
import me.ryzenfromfire.chesslin.ChessBoard.Position

class ChessPiece(var type: PieceType = PieceType.NONE, val player: Player = Player.NONE) {
    var hasMoved = false
        set(value) {
            hasJustMoved = !field && value
            field = value
        }
    var hasJustMoved = false

    enum class PieceType(val str: String, val fullStr: String, val points: Int) {
        NONE("", "", 0),
        PAWN("P", "Pawn", 1),
        KNIGHT("N", "Knight", 3),
        BISHOP("B", "Bishop", 3),
        ROOK("R", "Rook", 5),
        QUEEN("Q", "Queen", 9),
        KING("K", "King", 0);

        companion object {
            private val typeAbbrevMap = PieceType.values().associateBy { it.str }
            private val typeFullNameMap = PieceType.values().associateBy { it.fullStr }
            // Parses a piece as a string into a ChessPiece.Piece (ex. "p" -> PAWN)
            fun parseShort(abbreviation: String) = typeAbbrevMap[abbreviation.uppercase().take(1)] ?: NONE
            fun parseLong(fullName: String) = typeFullNameMap[fullName.lowercase().replaceFirstChar { it.uppercase() }] ?: NONE
        }
    }

    // TODO: Implement move checking
    // TODO: Can check if a move is blocked by checking game.board.get(position)
    /**
     * Returns a list of positions that can be potentially be reached by this piece,
     * without considering obstructions or limitations due to checks or pins.
     */
    fun getMovablePositions(game: ChessGame): MutableList<Position> {
        return when (this.type) {
            PieceType.PAWN -> getPawnPositions(game)
            PieceType.ROOK -> getRookPositions(game)
            PieceType.BISHOP -> getBishopPositions(game)
            PieceType.KNIGHT -> getKnightPositions(game)
            PieceType.QUEEN -> getQueenPositions(game)
            PieceType.KING -> getKingPositions(game)
            else -> mutableListOf<Position>()
        }
    }

    /**
     * Promotes the piece to a knight, bishop, rook, or queen by changing `this.type`
     * Returns true only if the passed `type` was a valid promotion piece.
     */
    fun promote(type: PieceType): Boolean {
        this.type = when (type) {
            PieceType.ROOK -> type
            PieceType.BISHOP -> type
            PieceType.KNIGHT -> type
            PieceType.QUEEN -> type
            else -> return false
        }
        return true
    }

    // Global/static methods for checking positions given a game state
    companion object {
        val NULL = ChessPiece()
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


