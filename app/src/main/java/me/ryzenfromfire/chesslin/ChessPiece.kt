package me.ryzenfromfire.chesslin

import me.ryzenfromfire.chesslin.ChessGame.Player
import me.ryzenfromfire.chesslin.ChessBoard.Position

/**
 * Represents a single instance of a chess piece (for instance, a white pawn).
 * Contains state about if it has moved (for castling and en passant).
 */
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

    fun getDrawableID(): Int {
        var idx = PieceType.values().indexOf(this.type)
        if (this.player == Player.BLACK)
            idx += NUM_PIECES
        return drawables[idx]
    }

    /**
     * Returns a list of positions that can be legally be reached by the piece at the specified position.
     * @param checkLegality: default true, set false to ignore legality of moves considered, and only consider if the move is possible.
     */
    fun getMovablePositions(game: ChessGame, start: Position, checkLegality: Boolean = true): MutableList<Position> {
        return when (this.type) {
            PieceType.PAWN -> getPawnPositions(game, start, checkLegality)
            PieceType.ROOK -> getRookPositions(game, start, checkLegality)
            PieceType.BISHOP -> getBishopPositions(game, start, checkLegality)
            PieceType.KNIGHT -> getKnightPositions(game, start, checkLegality)
            PieceType.QUEEN -> getQueenPositions(game, start, checkLegality)
            PieceType.KING -> getKingPositions(game, start, checkLegality)
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
    /**
     * A wrapper for a mutable list that implements methods to ease adding legal positions relative to a starting position.
     */
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
                promotion = false,
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
        private val drawables = arrayOf(
            R.drawable.chess_null,
            R.drawable.chess_plt45,
            R.drawable.chess_nlt45,
            R.drawable.chess_blt45,
            R.drawable.chess_rlt45,
            R.drawable.chess_qlt45,
            R.drawable.chess_klt45,
            R.drawable.chess_pdt45,
            R.drawable.chess_ndt45,
            R.drawable.chess_bdt45,
            R.drawable.chess_rdt45,
            R.drawable.chess_qdt45,
            R.drawable.chess_kdt45
        )

        const val NUM_PIECES = 6

        val NULL = ChessPiece()

        /**
         * Determine the direction which a piece (realistically, a pawn)
         * of a given color will move based on the specified player (color).
         * @return a scalar (+1 or -1) for white or black respectively.
         * Avoid usage with Player.NONE (returns 0) - this could yield unexpected results.
         */
        fun getPawnDirection(player: Player): Int {
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
            // Must check for occupation so that capture is not allowed
            var targetPos = game.board.getRelativePosition(position, 0, getPawnDirection(player))
            if (!game.board.isOccupied(targetPos))
                positions.addIfLegal(targetPos, checkLegality)

            // Add initial 2-square move if the pawn has not yet moved
            // Must check for occupation so that capture is not allowed
            targetPos = game.board.getRelativePosition(position, 0, 2 * getPawnDirection(player))
            if (!game.board.get(position).hasMoved &&
                !game.board.isOccupied(targetPos) &&
                !game.board.isOccupied(game.board.getRelativePosition(targetPos, 0, -1 * getPawnDirection(player)))
            ) {
                positions.addIfLegal(targetPos, checkLegality)
            }

            // Add capturing positions if there is an opposing piece to the left or right (forward and diagonally)
            val tempList = listOf(
                game.board.getRelativePosition(position, +1, getPawnDirection(player)),
                game.board.getRelativePosition(position, -1, getPawnDirection(player))
            )
            for (pos in tempList) {
                if (game.board.get(pos).player == game.board.get(position).player.opponent()) {
                    positions.addIfLegal(pos)
                }
            }

            // Add en passant target position if available
            val enPassantTarget: Position = game.getEnPassantTarget(position)
            if (enPassantTarget != Position.NULL)
                positions.addIfLegal(enPassantTarget)

            return positions
        }

        fun getRookPositions(game: ChessGame, position: Position, checkLegality: Boolean = true) : MutableList<Position> {
            val positions = getLinearPositions(game, position, +1, 0)
            positions.addAll(getLinearPositions(game, position, -1, 0))
            positions.addAll(getLinearPositions(game, position, 0, +1))
            positions.addAll(getLinearPositions(game, position, 0, -1))
            return positions
        }

        fun getBishopPositions(game: ChessGame, position: Position, checkLegality: Boolean = true) : MutableList<Position> {
            val positions = getLinearPositions(game, position, +1, +1)
            positions.addAll(getLinearPositions(game, position, +1, -1))
            positions.addAll(getLinearPositions(game, position, -1, -1))
            positions.addAll(getLinearPositions(game, position, -1, +1))
            return positions
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
            val positions = PositionList(game, position)
            positions.addIfLegal( 0, +1, checkLegality)
            positions.addIfLegal(+1, +1, checkLegality)
            positions.addIfLegal(+1,  0, checkLegality)
            positions.addIfLegal(+1, -1, checkLegality)
            positions.addIfLegal( 0, -1, checkLegality)
            positions.addIfLegal(-1, -1, checkLegality)
            positions.addIfLegal(-1,  0, checkLegality)
            positions.addIfLegal(-1, +1, checkLegality)

            // Castling Logic
            val piece = game.board.get(position)

            // preliminary check to avoid computation if possible. technically redundant with castleValid, but kept for safety
            if (!piece.hasMoved) {
                val rank = when (piece.player) {
                    Player.WHITE -> 1
                    Player.BLACK -> 8
                    else -> 0
                }
                val startPos = Position(rank = rank, file = ChessBoard.File.E)
                val queensideEndPos = Position(rank = rank, file = ChessBoard.File.C)
                val queensideRookPos = Position(rank = rank, file = ChessBoard.File.A)
                val kingsideEndPos = Position(rank = rank, file = ChessBoard.File.G)
                val kingsideRookPos = Position(rank = rank, file = ChessBoard.File.H)
                if (rank != 0) {
                    if (game.castleValid(piece, startPos, queensideEndPos)) {
                        positions.addIfLegal(queensideEndPos, checkLegality)
                        positions.addIfLegal(queensideRookPos, false)
                    } else if (game.castleValid(piece, startPos, kingsideEndPos)) {
                        positions.addIfLegal(kingsideEndPos, checkLegality)
                        positions.addIfLegal(kingsideRookPos, false)
                    }
                }
            }

            return positions
        }

        fun getQueenPositions(game: ChessGame, position: Position, checkLegality: Boolean = true) : MutableList<Position> {
            val positions = getRookPositions(game, position, checkLegality)
            positions.addAll(getBishopPositions(game, position, checkLegality))
            return positions
        }

        /**
         * Sequentially look through linear positions according to the specified scalars,
         * and add them to a running PositionList. Return when an invalid position or another piece is encountered.
         */
        private fun getLinearPositions(game: ChessGame, start: Position, rightOffsetScalar: Int, upOffsetScalar: Int): PositionList {
            val player = game.board.get(start).player
            var pos: Position
            var piece: ChessPiece
            val list = PositionList(game, start)
            var i = 1
            do {
                pos = game.board.getRelativePosition(position = start, rightOffset = rightOffsetScalar * i, upOffset = upOffsetScalar * i)
                piece = game.board.get(pos)
                if (piece.player != player) {
                    list.addIfLegal(pos)
                    if (piece.player == player.opponent()) break
                } else { break }
                i++
            } while (pos.valid)
            return list
        }
    }
}


