package me.ryzenfromfire.chesslin

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.gridlayout.widget.GridLayout
import com.google.android.material.shape.MaterialShapeDrawable
import me.ryzenfromfire.chesslin.ChessBoard.Companion.NUM_RANKS_FILES
import me.ryzenfromfire.chesslin.ChessGame.Player
import me.ryzenfromfire.chesslin.ChessBoard.Position
import me.ryzenfromfire.chesslin.ChessBoard.File

class MainActivity : AppCompatActivity() {
    private lateinit var boardGridLayout: GridLayout
    private lateinit var boardViewArray: Array<Array<TextView>>
    private lateinit var game: ChessGame

    private lateinit var turnTextView: TextView

    // TODO: Remove; for debug only
    private lateinit var debugTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        game = ChessGame()

        boardGridLayout = findViewById(R.id.boardGridLayout)
        boardGridLayout.removeAllViews()
        boardGridLayout.columnCount = NUM_RANKS_FILES
        boardGridLayout.rowCount = NUM_RANKS_FILES

        boardViewArray = Array(NUM_RANKS_FILES) { Array(NUM_RANKS_FILES) { TextView(this) } }

        turnTextView = findViewById(R.id.turnTextView)
        debugTextView = findViewById(R.id.debugView)

        // Dynamic GridLayout Generation adapted from:
        // https://stackoverflow.com/questions/14728157/dynamic-gridlayout
        var tv: TextView

        // loop must go from highest rank to lowest and across each file,
        // so the positions are added to the grid layout in the correct order
        for (rank in NUM_RANKS_FILES downTo 1) {
            for (file in 0 until NUM_RANKS_FILES) { // TODO: replace with File.values() for better semantics?
                // val str = "${File.values()[file].str}${rank}"
                val pos = Position(file = File[file], rank = rank)
                tv = TextView(this)
                tv.text = game.board.get(pos).type.str
//                tv.gravity = Gravity.CENTER // Center the text in the square
                tv.gravity = Gravity.FILL
                tv.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                tv.textSize = 24F
                tv.typeface = Typeface.MONOSPACE // https://stackoverflow.com/questions/12128331/how-to-change-fontfamily-of-textview-in-android
                tv.setOnClickListener {
                    game.select(pos)
                    debugTextView.text = "selected $pos" // TODO: Debug; remove
                }
                setColor(tv, game.board.get(pos).player)
                // boardViewArray[rank - 1][file] = tv
                setView(pos, tv)

                // Border for debugging
                // https://stackoverflow.com/a/62720394
//                val shapeDrawable = MaterialShapeDrawable()
//                shapeDrawable.fillColor = ContextCompat.getColorStateList(this,android.R.color.transparent)
//                shapeDrawable.setStroke(1.0f, ContextCompat.getColor(this,R.color.black))
//                tv.background = shapeDrawable

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
}