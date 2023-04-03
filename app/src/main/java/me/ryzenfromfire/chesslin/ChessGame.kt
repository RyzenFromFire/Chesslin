package me.ryzenfromfire.chesslin

class ChessGame {
    companion object {
        const val NUM_RANKS_FILES = 8
    }

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