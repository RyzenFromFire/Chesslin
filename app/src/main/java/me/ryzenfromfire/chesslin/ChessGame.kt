package me.ryzenfromfire.chesslin

import android.util.Log
import me.ryzenfromfire.chesslin.ChessPiece.PieceType
import me.ryzenfromfire.chesslin.ChessPiece.PieceType.*
import me.ryzenfromfire.chesslin.ChessBoard.File.Companion.fileNames
import me.ryzenfromfire.chesslin.ChessBoard.Position
import me.ryzenfromfire.chesslin.ChessBoard.File

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
    var onCheckmateListener: ((Player) -> Unit)? = null // Player passed is the player who has been checkmated

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
        if (selectedPosition == pos) return isPieceSelected // if the selected piece hasn't changed, do nothing
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

        // TODO: Implement promotion logic if pawn reaches back rank
        val promotion = PieceType.NONE

        // If castling is valid in the context of the specified end position, the move must be a castle
        val castle = castleValid(selectedPiece, selectedPosition, end)

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
        if (end == getEnPassantTarget(selectedPosition)) {
            val capturedPosition = board.getRelativePosition(end, 0, -1 * ChessPiece.getPawnDirection(selectedPiece.player))
            board.set(position = capturedPosition, piece = ChessPiece.NULL)
        }

        // Castling Handling
        if (move.castle) {
            // Direction to move the rook
            var dir = 0
            var rookOffset = 0
            when (end.file) {
                File.C -> {
                    dir = 1 // right of king, when castling queenside
                    rookOffset = -4
                    // TODO: consider adding additional check like so:
                    // if (!queensideCastlePossible(move.piece, move.start, move.end)) return MoveResult.MOVE_ILLEGAL
                }
                File.G -> {
                    dir = -1 // left of king, when castling kingside
                    rookOffset = 3
                }
                else -> {}
            }
            // If castle is true/valid, dir of 0 should be impossible
            if (dir == 0) return MoveResult.MOVE_ILLEGAL
            val rookStartPos = board.getRelativePosition(move.start, rookOffset, 0)
            val rookEndPos = board.getRelativePosition(move.end, dir, 0)
            val rookPiece = board.get(rookStartPos)
            board.set(position = rookEndPos, piece = rookPiece)
            board.set(position = rookStartPos, piece = ChessPiece.NULL)
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

        move.check = inCheck

        switchTurn()
        lastMove = move

        // Deselect position, empty list of movable positions, and invoke listener
        selectedPosition = Position.NULL
        isPieceSelected = false
        movablePositions = mutableListOf()
        onMoveListener?.invoke()

        checkIfMate()

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

    private fun checkIfMate(): Boolean {
        if (inCheck == Player.NONE) return false
        val positions = when (inCheck) {
            Player.WHITE -> board.whitePiecePositions.toList()
            else -> board.blackPiecePositions.toList()
        }
        var temp: MutableList<Position>
        for (pos in positions) {
            temp = board.get(pos).getMovablePositions(this, pos, true)
            // If there is a legal move, it is not checkmate
            if (temp.isNotEmpty()) return false
        }
        // At this point, all of the player's positions have been checked, and there are no legal moves, so it is checkmate.
        onCheckmateListener?.invoke(inCheck)
        return true
    }

    /**
     * Given a Position (at which it is assumed there is a pawn)
     * and the game (from which the last move is retrieved),
     * return the target Position if en passant is possible.
     */
    fun getEnPassantTarget(pos: Position): Position {
        // Return null if pieces are not pawns or last move is invalid
        if (board.get(pos).type != PAWN ||
            !lastMove.valid ||
            lastMove.piece.type != PAWN
        ) return Position.NULL

        // Return appropriate position based on if the last move ended to the left or right
        val left = board.getRelativePosition(pos, -1, 0)
        val right = board.getRelativePosition(pos, 1, 0)
        return if (lastMove.end == left && board.get(left).hasJustMoved) {
            board.getRelativePosition(pos, -1, 1)
        } else if (lastMove.end == right && board.get(right).hasJustMoved) {
            board.getRelativePosition(pos, 1, 1)
        } else Position.NULL
    }

    /**
     * Determines if a castling move is valid given the start and end positions
     * alongside a reference to the piece (assumed to be a king)
     */
    fun castleValid(piece: ChessPiece, start: Position, end: Position): Boolean {
        return (
            start.rank == end.rank &&
            piece.type == KING &&
            !piece.hasMoved &&
            start.file == File.E &&
            (end.file == File.C || end.file == File.G) &&
            ((start.rank == 1 && piece.player == Player.WHITE) || (start.rank == 8 && piece.player == Player.BLACK)) &&
            (queensideCastlePossible(piece, start, end) || kingsideCastlePossible(piece, start, end))
        )
    }

    /**
     * Checks the following items, and returns false if any of them are true:
     * If the end file of the specified end position is not C, since that must be the ending file for a queenside castle
     * If the queenside rook has moved, since castling is only possible when both the king and rook have not moved.
     * If the king would have to move through check to castle.
     */
    private fun queensideCastlePossible(piece: ChessPiece, start: Position, end: Position): Boolean {
//        println("Checking if queenside castle is possible {piece=$piece, start=$start, end=$end}")
        if (end.file != File.C) return false
        val rookPos = board.getRelativePosition(start, -4, 0)
//        println("rookPos: $rookPos, rook has moved: ${board.get(rookPos).hasMoved}")
        if (board.get(rookPos).hasMoved) return false
        val positions = arrayOf(
            board.getRelativePosition(start, -1, 0),
            board.getRelativePosition(start, -2, 0),
            board.getRelativePosition(start, -3, 0),
        )
//        println("positions: [ ${positions.joinToString(" ")} ]")
        var testMove: ChessMove
        var checkedAfterMove: Boolean
        // Test if the king would be in check in each position, as castling through check is invalid
        for (pos in positions) {
//            println("testing $pos, isOccupied?=${board.isOccupied(pos)}")
            if (board.isOccupied(pos)) return false
            testMove = ChessMove(
                game = this,
                num = ChessMove.NUM_TEST_MOVE,
                piece = piece,
                start = start,
                end = pos,
                capture = ChessPiece.NULL,
                promotion = NONE,
                castle = false
            )
//            println("test move: $testMove")
//            println("test move is valid? ${testMove.valid}")
            checkedAfterMove = isCheckedAfterMove(piece.player, testMove)
//            println("checked after test move? $checkedAfterMove")
            if (checkedAfterMove) return false
        }
//        println("queenside castle valid (returning true)")
        return true
    }

    /**
     * Checks the following items, and returns false if any of them are true:
     * If the end file of the specified end position is not G, since that must be the ending file for a kingside castle
     * If the kingside rook has moved, since castling is only possible when both the king and rook have not moved.
     * If the king would have to move through check to castle.
     */
    private fun kingsideCastlePossible(piece: ChessPiece, start: Position, end: Position): Boolean {
//        println("Checking if kingside castle is possible {piece=$piece, start=$start, end=$end}")
        if (end.file != File.G) return false
        val rookPos = board.getRelativePosition(start, +3, 0)
//        println("rookPos: $rookPos, rook has moved: ${board.get(rookPos).hasMoved}")
        if (board.get(rookPos).hasMoved) return false
        val positions = arrayOf(
            board.getRelativePosition(start, +1, 0),
            board.getRelativePosition(start, +2, 0)
        )
//        println("positions: [ ${positions.joinToString(" ")} ]")
        // Test if the king would be in check in each position, as castling through check is invalid
        var testMove: ChessMove
        var checkedAfterMove: Boolean
        for (pos in positions) {
//            println("testing $pos, isOccupied?=${board.isOccupied(pos)}")
            if (board.isOccupied(pos)) return false
            testMove = ChessMove(
                game = this,
                num = ChessMove.NUM_TEST_MOVE,
                piece = piece,
                start = start,
                end = pos,
                capture = ChessPiece.NULL,
                promotion = NONE,
                castle = false
            )
//            println("test move: $testMove")
//            println("test move is valid? ${testMove.valid}")
            checkedAfterMove = isCheckedAfterMove(piece.player, testMove)
//            println("checked after test move? $checkedAfterMove")
            if (checkedAfterMove) return false
        }
//        println("kingside castle valid (returning true)")
        return true
    }
}