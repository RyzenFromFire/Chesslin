/**
 * Chess Piece Images from: https://commons.wikimedia.org/wiki/Template:SVG_chess_pieces
 * Licensed Under CC3: https://creativecommons.org/licenses/by-sa/3.0/deed.en
 * Author: Cburnett
 */

package me.ryzenfromfire.chesslin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.gridlayout.widget.GridLayout
import me.ryzenfromfire.chesslin.ChessBoard.Companion.NUM_RANKS_FILES
import me.ryzenfromfire.chesslin.ChessGame.Player
import me.ryzenfromfire.chesslin.ChessBoard.Position
import me.ryzenfromfire.chesslin.ChessBoard.File
import me.ryzenfromfire.chesslin.ChessGame.MoveResult.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var mainLayout: ConstraintLayout
    private lateinit var boardGridLayout: GridLayout
    private lateinit var boardViewArray: Array<Array<SquareImageView>>
    private lateinit var game: ChessGame

    private lateinit var turnTextView: TextView

    // TODO: Remove; for debug only
    private lateinit var debugTextView: TextView
    private lateinit var resetButton: Button

    private var followerView: SquareImageView? = null
    private val followerViewScalar = 1.66f
    private var followerViewRadius = 0.0f

    private val selectEmptyShadowScalar = 0.4f
    private val selectPieceShadowScalar = 1.0f
    private var shadowedPositions = mutableListOf<Position>()

    private var boardFlipped = true

    private lateinit var audioManager: AudioManager
    private var lastSelectedPosition = Position.NULL
    private val selectedPieceAlpha = 0.4f

    private lateinit var dialogBuilder: AlertDialog.Builder

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainLayout = findViewById(R.id.layout_main)

        game = ChessGame()

        boardGridLayout = findViewById(R.id.boardGridLayout)
        boardGridLayout.removeAllViews()
        boardGridLayout.columnCount = NUM_RANKS_FILES
        boardGridLayout.rowCount = NUM_RANKS_FILES

        followerViewRadius = ((boardGridLayout.width / NUM_RANKS_FILES) * followerViewScalar).roundToInt() / 4.0f

        boardViewArray = Array(NUM_RANKS_FILES) { Array(NUM_RANKS_FILES) { SquareImageView(this) } }

        turnTextView = findViewById(R.id.turnTextView)
        debugTextView = findViewById(R.id.debugView)

        dialogBuilder = AlertDialog.Builder(this)

        // Dynamic GridLayout Generation adapted from:
        // https://stackoverflow.com/questions/14728157/dynamic-gridlayout
        var iView: SquareImageView

        // loop must go from highest rank to lowest and across each file,
        // so the positions are added to the grid layout in the correct order
        for (rank in NUM_RANKS_FILES downTo 1) {
            for (file in 0 until NUM_RANKS_FILES) {
                val pos = Position(file = File[file]!!, rank = rank)

                // Create view using helper function
                iView = createChessPieceView(game.board.get(pos))

                iView.setOnTouchListener { v, event ->
                    onTouchCallback(v, event, pos)
                }

                setView(pos, iView)

                // Border for debugging
                // https://stackoverflow.com/a/62720394
                // val shapeDrawable = MaterialShapeDrawable()
                // shapeDrawable.fillColor = ContextCompat.getColorStateList(this,android.R.color.transparent)
                // shapeDrawable.setStroke(1.0f, ContextCompat.getColor(this,R.color.black))
                // tv.background = shapeDrawable

                // Create the row and column specifications
                // 'start' = UNDEFINED, that's fine.
                // 'size' = 1 always, since we aren't using >1 cell for each square.
                // 'alignment' = FILL since we want to distribute the cells evenly over the grid.
                // 'weight' = 1 is needed to determine the aforementioned distribution.
                val rowSpan = GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL, 1F)
                val colSpan = GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL, 1F)
                val params = GridLayout.LayoutParams(rowSpan, colSpan)
                params.setGravity(Gravity.CENTER)
                boardGridLayout.addView(iView, params)
            }
        }

        game.board.onSet = { position: Position, piece: ChessPiece ->
            if (position.valid) {
                val view = boardViewArray[position.rank - 1][position.file.index]
                resetViewDrawable(view, position)
            }
        }

        game.onSelectListener = {
            onSelectCallback()
        }

        // https://stackoverflow.com/questions/7914518/how-to-play-default-tick-sound
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // TODO: listener may not be necessary - could check for MOVE_GOOD in viewOnTouchListener when event UP and perform these actions then
        // TODO: however, that may also be trying to do too much at once.
        game.onMoveListener = {
            onMoveCallback()
        }

        game.onCheckmateListener = { player ->
            onCheckmateCallback(player)
        }

        // For Debug
        resetButton = findViewById(R.id.resetButton)
        resetButton.setOnClickListener {
            resetGame()
        }
    }

    private fun resetGame() {
        this.recreate()
    }

    private fun getView(position: Position): SquareImageView? {
        return if (position.valid)
            boardViewArray[position.rank - 1][position.file.index]
        else null
    }

    private fun setView(position: Position, view: SquareImageView): Boolean {
        return if (position.valid) {
            boardViewArray[position.rank - 1][position.file.index] = view
            true
        } else false
    }

    private fun switchTurn() {
        if (game.turn == Player.BLACK)
            turnTextView.text = getString(R.string.turn_black)
        else turnTextView.text = getString(R.string.turn_white)
    }

    private fun addPositionShadow(position: Position): Boolean {
        return if (position.valid) {
            val v = boardViewArray[position.rank - 1][position.file.index]
            v.background = getShadowDrawable() // radius = 0.5f * size
            val scalar = if (game.board.isOccupied(position)) {
                selectPieceShadowScalar
            } else {
                selectEmptyShadowScalar
            }

            v.scaleX = scalar
            v.scaleY = scalar
            true
        } else false
    }

    private fun removePositionShadow(position: Position) {
        val v = boardViewArray[position.rank - 1][position.file.index]
        v.background = null
        v.scaleX = 1.0f
        v.scaleY = 1.0f
    }

    private fun resetViewDrawable(view: SquareImageView, position: Position) = resetViewDrawable(view, game.board.get(position))

    private fun resetViewDrawable(view: SquareImageView, piece: ChessPiece) {
        val id = piece.getDrawableID()

        if (boardFlipped && piece.player == Player.BLACK) {
            val matrix = Matrix()
            matrix.preScale(1.0f, -1.0f)
            val dwg = AppCompatResources.getDrawable(this, id)
            val bmp = Bitmap.createBitmap(dwg!!.toBitmap(), 0, 0, dwg.intrinsicWidth, dwg.intrinsicHeight, matrix, true)
            view.setImageBitmap(bmp)
        } else {
            view.setImageResource(id)
        }

    }

    private fun setEmptyDrawable(view: SquareImageView) {
        view.setImageResource(R.drawable.chess_null)
    }

    private fun createChessPieceView(piece: ChessPiece): SquareImageView {
        val view = SquareImageView(this)
        resetViewDrawable(view, piece)
        return view
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFollowerView(v: View, piece: ChessPiece): SquareImageView {
        val followerView = createChessPieceView(piece)
        mainLayout.addView(followerView)

        // Creating backdrop shadow
        val gd = getShadowDrawable()
        followerView.background = gd
        followerView.scaleX = followerViewScalar
        followerView.scaleY = followerViewScalar

        resetViewDrawable(followerView, piece)
        followerViewRadius = (followerViewScalar * v.width).roundToInt() / 4.0f
        followerView.x = v.x + boardGridLayout.x - followerViewRadius
        followerView.y = v.y + boardGridLayout.y - followerViewRadius
        return followerView
    }

    private fun destroyFollowerView() {
        if (followerView != null) {
            // Destroy and reset for next use
            setEmptyDrawable(followerView!!)
            followerView!!.background = null
            followerView = null
        }
    }

    private fun getShadowDrawable(color: Int = R.color.shadow): GradientDrawable {
        // Creating backdrop shadow
        // https://stackoverflow.com/questions/45608049/how-to-make-a-circular-drawable-with-stroke-programmatically/45608694
        val gd = GradientDrawable()
        gd.color = ContextCompat.getColorStateList(this, color)
        gd.shape = GradientDrawable.OVAL
        return gd
    }

    // Currently these two functions are nearly identical, but in case I need to change their functionality, I'm keeping them separate
    private fun getCheckDrawable(v: SquareImageView): GradientDrawable {
        val gd = GradientDrawable()
        gd.colors = intArrayOf(getColor(R.color.checkColor), getColor(android.R.color.transparent))
        gd.gradientType = GradientDrawable.RADIAL_GRADIENT
        gd.gradientRadius = 0.66f * v.width
        gd.setStroke(0, getColor(android.R.color.transparent))
        return gd
    }

    private fun getSelectionDrawable(v: SquareImageView): GradientDrawable {
        val gd = GradientDrawable()
        gd.colors = intArrayOf(getColor(R.color.selectionColor), getColor(android.R.color.transparent))
        gd.gradientType = GradientDrawable.RADIAL_GRADIENT
        gd.gradientRadius = 0.66f * v.width
        gd.setStroke(0, getColor(android.R.color.transparent))
        return gd
    }

    private fun getPositionFromCoordinates(v: View, event: MotionEvent): Position {
        // Note: cell width (v.width) == 132px and board width = 1058px on Pixel 3a emulator
        // Debugging info for view and event coordinates
//        println("view (${v.x}, ${v.y})")
//        println("event (${event.x}, ${event.y})")
//        println("v.x + event.x: ${v.x + event.x}, v.y + event.y: ${v.y + event.y}")

        // The event coordinates are relative to the original view (v), so first add them.
        // Then, calculate the "radius" of the view and then subtract it from the result.
        // This is necessary because the position of the view is considered the top left.
        val radius = v.width.toDouble() / 2.0
//        println("radius: $radius")

        val xPct = (v.x + event.x - radius) / boardGridLayout.width
        val yPct = (v.y + event.y - radius) / boardGridLayout.height

//        println("xPct: $xPct, yPct: $yPct")

        val newRank = NUM_RANKS_FILES - ((yPct * NUM_RANKS_FILES).roundToInt())
        val newFileIdx = (xPct * NUM_RANKS_FILES).roundToInt()
//        println("newRank: $newRank, newFile: $newFile")

        if (newFileIdx !in File.values().indices) return Position.NULL

        return Position(rank = newRank, file = File[newFileIdx]!!)
    }

    private fun onSelectCallback() {
        val lv = getView(lastSelectedPosition)
        lv?.background = null
        for (position in shadowedPositions) {
            removePositionShadow(position)
        }
//        print("positions: [ ")
        for (position in game.movablePositions) {
            addPositionShadow(position)
//            print("$position ")
        }
//        println("]")
        shadowedPositions = game.movablePositions

        // Add an indicator to the selected position
        val v = getView(game.selectedPosition)
        v?.background = getSelectionDrawable(v!!)
        lastSelectedPosition = game.selectedPosition
    }

    private fun onMoveCallback() {
        // remove position shadows. will not select any new positions since game.movablePositions is an empty list.
        game.onSelectListener?.invoke()
        switchTurn()
        audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK)
//            println("White Piece Positions: [ ${game.board.whitePiecePositions.joinToString(" ")} ]")
//            println("Black Piece Positions: [ ${game.board.blackPiecePositions.joinToString(" ")} ]")

        when (game.inCheck) {
            Player.WHITE -> {
                val v = getView(game.whiteKingPos)
                v?.background = getCheckDrawable(v!!)
            }
            Player.BLACK -> {
                val v = getView(game.blackKingPos)
                v?.background = getCheckDrawable(v!!)
            }
            else -> {
                getView(game.whiteKingPos)?.background = null
                getView(game.blackKingPos)?.background = null
            }
        }
    }

    // Logic to allow selecting rook when castling
    private fun remapEndIfCastlingOntoRook(end: Position): Position {
        val kingPos = when (game.turn) {
            Player.WHITE -> game.whiteKingPos
            Player.BLACK -> game.blackKingPos
            else -> Position.NULL
        }
        val king = game.board.get(kingPos)
        if (game.selectedPosition == kingPos &&
            !king.hasMoved &&
            !game.board.get(end).hasMoved
        ) {
            if (end.file == File.H) {
                return Position(rank = end.rank, file = File.G)
            } else if (end.file == File.A) {
                return Position(rank = end.rank, file = File.C)
            }
        }
        return end
    }

    private fun onTouchCallback(v: View, event: MotionEvent, pos: Position): Boolean {
        val piece = game.board.get(pos)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val newPos = remapEndIfCastlingOntoRook(pos)
                game.select(newPos)
                debugTextView.text = "selected $newPos: ${game.board.get(pos)}" // TODO: Debug; remove
            }
            MotionEvent.ACTION_MOVE -> {
                if (game.isPieceSelected && pos == game.selectedPosition) {
                    // First, set this view as partially invisible
                    v.alpha = selectedPieceAlpha

                    // Create a new view to follow around the player's finger
                    if (followerView == null) {
                        followerView = createFollowerView(v, piece)
                    }

                    // Follow the player's finger
                    followerView!!.x = v.x + event.x + boardGridLayout.x - followerViewRadius
                    followerView!!.y = v.y + event.y + boardGridLayout.y - followerViewRadius

                    // Detect if the follower view has moved outside the board grid layout and reset if so
                    if (
                        followerView!!.x + followerViewRadius < boardGridLayout.x ||
                        followerView!!.x + followerViewRadius > (boardGridLayout.x + boardGridLayout.width) ||
                        followerView!!.y + followerViewRadius < boardGridLayout.y ||
                        followerView!!.y + followerViewRadius > (boardGridLayout.y + boardGridLayout.height)
                    ) {
                        getView(pos)?.let { resetViewDrawable(it, pos) }
                        destroyFollowerView()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                v.alpha = 1.0f
                // Determine board position from current x/y coordinates
                var newPos = getPositionFromCoordinates(v, event)
                newPos = remapEndIfCastlingOntoRook(newPos)

                destroyFollowerView()

                println("sel pos: ${game.selectedPosition}")

                // Attempt to move to the new position
                val moveResult = game.move(newPos)
                println("move result: $moveResult")

                // If the move was unsuccessful because of the following, reset:
                if (moveResult == SAME_START_END_POS ||
                    moveResult == MOVE_ONTO_OWN_PIECE ||
                    moveResult == MOVE_ILLEGAL) {
                    if (game.selectedPosition.valid)
                        resetViewDrawable(getView(game.selectedPosition)!!, game.selectedPiece)
                } else if (moveResult == MOVE_GOOD) {
                    debugTextView.text = "moved to $newPos: ${game.board.get(newPos)}"
                }
            }
        }
        return true
    }

    private fun onCheckmateCallback(player: Player) {
        dialogBuilder.setTitle(getString(R.string.checkmate_dialog_title))
        val text = StringBuilder()
        text.append(player.opponent().str).append(" ").append(getString(R.string.player_victory_text))
        dialogBuilder.setMessage(text)
        dialogBuilder.setPositiveButton(R.string.new_game) { dialog, which ->
            resetGame()
        }
        dialogBuilder.show()
    }
}