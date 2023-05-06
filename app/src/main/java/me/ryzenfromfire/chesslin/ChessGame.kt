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
    var selectedPosition = Position.NULL
    var selectedPiece = ChessPiece.NULL
    var movablePositions = mutableListOf<Position>()
    var isPieceSelected = false // TODO: private?
    var inCheck: Player = Player.NONE
    var gameOver: Boolean = false
    var currentMove = 1
    lateinit var winner: Player
    var lastMove: ChessMove = ChessMove.NULL
    var whiteKingPos = Position("e1")
    var blackKingPos = Position("e8")

    // Listeners for Controller (MainActivity)
    var onTurnChangedListener: (() -> Unit)? = null
    var onMoveListener: (() -> Unit)? = null
    var onSelectListener: (() -> Unit)? = null

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
     * Selects, but does not move a piece.
     * Returns true if a valid piece of the player whose turn it is was selected, and false otherwise.
     */
    fun select(pos: Position): Boolean {
        if (pos.valid && board.get(pos).player == turn) {
            println("SELECTED: plr: ${board.get(pos).player}, turn: $turn, pos valid: ${pos.valid}")
            isPieceSelected = true
            selectedPosition = pos
            selectedPiece = board.get(pos)

            // Determine legal positions to move to from the selected position
            movablePositions = selectedPiece.getMovablePositions(this, selectedPosition)

            onSelectListener?.invoke()
        }

        return isPieceSelected
    }

    enum class MoveResult {
        NO_PIECE_SELECTED,
        END_POSITION_INVALID,
        SAME_START_END_POS,
        MOVE_ONTO_OWN_PIECE,
        MOVE_INVALID,
        MOVE_ILLEGAL,
        MOVE_GOOD
    }

    /**
     * Returns true only if the move created from the specified end position is legal.
     */
    fun isMoveLegal(end: Position): Boolean {
        return getMove(end).legal
    }

    /**
     * Generates a ChessMove from the given position based on the current game state.
     */
    private fun getMove(end: Position): ChessMove {
        val targetPiece = board.get(end)
        val promotion = PieceType.NONE // TODO: Implement promotion logic if pawn reaches back rank
        val castle =
            false // TODO: Implement castling logic using ChessMove.castleValid().
        val capture = targetPiece.player == selectedPiece.player.opponent()
        return ChessMove(
            game = this,
            num = currentMove,
            piece = selectedPiece,
            start = selectedPosition,
            end = end,
            capture = if (capture) targetPiece else ChessPiece.NULL,
            promotion = promotion,
            castle = castle
        )
    }

    /**
     * Determines if moving the selected piece from the selected position to the specified target/end position is legal.
     * If so, it performs the move and calls the appropriate update listener, then resets the selection.
     */
    fun move(end: Position): MoveResult {
        if (!isPieceSelected) return MoveResult.NO_PIECE_SELECTED
        if (!end.valid) return MoveResult.END_POSITION_INVALID
        if (end == selectedPosition) return MoveResult.SAME_START_END_POS
        if (board.get(end).player == selectedPiece.player) return MoveResult.MOVE_ONTO_OWN_PIECE

        // Generate move based on selected position
        val move = getMove(end)

        if (!move.valid) return MoveResult.MOVE_INVALID
        if (!move.legal) return MoveResult.MOVE_ILLEGAL

        // At this point the move is good, so perform it
        // En Passant Handling
        if (end == ChessMove.getEnPassantTarget(this, selectedPosition)) {
            val capturedPosition = board.getRelativePosition(end, 0, -1 * ChessPiece.getPawnDirection(selectedPiece.player))
            board.set(position = capturedPosition, piece = ChessPiece.NULL)
        }

        board.set(position = move.end, piece = move.piece)
        board.set(position = move.start, piece = ChessPiece.NULL)
        // TODO: add functionality to keep track of pieces captured by each player
        moves.add(move)
        currentMove++
        selectedPiece.hasMoved = true

        if (move.piece.type == KING) {
            when (move.piece.player) {
                Player.WHITE -> whiteKingPos = move.end
                Player.BLACK -> blackKingPos = move.end
                else -> Log.e("ChessGame", "Movement Error: non-player king movement detected")
            }
        }

        // Update if a player is in check
        // Should only happen to the player whose turn it is not
        inCheck = if (isKingInCheck(Player.WHITE, whiteKingPos)) {
            Player.WHITE
        } else if (isKingInCheck(Player.BLACK, blackKingPos)) {
            Player.BLACK
        } else Player.NONE

        if (inCheck == turn)
            Log.e("ChessGame", "Player $turn moved into check!")

        switchTurn()
        lastMove = move

        // Deselect position, empty list of movable positions, and invoke listener
        selectedPosition = Position.NULL
        isPieceSelected = false
        movablePositions = mutableListOf()
        onMoveListener?.invoke()
        return MoveResult.MOVE_GOOD
    }

    // Takes a move in algebraic notation, e.g. Qf8
    // TODO: The only reason this is still around is I think it might be useful for generating algebraic notation when I get around to that
    @Deprecated("Deprecated and soon to be replaced with new functionality")
    fun moveOlder(str: String) {

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
        // must consider if new (target/end) position places king in check if moving the king, instead of current position
        val kingPos = if (move.piece.type == KING) {
            move.end
        } else when (player) {
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
        pawnPositions.add(board.getRelativePosition(kingPos, -1, ChessPiece.getPawnDirection(player)))
        pawnPositions.add(board.getRelativePosition(kingPos, 1, ChessPiece.getPawnDirection(player)))
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
        println("performing linear check for $player originating at $position with scalars {r $rightOffsetScalar, u $upOffsetScalar}, pieceType = $pieceType")
        println(board.toString())
        do {
            pos = board.getRelativePosition(position = position, rightOffset = rightOffsetScalar * i, upOffset = upOffsetScalar * i)
            piece = board.get(pos)
            println("lin check looping; pos: $pos with piece $piece")
            println("piece.player = ${piece.player}, player = $player")
            if (piece.player == player.opponent()) {
                if (pieceType == ROOK) {
                    return (piece.type == ROOK || piece.type == QUEEN)
                } else if (pieceType == BISHOP) {
                    return (piece.type == BISHOP || piece.type == QUEEN)
                }
            } else if (piece.player == player) { return false }
            i++
        } while (pos.valid)
        return false
    }
}