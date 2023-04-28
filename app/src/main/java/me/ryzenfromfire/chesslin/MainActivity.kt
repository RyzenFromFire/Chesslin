package me.ryzenfromfire.chesslin

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout
import me.ryzenfromfire.chesslin.ChessBoard.Companion.NUM_RANKS_FILES
import me.ryzenfromfire.chesslin.ChessGame.Player
import me.ryzenfromfire.chesslin.ChessBoard.Position
import me.ryzenfromfire.chesslin.ChessBoard.File

class MainActivity : AppCompatActivity() {
    private lateinit var boardGridLayout: GridLayout
    private lateinit var boardViewArray: Array<Array<TextView>>
    private lateinit var game: ChessGame

    // TODO: Implement actual move system
    private lateinit var moveEditText: EditText
    private lateinit var moveSubmitButton: Button
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
                tv.gravity = Gravity.CENTER // Center the text in the square
                tv.setOnClickListener {
                    game.select(pos)
                    debugTextView.text = "selected $pos" // TODO: Debug; remove
                }
                setColor(tv, game.board.get(pos).player)
                // boardViewArray[rank - 1][file] = tv
                setView(pos, tv)

                // Create the row and column specifications
                // 'start' = UNDEFINED, that's fine.
                // 'size' = 1 always, since we aren't using >1 cell for each square.
                // 'alignment' = FILL since we want to distribute the cells evenly over the grid.
                // 'weight' = 1 is needed to determine the aforementioned distribution.
                val rowSpan: GridLayout.Spec? = GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL, 1F)
                val colSpan: GridLayout.Spec? = GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL, 1F)
                val params = GridLayout.LayoutParams(rowSpan, colSpan)
                params.setGravity(Gravity.FILL) // Make the view take the entire grid square
                boardGridLayout.addView(tv, params)
            }
        }

        moveEditText = findViewById(R.id.moveEditText)
        moveSubmitButton = findViewById(R.id.moveSubmitButton)
        moveSubmitButton.setOnClickListener {
            game.moveOld(moveEditText.text.toString())
            if (game.turn == Player.BLACK)
                turnTextView.text = getString(R.string.turn_black)
            else turnTextView.text = getString(R.string.turn_white)
        }

        game.board.onSet = { position: Position, piece: ChessPiece ->
            if (position.valid) {
                val view = boardViewArray[position.rank - 1][position.file.index]
                view.text = piece.type.str
                setColor(view, piece.player)
            }
        }
    }

    private fun setColor(tv: TextView, player: Player) {
        if (player == Player.WHITE)
            tv.setTextColor(getColor(R.color.white))
        else if (player == Player.BLACK)
            tv.setTextColor(getColor(R.color.black))
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