package me.ryzenfromfire.chesslin

import android.util.Log
import me.ryzenfromfire.chesslin.ChessPiece.PieceType
import me.ryzenfromfire.chesslin.ChessPiece.PieceType.*
import me.ryzenfromfire.chesslin.ChessBoard.File.Companion.fileNames
import me.ryzenfromfire.chesslin.ChessBoard.Position

class ChessGame {
    var turn = Player.WHITE
    val board = ChessBoard()
    val moves = mutableListOf<ChessMove>()
    var position = Position.NULL
    var lastPosition = Position.NULL
    var isPieceSelected = false
    var inCheck: Player = Player.NONE
    var gameOver: Boolean = false
    var currentMove = 1
    lateinit var winner: Player
    var lastMove: ChessMove = ChessMove.NULL // TODO: Implement
    private var whiteKingPos = Position("e1")
    private var blackKingPos = Position("e8")

    var onTurnChangedListener: (() -> Unit)? = null

    enum class Player(val str: String) {
        NONE("None"),
        WHITE("White"),
        BLACK("Black");

        fun opponent(): Player {
            return when (this) {
                WHITE -> BLACK
                BLACK -> WHITE
                else -> NONE
            }
        }
    }

    /**
     * Processes game state and moves pieces if appropriate when a position is selected, as determined by the controller.
     * Returns true if a valid piece of the player whose turn it is was selected, and false otherwise.
     */
    fun select(pos: Position): Boolean {
        // TODO: Implement checking for move (end position) *legality*, not just position validity
        // TODO: (after above) give feedback to MainActivity (through a listener?) about legal positions to move, so it can highlight them in the View
        val lastIsPieceSelected = isPieceSelected

        this.position = if (pos.valid) pos else Position.NULL

        val selectedPiece = board.get(position)
        val lastSelectedPiece = board.get(lastPosition)

        // if there is a piece at the targeted position and the piece belongs to the player whose turn it currently is
        // Handles null position since Position.NULL is always invalid
        isPieceSelected = (position.valid) && (selectedPiece.type != PieceType.NONE) && (selectedPiece.player == turn)

        // if we have just selected a piece, and one was not already selected
        if (isPieceSelected && !lastIsPieceSelected) {
            // determine available moves
            // using listener similar to ChessBoard, update MainActivity with list of movable positions to display indicators
        }
        // at this point, `position` and `isPieceSelected` refer to the target position of the move and if there is a piece there
        // otherwise, if we have selected a piece last time this function was called, and we just selected a new position, make a move
        // note that `position.valid` is functionally identical to `position != Position.NULL`, since Position.NULL is automatically invalid
        // also, if the selected piece is of the same player whose turn it is, don't move because you can't move onto your own piece.
        else if (
            lastIsPieceSelected && // a piece was selected previously
            lastPosition.valid && // the last selected position is valid
            position.valid && // the target position is valid
            lastPosition != position &&
            selectedPiece.player != turn // the player is not trying to move onto their own piece
        ) {
            val promotion = PieceType.NONE // TODO: Implement promotion logic if pawn reaches back rank
            val castle = false // TODO: Implement castling logic using ChessMove.castleValid(). This might be better suited to the previous if statement and/or class property
            val move = ChessMove(
                game = this,
                num = currentMove,
                piece = lastSelectedPiece,
                start = lastPosition,
                end = position,
                capture = if (isPieceSelected) selectedPiece else ChessPiece.NULL,
                promotion = promotion,
                castle = castle
            )

            move(move)
            this.position = Position.NULL
            isPieceSelected = false
        }

        // this will update lastSelection appropriately, including setting it to Position.NULL if a move was made
        lastPosition = position

        return isPieceSelected
    }

    /**
     * Helper function to perform an actual move of a chess piece given a ChessMove instance.
     */
    private fun move(move: ChessMove): Boolean {
        if (!move.valid) return false // TODO: Also check for legality.
        board.set(position = move.end, piece = move.piece)
        board.set(position = move.start, piece = ChessPiece.NULL)
        // TODO: add functionality to keep track of pieces captured by each player
        moves.add(move)
        currentMove++
        switchTurn()
        if (move.piece.type == KING) {
            when (move.piece.player) {
                Player.WHITE -> whiteKingPos = move.end
                Player.BLACK -> blackKingPos = move.end
                else -> Log.e("ChessGame", "Movement Error: non-player king movement detected")
            }
        }
        return true
    }


    // Takes a move in algebraic notation, e.g. Qf8
    @Deprecated("Deprecated and soon to be replaced with new functionality")
    fun moveOld(str: String) {
        // TODO: Input validation
        // TODO: the ChessPiece should determine whether it can move to a different square given its current square.

        // If the first character of the algebraic notation is 'a'..'h', then the piece is a pawn.
        val pieceType: PieceType
        val originSquare: PieceType
        val first = str.lowercase().first()
        if (first in fileNames) {
            val file = first
            // Pawn move without capture (ex. c5)
            if (str.length == 2) {
                val destRank = str[1].digitToInt()
                val origRank: Int = if (turn == Player.WHITE) {
                    if (board.get("${str[0]}${destRank - 2}").type == PieceType.PAWN)
                        destRank - 2
                    else
                        destRank - 1
                } else if (turn == Player.BLACK) {
                    if (board.get("${str[0]}${destRank + 2}").type == PieceType.PAWN)
                        destRank + 2
                    else
                        destRank + 1
                } else destRank // error state, should never occur
                // TODO: make a board.move function to let that handle the setting instead of this:
                board.set("$file${origRank}", turn, PieceType.NONE)
                board.set("$file${destRank}", turn, PieceType.PAWN)
            }
            // Pawn move with capture
            else if (str.length == 4 && str[1].equals('x', true)) {
                // TODO: Implement
                print("pawn move capture")
            }
        } else { // Otherwise it is a non-pawn piece
            pieceType = PieceType.parseShort(str[0].toString())

            val isCapture: Boolean
            val offset: Int
            if (str[1].equals('x', true)) {
                // TODO: Deal with capture
                offset = 1
                isCapture = true
            } else {
                offset = 0
                isCapture = false
            }

            val substr = str.substring(1 + offset, 2 + offset)
        }


        switchTurn()
    }

