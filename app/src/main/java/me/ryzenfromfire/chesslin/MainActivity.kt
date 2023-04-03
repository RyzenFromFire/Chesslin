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

class MainActivity : AppCompatActivity() {
    private lateinit var boardGridLayout: GridLayout
    private lateinit var boardViewArray: Array<Array<TextView>>
    private lateinit var game: ChessGame

    // TODO: Implement actual move system
    private lateinit var moveEditText: EditText
    private lateinit var moveSubmitButton: Button
    private lateinit var turnTextView: TextView

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

        // Dynamic GridLayout Generation adapted from:
        // https://stackoverflow.com/questions/14728157/dynamic-gridlayout
        var tv: TextView
        for (rank in NUM_RANKS_FILES downTo 1) {
            for (file in 0 until NUM_RANKS_FILES) {
                val str = "${ChessBoard.File.values()[file].str}${rank}"
                tv = TextView(this)
                tv.text = game.board.get(str).piece.str
                setColor(tv, game.board.get(str).player)
                boardViewArray[rank - 1][file] = tv

                // Create the row and column specifications
                // 'start' = UNDEFINED, that's fine.
                // 'size' = 1 always, since we aren't using >1 cell for each square.
                // 'alignment' = FILL since we want to distribute the cells evenly over the grid.
                // 'weight' = 1 is needed to determine the aforementioned distribution.
                val rowSpan: GridLayout.Spec? = GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL, 1F)
                val colSpan: GridLayout.Spec? = GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL, 1F)
                val params = GridLayout.LayoutParams(rowSpan, colSpan)
                params.setGravity(Gravity.CENTER) // Center the text in the square
                boardGridLayout.addView(tv, params)
            }
        }

        moveEditText = findViewById(R.id.moveEditText)
        moveSubmitButton = findViewById(R.id.moveSubmitButton)
        moveSubmitButton.setOnClickListener {
            game.move(moveEditText.text.toString())
            if (game.turn == Player.BLACK)
                turnTextView.text = getString(R.string.turn_black)
            else turnTextView.text = getString(R.string.turn_white)
        }

        game.board.onSet = { rank0: Int, file: ChessBoard.File, player: Player, piece: ChessPiece.Piece ->
            val view = boardViewArray[rank0][file.index]
            view.text = piece.str
            setColor(view, player)
        }
    }

    private fun setColor(tv: TextView, player: Player) {
        if (player == Player.WHITE)
            tv.setTextColor(getColor(R.color.white))
        else if (player == Player.BLACK)
            tv.setTextColor(getColor(R.color.black))
    }
}