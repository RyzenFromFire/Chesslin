package me.ryzenfromfire.chesslin

import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout


class MainActivity : AppCompatActivity() {
    private lateinit var boardGridLayout: GridLayout
    companion object {
        const val NUM_RANKS_FILES = 8
    }
    enum class ChessFile(val str: String, val index: Int) {
        A("a", 0),
        B("b", 1),
        C("c", 2),
        D("d", 3),
        E("e", 4),
        F("f", 5),
        G("g", 6),
        H("h", 7)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        boardGridLayout = findViewById(R.id.boardGridLayout)

        boardGridLayout.removeAllViews()
        boardGridLayout.columnCount = NUM_RANKS_FILES
        boardGridLayout.rowCount = NUM_RANKS_FILES

        // Dynamic GridLayout Generation adapted from:
        // https://stackoverflow.com/questions/14728157/dynamic-gridlayout
        // TODO: Store a reference to each textview in an array?
        var tv: TextView
        for (rank in NUM_RANKS_FILES downTo 1) {
            for (file in 0 until NUM_RANKS_FILES) {
                tv = TextView(this)
                tv.text = "${ChessFile.values()[file].str}$rank"

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

//        var i = 0
//        var c = 0
//        var r = 0
//        while (i < 64) {
//            if (c == column) {
//                c = 0
//                r++
//            }
//            val oImageView = ImageView(this)
//            oImageView.setImageResource(R.drawable.ic_launcher)
//            oImageView.layoutParams = GridLayout.LayoutParams(100, 100)
//            var rowSpan: GridLayout.Spec? = GridLayout.spec(GridLayout.UNDEFINED, 1)
//            var colspan: GridLayout.Spec? = GridLayout.spec(GridLayout.UNDEFINED, 1)
//            if (r == 0 && c == 0) {
//                Log.e("", "spec")
//                colspan = GridLayout.spec(GridLayout.UNDEFINED, 2)
//                rowSpan = GridLayout.spec(GridLayout.UNDEFINED, 2)
//            }
//            val gridParam: GridLayout.LayoutParams = GridLayout.LayoutParams(
//                rowSpan, colspan
//            )
//            gridLayout.addView(oImageView, gridParam)
//            i++
//            c++
//        }

    }
}