    /**
     * Helper function for turn switching and notifying controller of a turn change.
     */
    private fun switchTurn() {
        if (turn == Player.WHITE) {
            turn = Player.BLACK
        } else if (turn == Player.BLACK) {
            turn = Player.WHITE
        }
        onTurnChangedListener?.invoke()
    }

    /**
     * Returns true iff. the move specified for the player in question
     * results in a check on that player's king, given the current game state.
     */
    fun isCheckedAfterMove(player: Player, move: ChessMove): Boolean {
        var result = false
        val moveEndPiece = board.get(move.end)
        // make appropriate changes to board temporarily without affecting game state
        // this function is effectively a modified version of move()
        if (!move.valid) return false
        board.set(position = move.end, piece = move.piece, callOnSetListener = false)
        board.set(position = move.start, piece = ChessPiece.NULL, callOnSetListener = false)

        // test if specified player's king is in check; store result
        val kingPos = when (player) {
            Player.WHITE -> whiteKingPos
            Player.BLACK -> blackKingPos
            else -> Position.NULL
        }
        result = isKingInCheck(player, kingPos)

        // revert changes
        board.set(position = move.end, piece = moveEndPiece, callOnSetListener = false)
        board.set(position = move.start, piece = move.piece, callOnSetListener = false)
        return result
    }

    /**
     * Tests if the specified player's king, at the given position, is currently in check.
     */
    private fun isKingInCheck(player: Player, kingPos: Position): Boolean {
        if (player == Player.NONE) return false
        var piece: ChessPiece

        // Test if a pawn could check the king
        val pawnPositions = mutableListOf<Position>()
        val rankDirection: Int = when (player) {
            Player.BLACK -> -1
            else -> 1 // when Player.WHITE, since Player.NONE will return by now
        }

        pawnPositions.add(board.getRelativePosition(kingPos, -1, rankDirection))
        pawnPositions.add(board.getRelativePosition(kingPos, 1, rankDirection))
        for (pos in pawnPositions) {
            piece = board.get(pos)
            // If a pawn belonging to the opponent is in the position, return true
            if (piece.player == player.opponent() && piece.type == PAWN) return true
        }

        // Test all eight possible spots a knight could check from
        val knightPositions = ChessPiece.getKnightPositions(this, kingPos, false)
        for (pos in knightPositions) {
            piece = board.get(pos)

            // If a knight belonging to the opponent is in the position, return true
            if (piece.player == player.opponent() && piece.type == KNIGHT) return true
        }

        // Test each of four linear directions sequentially for rook/queen check,
        // but stop if we encounter a piece blocking the check
        var check = false

        // Test upwards
        check = doLinearCheck(player, kingPos, 0, 1, ROOK)
        if (check) return true

        // Test downwards
        check = doLinearCheck(player, kingPos, 0, -1, ROOK)
        if (check) return true

        // Test right
        check = doLinearCheck(player, kingPos, 1, 0, ROOK)
        if (check) return true

        // Test left
        check = doLinearCheck(player, kingPos, -1, 0, ROOK)
        if (check) return true


        // Test each of the four diagonal directions sequentially for a bishop/queen check similarly

        // Test up and right
        check = doLinearCheck(player, kingPos, 1, 1, BISHOP)
        if (check) return true

        // Test up and left
        check = doLinearCheck(player, kingPos, -1, 1, BISHOP)
        if (check) return true

        // Test down and left
        check = doLinearCheck(player, kingPos, -1, -1, BISHOP)
        if (check) return true

        // Test down and right
        check = doLinearCheck(player, kingPos, 1, -1, BISHOP)
        if (check) return true

        return false
    }

    /**
     * Check in a line of positions emanating from the specified starting position
     * for an opposing piece capable of delivering check. If one is found, return true.
     * If a piece is found that is incapable of delivering check, or is owned by the
     * specified player, return false, as that piece is blocking any potential checks.
     */
    private fun doLinearCheck(
        player: Player,
        position: Position,
        rightOffsetScalar: Int,
        upOffsetScalar: Int,
        pieceType: PieceType = ROOK
    ): Boolean {
        var pos: Position
        var piece: ChessPiece
        var i = 1
        do {
            pos = board.getRelativePosition(position = position, rightOffset = rightOffsetScalar * i, upOffset = upOffsetScalar * i)
            piece = board.get(pos)
            if (piece.player != Player.NONE)
                if (pieceType == ROOK) {
                    return piece.player == player.opponent() && (piece.type == ROOK || piece.type == QUEEN)
                } else if (pieceType == BISHOP) {
                    return piece.player == player.opponent() && (piece.type == BISHOP || piece.type == QUEEN)
                }
            i++
        } while (pos.valid)
        return false
    }
}