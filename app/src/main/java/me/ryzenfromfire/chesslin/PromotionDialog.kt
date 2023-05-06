package me.ryzenfromfire.chesslin

import android.app.Dialog
import android.content.Context
import android.os.Bundle

class PromotionDialog(context: Context, position: ChessBoard.Position): Dialog(context) {
    var onPromotionListener: (() -> ChessPiece.PieceType)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.promotion_dialog)
    }
}