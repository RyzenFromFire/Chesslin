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

    override fun toString(): String {
        return StringBuilder().append(player.str).append(" ").append(type.fullStr).toString()
    }

    enum class PieceType(val str: String, val fullStr: String, val points: Int) {
        NONE("_", "", 0),
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

    // TODO: Implement move checking (in progress)
    // TODO: Can check if a move is blocked by checking game.board.get(position)
    /**
     * Returns a list of positions that can be legally be reached by the piece at the specified position.
     * @param checkLegality: default true, set false to ignore legality of moves considered, and only consider if the move is possible.
     */
    fun getMovablePositions(game: ChessGame, start: Position, checkLegality: Boolean = true): MutableList<Position> {
        return when (this.type) {
            PieceType.PAWN -> getPawnPositions(game, start)
            PieceType.ROOK -> getRookPositions(game, start)
            PieceType.BISHOP -> getBishopPositions(game, start)
            PieceType.KNIGHT -> getKnightPositions(game, start)
            PieceType.QUEEN -> getQueenPositions(game, start)
            PieceType.KING -> getKingPositions(game, start)
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

    // TODO: Consider refactoring PositionList and all the lists to MutableSet
    class PositionList(private val game: ChessGame, val startingPosition: Position) : ArrayList<Position>() {
        /**
         * Generates a simple move from this class instance's game, starting position, and the specified ending position.
         */
        private fun getMove(endPos: Position): ChessMove {
            return ChessMove(
                game = game,
                num = ChessMove.NUM_TEST_MOVE,
                piece = game.board.get(startingPosition),
                start = startingPosition,
                end = endPos,
                capture = if (game.board.get(endPos) != ChessPiece.NULL) game.board.get(endPos) else ChessPiece.NULL,
                promotion = PieceType.NONE,
                castle = false
            )
        }
        fun addIfLegal(rightOffset: Int, upOffset: Int, checkLegality: Boolean = true) {
            val pos = game.board.getRelativePosition(startingPosition, rightOffset, upOffset)
            addIfLegal(pos, checkLegality)
        }

        fun addIfLegal(pos: Position, checkLegality: Boolean = true) {
            val piece = game.board.get(startingPosition)
            if (pos.valid) {
                if (!checkLegality) {
                    this.add(pos)
                } else if (
                    (!game.board.isOccupied(pos) || game.board.get(pos).player == piece.player.opponent()) &&
                    !game.isCheckedAfterMove(piece.player, getMove(pos))
                ) {
                    this.add(pos)
                }
            }
        }
    }

    // Global/static methods for checking positions for the next move of a piece, given a game state
    companion object {
        val NULL = ChessPiece()

        /**
         * Determine the direction which a piece (realistically, a pawn)
         * of a given color will move based on the specified player (color).
         * @return a scalar (+1 or -1) for white or black respectively.
         * Avoid usage with Player.NONE (returns 0) - this could yield unexpected results.
         */
        fun getRankDirection(player: Player): Int {
            return when (player) {
                Player.WHITE -> +1
                Player.BLACK -> -1
                else -> 0
            }
        }

        /**
         * Generates a list of possible pawn moves from the specified position based on the current game state.
         * Pawns can move forward one square, forward two squares on first move, or diagonally if capturing (including en passant).
         * @param checkLegality: default true, set false to ignore legality of moves considered.
         */
        fun getPawnPositions(game: ChessGame, position: Position, checkLegality: Boolean = true) : MutableList<Position> {
            val positions = PositionList(game, position)
            val player = game.board.get(position).player

            // Forward one square
            positions.addIfLegal(0, getRankDirection(player), checkLegality)

            // Add initial 2-square move if the pawn has not yet moved
            if (!game.board.get(position).hasMoved)
                positions.addIfLegal(0, 2 * getRankDirection(player), checkLegality)

            // Add capturing positions if there is an opposing piece to the left or right (forward and diagonally)
            val tempList = listOf(
                game.board.getRelativePosition(position, +1, getRankDirection(player)),
                game.board.getRelativePosition(position, -1, getRankDirection(player))
            )
            for (pos in tempList) {
                if (game.board.get(pos).player == game.board.get(position).player.opponent()) {
                    positions.addIfLegal(pos)
                }
            }

            // Add en passant target position if available
            val enPassantTarget: Position = ChessMove.getEnPassantTarget(game, position)
            if (enPassantTarget != Position.NULL)
                positions.addIfLegal(enPassantTarget)

            return positions
        }

        fun getRookPositions(game: ChessGame, position: Position, checkLegality: Boolean = true) : MutableList<Position> {
            // TODO: Implement, similar to ChessGame.doLinearCheck
            return mutableListOf<Position>()
        }

        fun getBishopPositions(game: ChessGame, position: Position, checkLegality: Boolean = true) : MutableList<Position> {
            // TODO: Implement, similar to ChessGame.doLinearCheck
            return mutableListOf<Position>()
        }

        fun getKnightPositions(game: ChessGame, position: Position, checkLegality: Boolean = true) : MutableList<Position> {
            val positions = PositionList(game, position)
            positions.addIfLegal(+1, +2, checkLegality)
            positions.addIfLegal(+2, +1, checkLegality)
            positions.addIfLegal(+2, -1, checkLegality)
            positions.addIfLegal(+1, -2, checkLegality)
            positions.addIfLegal(-1, -2, checkLegality)
            positions.addIfLegal(-2, -1, checkLegality)
            positions.addIfLegal(-2, +1, checkLegality)
            positions.addIfLegal(-1, +2, checkLegality)
            return positions
        }

        fun getKingPositions(game: ChessGame, position: Position, checkLegality: Boolean = true) : MutableList<Position> {
            // TODO: Implement, easy static list of positions like Knight
            return mutableListOf<Position>()
        }

        fun getQueenPositions(game: ChessGame, position: Position, checkLegality: Boolean = true) : MutableList<Position> {
            val positions = getRookPositions(game, position)
            positions.addAll(getBishopPositions(game, position))
            return positions
        }
    }
}


