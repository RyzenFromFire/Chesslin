package me.ryzenfromfire.chesslin

class ChessGame {


    var turn = Player.WHITE
    val board = ChessBoard()

    enum class Player {
        NONE, WHITE, BLACK
    }

    // Takes a move in algebraic notation, e.g. Qf8
    fun move(move: String) {
        // TODO: Input validation

    }

}