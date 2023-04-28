package me.ryzenfromfire.chesslin

import me.ryzenfromfire.chesslin.ChessPiece.PieceType
import me.ryzenfromfire.chesslin.ChessBoard.File.Companion.fileNames
import me.ryzenfromfire.chesslin.ChessBoard.Position

class ChessGame {
    var turn = Player.WHITE
    val board = ChessBoard()
    val moves = mutableListOf<ChessMove>()
    var selection = Position.NULL // Don't modify directly outside of this file
    var lastSelection = Position.NULL
    var pieceSelected = false
    var inCheck: Player = Player.NONE
    var gameOver: Boolean = false
    var currentMove = 1
    lateinit var winner: Player
    lateinit var lastMove: ChessMove // TODO: Implement

    enum class Player(val str: String) {
        NONE("None"),
        WHITE("White"),
        BLACK("Black")
    }

    fun select(position: Position) {
        val lastPieceSelected = pieceSelected

        // TODO: Consider refactoring to a local variable. Would also have to make lastPieceSelected basically duplicate this initialization with lastSelection.
        // if the player whose turn it currently is has a piece at the targeted position
        pieceSelected = (position.valid) && (board.get(position).type != PieceType.NONE) && (board.get(position).player == turn)

        // select the passed position if it is valid
        selection = if (position.valid) position else { Position.NULL }

        // if we have just selected a piece, and one was not already selected
        if (pieceSelected && !lastPieceSelected) {
            // determine available moves
            // using listener similar to ChessBoard, update MainActivity with list of movable positions to display indicators
        }
        // otherwise, if we have selected a piece last time this function was called, and we just selected a new position, make a move
        // note that `position.valid` is functionally identical to `selection != Position.NULL`, since Position.NULL is automatically invalid
        else if (lastPieceSelected && lastSelection != Position.NULL && position.valid) {
            val promotion = PieceType.NONE // TODO: Implement promotion logic if pawn reaches back rank
            val castle = false // TODO: Implement castling logic using ChessMove.castleValid(). This might be better suited to the previous if statement and/or class property
            val move = ChessMove(
                game = this,
                num = currentMove,
                piece = board.get(lastSelection),
                start = lastSelection,
                end = selection,
                capture = if (pieceSelected) board.get(selection) else ChessPiece.NULL,
                promotion = promotion,
                castle = castle
            )

            move(move)
            selection = Position.NULL
            pieceSelected = false
        }

        // this will update lastSelection appropriately, including setting it to Position.NULL if a move was made
        lastSelection = selection
    }

    private fun move(move: ChessMove): Boolean {
        if (!move.valid) return false
        board.set(position = move.end, piece = move.piece)
        board.set(position = move.start, piece = ChessPiece.NULL)
        // TODO: add functionality to keep track of pieces captured by each player
        moves.add(move)
        currentMove++
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

    private fun switchTurn() {
        if (turn == Player.WHITE) {
            turn = Player.BLACK
        } else if (turn == Player.BLACK) {
            turn = Player.WHITE
        }
    }
}