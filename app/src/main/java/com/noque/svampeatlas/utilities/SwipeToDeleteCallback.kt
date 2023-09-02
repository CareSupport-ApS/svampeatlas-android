package com.noque.svampeatlas.utilities

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.dpToPx

class SwipeToDeleteCallback(
    private val onDelete: (RecyclerView.ViewHolder) -> Unit,
    private val context: Context,
    private val resources: Resources
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    override fun onChildDrawOver(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder?,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDrawOver(
            c,
            recyclerView,
            viewHolder,
            dX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val rightMargin = 32.dpToPx(context)
        val iconSize = 16.dpToPx(context)
        val icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_delete_black_24dp, null)
        val background = ColorDrawable(ResourcesCompat.getColor(resources, R.color.colorRed, null))

        icon?.bounds = Rect(
            viewHolder.itemView.right - iconSize * 2 - rightMargin,
            viewHolder.itemView.top + (viewHolder.itemView.height / 2) - iconSize,
            viewHolder.itemView.right - rightMargin,
            viewHolder.itemView.bottom - (viewHolder.itemView.height / 2) + iconSize
        )


        background.bounds = Rect(
            viewHolder.itemView.right + dX.toInt(),
            viewHolder.itemView.top + resources.getDimension(R.dimen.item_mushroom_top_margin).toInt(),
            viewHolder.itemView.right,
            viewHolder.itemView.bottom - resources.getDimension(R.dimen.item_mushroom_bottom_margin).toInt()
        )

        background.draw(c)
        icon?.draw(c)

        super.onChildDraw(
            c,
            recyclerView,
            viewHolder,
            dX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }


    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onDelete(viewHolder)
    }
}