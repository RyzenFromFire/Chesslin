package me.ryzenfromfire.chesslin

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import me.ryzenfromfire.chesslin.ChessPiece.PieceType
import me.ryzenfromfire.chesslin.ChessPiece.PieceType.*
import me.ryzenfromfire.chesslin.ChessGame.Player

class PromotionDialog(context: Context): Dialog(context) {
    var onPromotionListener: ((PieceType) -> Unit)? = null

    private lateinit var queenView: SquareImageView
    private lateinit var rookView: SquareImageView
    private lateinit var knightView: SquareImageView
    private lateinit var bishopView: SquareImageView
    private lateinit var promotionTextView1: TextView
    private lateinit var promotionTextView2: TextView
    private var player = Player.WHITE
    private var flipped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.promotion_dialog)
        this.setCancelable(false)

        promotionTextView1 = findViewById(R.id.promotionText)
        promotionTextView1.text = context.getString(R.string.promotion_text_1)

        promotionTextView2 = findViewById(R.id.promotionText2)
        promotionTextView2.text = context.getString(R.string.promotion_text_2)

        queenView = findViewById(R.id.promotionViewQueen)
        rookView = findViewById(R.id.promotionViewRook)
        knightView = findViewById(R.id.promotionViewKnight)
        bishopView = findViewById(R.id.promotionViewBishop)

        queenView.setOnClickListener {
            onPromotionListener?.invoke(QUEEN)
            dismiss()
        }

        rookView.setOnClickListener {
            onPromotionListener?.invoke(ROOK)
            dismiss()
        }

        knightView.setOnClickListener {
            onPromotionListener?.invoke(KNIGHT)
            dismiss()
        }

        bishopView.setOnClickListener {
            onPromotionListener?.invoke(BISHOP)
            dismiss()
        }

        when (player) {
            Player.BLACK -> {
                val view = arrayOf(queenView, rookView, knightView, bishopView)
                val id = arrayOf(
                    R.drawable.chess_qdt45,
                    R.drawable.chess_rdt45,
                    R.drawable.chess_ndt45,
                    R.drawable.chess_bdt45,
                )
                for (i in 0..3) {
                    if (flipped) {
                        val matrix = Matrix()
                        matrix.preScale(1.0f, -1.0f)
                        val dwg = AppCompatResources.getDrawable(context, id[i])
                        val bmp = Bitmap.createBitmap(dwg!!.toBitmap(), 0, 0, dwg.intrinsicWidth, dwg.intrinsicHeight, matrix, true)
                        view[i].setImageBitmap(bmp)
                    } else {
                        view[i].setImageResource(id[i])
                    }
                }
            }
            // Use Player.WHITE as default
            else -> {
                queenView.setImageResource(R.drawable.chess_qlt45)
                rookView.setImageResource(R.drawable.chess_rlt45)
                knightView.setImageResource(R.drawable.chess_nlt45)
                bishopView.setImageResource(R.drawable.chess_blt45)
            }
        }
    }

    /**
     * Must be called before show() to display the proper images.
     */
    fun setPlayer(player: Player, boardFlipped: Boolean = false) {
        this.player = player
        this.flipped = boardFlipped
        println("promotion dialog player set: $player, $flipped")
    }
}