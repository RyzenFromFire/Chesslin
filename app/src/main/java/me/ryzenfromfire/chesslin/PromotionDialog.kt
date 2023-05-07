package me.ryzenfromfire.chesslin

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import me.ryzenfromfire.chesslin.ChessPiece.PieceType
import me.ryzenfromfire.chesslin.ChessPiece.PieceType.*

class PromotionDialog(context: Context): Dialog(context) {
    var onPromotionListener: ((PieceType) -> Unit)? = null

    private lateinit var queenView: SquareImageView
    private lateinit var rookView: SquareImageView
    private lateinit var knightView: SquareImageView
    private lateinit var bishopView: SquareImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.promotion_dialog)
        queenView = findViewById(R.id.promotionViewQueen)
        rookView = findViewById(R.id.promotionViewRook)
        knightView = findViewById(R.id.promotionViewKnight)
        bishopView = findViewById(R.id.promotionViewBishop)

        queenView.setOnClickListener {
            onPromotionListener?.invoke(QUEEN)
        }

        rookView.setOnClickListener {
            onPromotionListener?.invoke(ROOK)
        }

        knightView.setOnClickListener {
            onPromotionListener?.invoke(KNIGHT)
        }

        bishopView.setOnClickListener {
            onPromotionListener?.invoke(BISHOP)
        }
    }
}