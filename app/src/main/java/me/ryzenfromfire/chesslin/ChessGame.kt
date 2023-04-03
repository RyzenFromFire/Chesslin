package me.ryzenfromfire.chesslin

import me.ryzenfromfire.chesslin.ChessPiece.Piece
import me.ryzenfromfire.chesslin.ChessBoard.File.Companion.files

class ChessGame {
    var turn = Player.WHITE
    val board = ChessBoard()

    enum class Player {
        NONE, WHITE, BLACK
    }

    // Takes a move in algebraic notation, e.g. Qf8
    fun move(str: String) {
        // TODO: Input validation

        // If the first character of the algebraic notation is 'a'..'h', then the piece is a pawn.
        val pieceType: Piece
        val originSquare: Piece
        val first = str[0].lowercase()[0]
        if (first in files) {
            val file = first
            // Pawn move without capture (ex. c5)
            if (str.length == 2) {
                val destRank = str[1].digitToInt()
                val origRank: Int = if (turn == Player.WHITE) {
                    if (board.get("${str[0]}${destRank - 2}").piece == Piece.PAWN)
                        destRank - 2
                    else
                        destRank - 1
                } else if (turn == Player.BLACK) {
                    if (board.get("${str[0]}${destRank + 2}").piece == Piece.PAWN)
                        destRank + 2
                    else
                        destRank + 1
                } else destRank // error state, should never occur
                board.set("$file${origRank}", turn, Piece.NONE)
                board.set("$file${destRank}", turn, Piece.PAWN)
            }
            // Pawn move with capture
            else if (str.length == 4 && str[1].equals('x', true)) {
                // TODO: Implement
                print("pawn move capture")
            }
        } else { // Otherwise it is a non-pawn piece
            pieceType = Piece.parse(str[0].toString())!!

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


        if (turn == Player.WHITE) {
            turn = Player.BLACK
        } else if (turn == Player.BLACK) {
            turn = Player.WHITE
        }
    }

}