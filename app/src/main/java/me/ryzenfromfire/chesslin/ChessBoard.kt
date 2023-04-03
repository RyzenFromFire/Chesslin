package me.ryzenfromfire.chesslin

import me.ryzenfromfire.chesslin.ChessPiece.Piece
import me.ryzenfromfire.chesslin.ChessGame.Player

class ChessBoard {
    // Access Syntax: boardArray[rank][file]
    private var boardArray = Array(NUM_RANKS_FILES) { Array(NUM_RANKS_FILES) { ChessPiece() } }

    // Lambda/Listener to override
    // rank is 0-based
//    var onSet = { rank0: Int, file: File, player: Player, piece: Piece -> print("${rank0}${file}: $player ${piece.str}")}

    var onSet: ((Int, File, Player, Piece) -> Unit)? = null

    companion object {
        const val NUM_RANKS_FILES = 8
    }

    enum class File(val str: String, val index: Int) {
        A("a", 0),
        B("b", 1),
        C("c", 2),
        D("d", 3),
        E("e", 4),
        F("f", 5),
        G("g", 6),
        H("h", 7);

        companion object {
            private val fileMap = File.values().associateBy { it.str }
            fun parse(file: Char) = fileMap[file.toString()]
            val files = Array(NUM_RANKS_FILES) { File.values()[it].str[0] }
        }
    }

    init { reset() }

    fun set(position: String, player: Player, piece: Piece) {
        // TODO: position validation
        val file: File = File.parse(position[0])!!
        val rank: Int = (position[1].digitToInt()) - 1 // 0-based
        set(rank, file, player, piece, true)
    }

    fun set(rank: Int, file: File, player: Player, piece: Piece, rankIsZeroBased: Boolean = false) {
        // ensure zero-based rank
        val rank0 = if (rankIsZeroBased) rank else (rank - 1)
        boardArray[rank0][file.index] = ChessPiece(piece, player)
        onSet?.invoke(rank0, file, player, piece)
    }

    fun get(position: String): ChessPiece {
        val rank = (position[1].digitToInt()) - 1
        val file = File.parse(position[0])!!
        return get(rank, file)
    }

    fun get(rank0: Int, file: File): ChessPiece {
        return boardArray[rank0][file.index]
    }

    fun reset() {
        // White's Pieces
        set("a1", Player.WHITE, Piece.ROOK)
        set("b1", Player.WHITE, Piece.KNIGHT)
        set("c1", Player.WHITE, Piece.BISHOP)
        set("d1", Player.WHITE, Piece.QUEEN)
        set("e1", Player.WHITE, Piece.KING)
        set("f1", Player.WHITE, Piece.BISHOP)
        set("g1", Player.WHITE, Piece.KNIGHT)
        set("h1", Player.WHITE, Piece.ROOK)

        // White's Pawns
        for (c in 'a'..'h') {
            set("${c}2", Player.WHITE, Piece.PAWN)
        }

        // Empty Middle of Board
        for (rank in 3..6) {
            for (c in 'a'..'h') {
                set("${c}${rank}", Player.NONE, Piece.NONE)
            }
        }

        // Black's Pawns
        for (c in 'a'..'h') {
            set("${c}7", Player.BLACK, Piece.PAWN)
        }

        // Black's Pieces
        set("a8", Player.BLACK, Piece.ROOK)
        set("b8", Player.BLACK, Piece.KNIGHT)
        set("c8", Player.BLACK, Piece.BISHOP)
        set("d8", Player.BLACK, Piece.QUEEN)
        set("e8", Player.BLACK, Piece.KING)
        set("f8", Player.BLACK, Piece.BISHOP)
        set("g8", Player.BLACK, Piece.KNIGHT)
        set("h8", Player.BLACK, Piece.ROOK)
    }
}