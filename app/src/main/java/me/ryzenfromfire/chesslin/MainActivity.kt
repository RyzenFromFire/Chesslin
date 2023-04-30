package me.ryzenfromfire.chesslin

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
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
    private lateinit var boardViewArray: Array<Array<TextView>>
    private lateinit var game: ChessGame

    private lateinit var turnTextView: TextView

    // TODO: Remove; for debug only
    private lateinit var debugTextView: TextView

    private var followerView: TextView? = null
    private val followerShadowScalar = 1.5
    private var followerShadowSize = 0
    private var followerViewRadius = 0.0
    private lateinit var mainLayout: ConstraintLayout

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
                val pos = Position(file = File[file], rank = rank)

                // Create view using helper function
                tv = createChessPieceView(game.board.get(pos))

//                tv.setOnClickListener {
//                    print("onclick listener called, followerPieceView is ")
//                    // If not dragging
//                    if (followerPieceView == null) {
//                        println("null")
//                        game.select(pos)
//                        debugTextView.text = "selected $pos" // TODO: Debug; remove
//                    } else println("not null")
//                }

                tv.setOnTouchListener { v, event ->
                    val piece = game.board.get(pos)
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            game.select(pos)
                            debugTextView.text = "selected $pos" // TODO: Debug; remove
                            getPositionFromCoordinates(v, event) // for debug
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (game.isPieceSelected) {
                                // First, set this textview as invisible
                                setColor(v as TextView, Player.NONE)

                                // Create a new view to follow around the player's finger
                                if (followerView == null) {
                                    followerView = createFollowerView(v, piece)
                                }

                                // Follow the player's finger
                                followerView!!.x = v.x + event.x + boardGridLayout.x - followerViewRadius.toFloat()
                                followerView!!.y = v.y + event.y + boardGridLayout.y - followerViewRadius.toFloat()
                            }
                        }
                        MotionEvent.ACTION_UP -> {
                            // Determine board position from current x/y coordinates and select it
                            val newPos = getPositionFromCoordinates(v, event)

                            if (followerView != null) {
                                // Destroy and reset for next use
                                setColor(followerView!!, Player.NONE)
                                followerView!!.background = null
                                followerView = null
                            }

                            // If an invalid position is detected,
                            // or the ending position is the same as the starting position,
                            // basically return the piece to its original position
                            // Otherwise, select the new position.
                            if (newPos.valid && newPos != pos) {
                                game.select(newPos)
                            } else if (!newPos.valid || newPos == pos) {
                                game.select(pos)
                                setColor(v as TextView, piece.player)
                            }
                            debugTextView.text = "selected $pos" // TODO: Debug; remove
                        }
                    }
                    true
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

        game.onTurnChangedListener = {
            if (game.turn == Player.BLACK)
                turnTextView.text = getString(R.string.turn_black)
            else turnTextView.text = getString(R.string.turn_white)
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
        // https://stackoverflow.com/questions/45608049/how-to-make-a-circular-drawable-with-stroke-programmatically/45608694
        val gd = GradientDrawable()
        gd.color = ContextCompat.getColorStateList(this, R.color.shadow)
        gd.shape = GradientDrawable.OVAL
        tv.background = gd
        tv.background.bounds = Rect(followerShadowSize, followerShadowSize, followerShadowSize, followerShadowSize)

        setColor(tv, piece.player)
        followerViewRadius = followerShadowSize.toDouble() / 2.0
        tv.x = v.x + boardGridLayout.x - followerViewRadius.toFloat()
        tv.y = v.y + boardGridLayout.y - followerViewRadius.toFloat()
        return tv
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

        return Position(rank = newRank, file = File[newFileIdx])
    }
}