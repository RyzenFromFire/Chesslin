/**
 * Chess Piece Images from: https://commons.wikimedia.org/wiki/Template:SVG_chess_pieces
 * Licensed Under CC3: https://creativecommons.org/licenses/by-sa/3.0/deed.en
 * Author: Cburnett
 */

package me.ryzenfromfire.chesslin

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.gridlayout.widget.GridLayout
import me.ryzenfromfire.chesslin.ChessBoard.Companion.NUM_RANKS_FILES
import me.ryzenfromfire.chesslin.ChessGame.Player
import me.ryzenfromfire.chesslin.ChessBoard.Position
import me.ryzenfromfire.chesslin.ChessBoard.File
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var boardGridLayout: GridLayout
    private lateinit var boardViewArray: Array<Array<TextView>> // TODO: Switch to ImageView
    private lateinit var game: ChessGame

    private lateinit var turnTextView: TextView

    // TODO: Remove; for debug only
    private lateinit var debugTextView: TextView
    private lateinit var resetButton: Button

    private var followerView: TextView? = null
    private val followerShadowScalar = 1.5
    private var followerShadowSize = 0
    private var followerViewRadius = 0.0
    private lateinit var mainLayout: ConstraintLayout
    private val selectEmptyShadowScalar = 0.5
    private val selectPieceShadowScalar = 1.0
    private var shadowedPositions = mutableListOf<Position>()

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

        followerShadowSize = ((boardGridLayout.width / NUM_RANKS_FILES) * followerShadowScalar).roundToInt()
        followerViewRadius = followerShadowSize / 2.0

        boardViewArray = Array(NUM_RANKS_FILES) { Array(NUM_RANKS_FILES) { TextView(this) } }

        turnTextView = findViewById(R.id.turnTextView)
        debugTextView = findViewById(R.id.debugView)

        // Dynamic GridLayout Generation adapted from:
        // https://stackoverflow.com/questions/14728157/dynamic-gridlayout
        var tv: TextView

        // loop must go from highest rank to lowest and across each file,
        // so the positions are added to the grid layout in the correct order
        for (rank in NUM_RANKS_FILES downTo 1) {
            for (file in 0 until NUM_RANKS_FILES) {
                val pos = Position(file = File[file]!!, rank = rank)

                // Create view using helper function
                tv = createChessPieceView(game.board.get(pos))

                tv.setOnTouchListener { v, event ->
                    viewOnTouchListener(v, event, pos)
                }

                setColor(tv, game.board.get(pos).player)
                setView(pos, tv)

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
                params.setGravity(Gravity.FILL) // Make the view take the entire grid square
                boardGridLayout.addView(tv, params)
            }
        }

        game.board.onSet = { position: Position, piece: ChessPiece ->
            if (position.valid) {
                val view = boardViewArray[position.rank - 1][position.file.index]
                view.text = piece.type.str
                setColor(view, piece.player)
            }
        }

        game.onSelectListener = {
            for (position in shadowedPositions) {
                removePositionShadow(position)
            }
            print("positions: [ ")
            for (position in game.movablePositions) {
                addPositionShadow(position)
                print("$position ")
            }
            println("]")
            shadowedPositions = game.movablePositions
        }

        game.onMoveListener = {
            // remove position shadows. will not select any new positions since game.movablePositions is an empty list.
            game.onSelectListener?.invoke()
            switchTurn()
        }

        // For Debug
        resetButton = findViewById(R.id.resetButton)
        resetButton.setOnClickListener {
            this.recreate()
        }
    }

    private fun setColor(tv: TextView, player: Player) {
        when (player) {
            Player.WHITE -> tv.setTextColor(getColor(R.color.white))
            Player.BLACK -> tv.setTextColor(getColor(R.color.black))
            else -> tv.setTextColor(getColor(R.color.transparent))
        }
    }

    private fun getView(position: Position): TextView? {
        return if (position.valid)
            boardViewArray[position.rank - 1][position.file.index]
        else null
    }

    private fun setView(position: Position, view: TextView): Boolean {
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
            v.background = createShadowDrawable()
            val size = if (game.board.isOccupied(position)) {
//                println("SELECT OCCUPIED")
                (selectPieceShadowScalar * v.width).roundToInt()
            } else {
//                println("SELECT EMPTY")
                (selectEmptyShadowScalar * v.width).roundToInt()
            }
//            println(size)
            v.background.bounds = Rect(size, size, size, size)
            true
        } else false
    }

    private fun removePositionShadow(position: Position) {
        boardViewArray[position.rank - 1][position.file.index].background = null
    }

    private fun createChessPieceView(piece: ChessPiece): TextView {
        val tv = TextView(this)
        tv.text = piece.type.str
        // tv.gravity = Gravity.CENTER // Center the text in the square
        tv.gravity = Gravity.FILL
        tv.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        tv.textSize = 24F
        tv.typeface = Typeface.MONOSPACE // https://stackoverflow.com/questions/12128331/how-to-change-fontfamily-of-textview-in-android
        return tv
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFollowerView(v: View, piece: ChessPiece): TextView {
        val tv = createChessPieceView(piece)
        mainLayout.addView(tv)
        followerShadowSize = (followerShadowScalar * v.width).roundToInt()
        tv.height = followerShadowSize
        tv.width = followerShadowSize
        tv.gravity = Gravity.CENTER

        // Creating backdrop shadow
        val gd = createShadowDrawable()
        tv.background = gd
        tv.background.bounds = Rect(followerShadowSize, followerShadowSize, followerShadowSize, followerShadowSize)

        setColor(tv, piece.player)
        followerViewRadius = followerShadowSize.toDouble() / 2.0
        tv.x = v.x + boardGridLayout.x - followerViewRadius.toFloat()
        tv.y = v.y + boardGridLayout.y - followerViewRadius.toFloat()
        return tv
    }

    private fun destroyFollowerView() {
        if (followerView != null) {
            // Destroy and reset for next use
            setColor(followerView!!, Player.NONE)
            followerView!!.background = null
            followerView = null
        }
    }

    private fun createShadowDrawable(color: Int = R.color.shadow): GradientDrawable {
        // Creating backdrop shadow
        // https://stackoverflow.com/questions/45608049/how-to-make-a-circular-drawable-with-stroke-programmatically/45608694
        val gd = GradientDrawable()
        gd.color = ContextCompat.getColorStateList(this, color)
        gd.shape = GradientDrawable.OVAL
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

    private fun viewOnTouchListener(v: View, event: MotionEvent, pos: Position): Boolean {
        val piece = game.board.get(pos)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                game.select(pos)
                debugTextView.text = "selected $pos: ${game.board.get(pos)}" // TODO: Debug; remove
            }
            MotionEvent.ACTION_MOVE -> {
                if (game.isPieceSelected && pos == game.selectedPosition) {
                    // First, set this textview as invisible
                    setColor(v as TextView, Player.NONE)

                    // Create a new view to follow around the player's finger
                    if (followerView == null) {
                        followerView = createFollowerView(v, piece)
                    }

                    // Follow the player's finger
                    followerView!!.x = v.x + event.x + boardGridLayout.x - followerViewRadius.toFloat()
                    followerView!!.y = v.y + event.y + boardGridLayout.y - followerViewRadius.toFloat()

                    // Detect if the follower view has moved outside the board grid layout and reset if so
                    if (
                        followerView!!.x + followerViewRadius.toFloat() < boardGridLayout.x ||
                        followerView!!.x + followerViewRadius.toFloat() > (boardGridLayout.x + boardGridLayout.width) ||
                        followerView!!.y + followerViewRadius.toFloat() < boardGridLayout.y ||
                        followerView!!.y + followerViewRadius.toFloat() > (boardGridLayout.y + boardGridLayout.height)
                    ) {
                        setColor(getView(pos) as TextView, game.selectedPiece.player)
                        destroyFollowerView()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                // Determine board position from current x/y coordinates
                val newPos = getPositionFromCoordinates(v, event)

                destroyFollowerView()

                println("sel pos: ${game.selectedPosition}")

                // Attempt to move to the new position
                val moved = game.move(newPos)
                println("moved: $moved")

                // If the move was unsuccessful because of either of the following, reset:
                // The player attempted to move the same position
                // The player attempted to move onto one of their own pieces
                if (!moved && (newPos == game.selectedPosition || game.board.get(newPos).player == game.turn)) {
                    if (game.selectedPosition.valid)
                        setColor(getView(game.selectedPosition) as TextView, game.selectedPiece.player)
                } else if (moved) {
                    debugTextView.text = "moved to $newPos: ${game.board.get(newPos)}"
                }
            }
        }
        return true
    }
}