package me.ryzenfromfire.chesslin

import me.ryzenfromfire.chesslin.ChessPiece.PieceType.*
import me.ryzenfromfire.chesslin.ChessPiece.PieceType
import me.ryzenfromfire.chesslin.ChessGame.Player.*
import me.ryzenfromfire.chesslin.ChessGame.Player

class ChessBoard {
    // Access Syntax: boardArray[rank][file]
    private val boardArray = Array(NUM_RANKS_FILES) { Array(NUM_RANKS_FILES) { ChessPiece() } }

    // Lambda/Listener to override
    // rank is 0-based
    // var onSet = { rank0: Int, file: File, player: Player, piece: Piece -> print("${rank0}${file}: $player ${piece.str}")}

    var onSet: ((Position, ChessPiece) -> Unit)? = null

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
            fun parse(file: Char) = fileMap[file.toString()] ?: A
            val fileNames = Array(NUM_RANKS_FILES) { File.values()[it].str[0] }
            operator fun get(index: Int) = File.values()[index]
        }
    }

    class Position(val file: File = File.A, val rank: Int = -1) {
        constructor(str: String) : this(File.parse(str[0]), str[1].digitToInt())

        var valid = true
        init {
            if (this.rank !in 1..NUM_RANKS_FILES) {
                valid = false
            }
            if (this.file !in File.values()) {
                valid = false
            }
        }

        override fun toString(): String {
            return if (!valid) "" else "${file.str}${rank}"
        }

        override fun equals(other: Any?): Boolean {
            return if (other !is Position) false
            else if (this.rank != other.rank) false
            else this.file == other.file
        }

        override fun hashCode(): Int {
            var result = file.hashCode()
            result = 31 * result + rank
            return result
        }

        companion object {
            val NULL = Position(File.A, -1)
        }
    }

    init { reset() }

    fun set(position: Position, piece: ChessPiece): Boolean {
        return if (position.valid) {
            val fileIdx = position.file.index
            val rank0 = position.rank - 1
            boardArray[rank0][fileIdx] = piece
            onSet?.invoke(position, piece)
            true
        } else false
    }

    fun set(posStr: String, piece: ChessPiece): Boolean = set(Position(posStr), piece)

    fun set(posStr: String, player: Player, pieceType: PieceType): Boolean = set(posStr, ChessPiece(type=pieceType, player=player))

    fun set(position: Position) = set(position, ChessPiece())

    fun set(posStr: String) = set(Position(posStr))

    fun get(position: Position): ChessPiece {
        return if (position.valid) {
            val fileIdx = position.file.index
            val rank0 = position.rank - 1
            boardArray[rank0][fileIdx]
        } else ChessPiece.NULL
    }

    fun get(posStr: String): ChessPiece = get(Position(posStr))

    fun reset() {
        // White's Pieces
        set("a1", WHITE, ROOK)
        set("b1", WHITE, KNIGHT)
        set("c1", WHITE, BISHOP)
        set("d1", WHITE, QUEEN)
        set("e1", WHITE, KING)
        set("f1", WHITE, BISHOP)
        set("g1", WHITE, KNIGHT)
        set("h1", WHITE, ROOK)

        // White's Pawns
        for (c in 'a'..'h') {
            set("${c}2", WHITE, PAWN)
        }

        // Empty Middle of Board
        for (rank in 3..6) {
            for (c in 'a'..'h') {
                set("${c}${rank}")
            }
        }

        // Black's Pawns
        for (c in 'a'..'h') {
            set("${c}7", BLACK, PAWN)
        }

        // Black's Pieces
        set("a8", BLACK, ROOK)
        set("b8", BLACK, KNIGHT)
        set("c8", BLACK, BISHOP)
        set("d8", BLACK, QUEEN)
        set("e8", BLACK, KING)
        set("f8", BLACK, BISHOP)
        set("g8", BLACK, KNIGHT)
        set("h8", BLACK, ROOK)
    }
}