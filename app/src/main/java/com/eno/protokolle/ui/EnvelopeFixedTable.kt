/**
 * Alternativer Renderer für FixedHeaderTableLayout, der eine einzelne UiTable darstellt.
 */
package com.eno.protokolle.ui

import android.content.Context
import android.graphics.Paint
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.eno.protokolle.R
import com.eno.protokolle.newmodel.UiTable
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderSubTableLayout
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableLayout
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableRow
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Renderer für Zardozz FixedHeaderTableLayout (parallel zum alten Adapter).
 * Baut die 4 Subtables programmatically und hängt sie in der korrekten Reihenfolge an.
 *
 * Relevante Wiki-Punkte:
 * - Subtables programmatically erzeugen (kein XML) und gleiche Zeilen/Spalten-Kardinalität beachten
 * - addViews(main, columnHeader, rowHeader, corner) – Parameterreihenfolge ist wichtig!
 */
class EnvelopeFixedTable(
    private val ctx: Context,
    private val textSizeSp: Float = 14f,
    rowHeightDp: Int = 40,
    private val padHDp: Int = 8,
    private val quarterProvider: () -> String = { "" }
) {

    // DP → PX
    private val dm = ctx.resources.displayMetrics
    private fun dp(v: Int) = (v * (if (dm.density > 0) dm.density else 1f)).roundToInt()
    private val rowH = dp(rowHeightDp)
    private val padH = dp(padHDp)
    private val minEmptyColPx = dp(40)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = textSizeSp * (if (dm.scaledDensity > 0) dm.scaledDensity else 1f)
    }
    private val placeholderColor = ContextCompat.getColor(ctx, android.R.color.darker_gray)
    private val textColor = ContextCompat.getColor(ctx, android.R.color.black)

    fun renderInto(container: FixedHeaderTableLayout, table: UiTable) {
        container.removeAllViews()

        // Daten aufbereiten
        val headerRows: List<List<String>> = table.header
        val bodyRows: List<List<String>> = table.rows

        val maxColsHeader = headerRows.maxOfOrNull { it.size } ?: 0
        val maxColsBody   = bodyRows.maxOfOrNull { it.size } ?: 0
        val totalCols = maxOf(maxColsHeader, maxColsBody)

        val colWidthsPx = computeColumnWidthsPx(table, totalCols)

        // Subtables
        val mainTable = FixedHeaderSubTableLayout(ctx)
        val columnHeaderTable = FixedHeaderSubTableLayout(ctx)
        val rowHeaderTable = FixedHeaderSubTableLayout(ctx)
        val cornerTable = FixedHeaderSubTableLayout(ctx)

        // ---- Column Header (k Headerzeilen, N-1 Zellen ab Spalte 1) ----
        for (hr in headerRows) {
            val tr = FixedHeaderTableRow(ctx)
            for (c in 1 until totalCols) {
                val t = hr.getOrNull(c).orEmpty()
                tr.addView(makeHeaderCell(t, colWidthsPx[c]))
            }
            columnHeaderTable.addView(tr)
        }

        // ---- Row Header (M Zeilen, 1 Zelle aus Spalte 0) ----
        for (r in bodyRows.indices) {
            val tr = FixedHeaderTableRow(ctx)
            val t = bodyRows[r].getOrNull(0).orEmpty()
            tr.addView(makeRowHeaderCell(t, colWidthsPx[0]))
            rowHeaderTable.addView(tr)
        }

        // ---- Main (M Zeilen, N-1 Zellen ab Spalte 1) ----
        for (r in bodyRows.indices) {
            val tr = FixedHeaderTableRow(ctx)
            for (c in 1 until totalCols) {
                val value = bodyRows[r].getOrNull(c).orEmpty()
                val type = table.cellTypes?.getOrNull(r)?.getOrNull(c)
                tr.addView(makeBodyCell(value, type, colWidthsPx[c]))
            }
            mainTable.addView(tr)
        }

        // ---- Corner (k Zeilen, 1 Zelle aus Header-Spalte 0) ----
        for (hr in headerRows) {
            val tr = FixedHeaderTableRow(ctx)
            tr.addView(makeCornerCell(hr.getOrNull(0).orEmpty(), colWidthsPx[0]))
            cornerTable.addView(tr)
        }

        // Wichtig: Reihenfolge! (Wiki)
        container.addViews(mainTable, columnHeaderTable, rowHeaderTable, cornerTable)
        // calculatePanScale(...) lasse ich bewusst weg (API-Signatur variiert je Version)
    }

    // ----- Cell-Fabriken -----

    private fun makeHeaderCell(text: String, w: Int) = baseCell(text, w).apply {
        setBackgroundResource(R.drawable.bg_header_cell_black)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun makeRowHeaderCell(text: String, w: Int) = baseCell(text, w).apply {
        setBackgroundResource(R.drawable.bg_header_cell_black)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun makeCornerCell(text: String, w: Int) = baseCell(text, w).apply {
        setBackgroundResource(R.drawable.bg_header_cell_black)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun makeBodyCell(value: String, type: String?, w: Int): TextView {
        val notInUse = type.equals("NotInUse", ignoreCase = true)
        val editable = type != null && !notInUse
        val placeholder = if (!type.isNullOrBlank() && !notInUse) type else ""
        val hasValue = value.isNotBlank()
        val displayText = when {
            hasValue -> value
            placeholder.isNotBlank() -> placeholder
            else -> ""
        }
        val usePlaceholderColor = !hasValue && placeholder.isNotBlank()

        return baseCell(displayText, w).apply {
            val initialBg = when {
                notInUse -> R.drawable.bg_cell_not_in_use
                hasValue -> R.drawable.bg_cell_selected
                else -> R.drawable.bg_cell_black
            }
            setBackgroundResource(initialBg)
            setTextColor(if (usePlaceholderColor) placeholderColor else textColor)

            if (editable) {
                tag = BodyCellState(placeholder = placeholder, filled = hasValue)
                setOnClickListener { toggleQuarterValue(this) }
            }
        }
    }

    private fun baseCell(text: String, width: Int) =
        TextView(ctx).apply {
            this.text = text
            textSize = textSizeSp
            setPadding(padH, 0, padH, 0)
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
            minHeight = rowH
            layoutParams = ViewGroup.LayoutParams(
                width,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

    private fun computeColumnWidthsPx(table: UiTable, totalCols: Int): List<Int> =
        List(totalCols) { col ->
            val hintChars = table.colWidths?.getOrNull(col)
            hintChars?.let { measureWidthFromChars(it) } ?: measureColumnPx(table, col)
        }

    private fun measureWidthFromChars(chars: Int): Int {
        if (chars <= 0) return minEmptyColPx
        val sample = "0".repeat(chars.coerceAtLeast(1))
        val width = paint.measureText(sample)
        return (width + padH * 2).roundToInt()
    }

    private fun measureColumnPx(table: UiTable, col: Int): Int {
        var maxPx = 0f
        table.header.forEach { row ->
            maxPx = max(maxPx, paint.measureText(row.getOrNull(col).orEmpty()))
        }
        table.rows.forEachIndexed { rowIndex, row ->
            maxPx = max(maxPx, paint.measureText(row.getOrNull(col).orEmpty()))
            val typeText = table.cellTypes?.getOrNull(rowIndex)?.getOrNull(col)
            if (!typeText.isNullOrBlank()) {
                maxPx = max(maxPx, paint.measureText(typeText))
            }
        }
        val withPad = if (maxPx > 0f) maxPx + padH * 2 else minEmptyColPx.toFloat()
        return withPad.roundToInt()
    }

    private fun toggleQuarterValue(tv: TextView) {
        val state = tv.tag as? BodyCellState ?: return
        if (state.filled) {
            tv.text = state.placeholder
            tv.setTextColor(if (state.placeholder.isNotBlank()) placeholderColor else textColor)
            tv.setBackgroundResource(R.drawable.bg_cell_black)
            state.filled = false
        } else {
            val q = quarterProvider()
            if (q.isNotBlank()) {
                tv.text = q
                tv.setTextColor(textColor)
                tv.setBackgroundResource(R.drawable.bg_cell_selected)
                state.filled = true
            }
        }
    }

    private data class BodyCellState(
        val placeholder: String,
        var filled: Boolean
    )
}
