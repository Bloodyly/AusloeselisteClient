
/**
 * Renderer, der mehrere UiTable-Sektionen in einem gemeinsamen FixedHeaderTableLayout stapelt.
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
import kotlin.math.max
import kotlin.math.roundToInt

data class UiTableSection(val title: String?, val table: UiTable)

/**
 * Renders multiple UiTables (e.g. Melder + Hardware) into a single FixedHeaderTableLayout
 * using stacked sections in the body with a single fixed column header.
 *
 * addViews order is crucial: (main, columnHeader, rowHeader, corner)
 */
class MultiSectionFixedTable(
    private val ctx: Context,
    private val textSizeSp: Float = 14f,
    rowHeightDp: Int = 40,
    private val padHDp: Int = 8
) {
    private val dm = ctx.resources.displayMetrics
    private fun dp(v: Int) = (v * (if (dm.density > 0f) dm.density else 1f)).roundToInt()
    private val rowH = dp(rowHeightDp)
    private val padH = dp(padHDp)
    private val minEmptyColPx = dp(40)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = textSizeSp * (if (dm.scaledDensity > 0) dm.scaledDensity else 1f)
    }

    fun renderInto(container: FixedHeaderTableLayout, sections: List<UiTableSection>) {
        require(sections.isNotEmpty()) { "sections must not be empty" }
        container.removeAllViews()

        // 1) Compute total column count across all sections
        val allHeaders = sections.flatMap { it.table.header }
        val allRows    = sections.flatMap { it.table.rows }
        val totalCols  = maxOf(
            allHeaders.maxOfOrNull { it.size } ?: 0,
            allRows.maxOfOrNull    { it.size } ?: 0
        ).coerceAtLeast(1)

        fun padRow(row: List<String>) =
            if (row.size >= totalCols) row else row + List(totalCols - row.size) { "" }

        val padded = sections.map { sec ->
            val h = sec.table.header.map(::padRow)
            val r = sec.table.rows.map(::padRow)
            UiTableSection(
                sec.title,
                UiTable(
                    h,
                    sec.table.spans,
                    r,
                    sec.table.editors,
                    sec.table.qStartCol,
                    sec.table.itemsEditable,
                    sec.table.colWidths
                )
            )
        }

        val colWidthsPx = computeColumnWidthsPx(padded, totalCols)

        // 2) Subtables
        val main  = FixedHeaderSubTableLayout(ctx)
        val colHd = FixedHeaderSubTableLayout(ctx)
        val rowHd = FixedHeaderSubTableLayout(ctx)
        val corner = FixedHeaderSubTableLayout(ctx)

        // Header/Corner opaque backgrounds (recommended)
        colHd.setBackgroundResource(R.drawable.bg_header_cell_black)
        rowHd.setBackgroundResource(R.drawable.bg_header_cell_black)
        corner.setBackgroundResource(R.drawable.bg_header_cell_black)

        // 3) Column header and corner from the first section
        val first = padded.first().table
        for (hr in first.header) {
            val tr = FixedHeaderTableRow(ctx)
            for (c in 1 until totalCols) tr.addView(makeHeaderCell(hr[c], colWidthsPx[c]))
            colHd.addView(tr)

            val cr = FixedHeaderTableRow(ctx)
            cr.addView(makeCornerCell(hr[0], colWidthsPx[0]))
            corner.addView(cr)
        }

        // 4) Row header + main body for all sections stacked
        padded.forEachIndexed { idx, sec ->
            // optional section title row
            sec.title?.let { title ->
                // Row header title (single cell)
                rowHd.addView(FixedHeaderTableRow(ctx).apply {
                    addView(makeSectionHeaderCell(title, colWidthsPx[0]))
                })
                // Main title row (spans visually across columns by filling cells)
                main.addView(FixedHeaderTableRow(ctx).apply {
                    addView(makeSectionHeaderBodyCell(title, colWidthsPx.getOrElse(1) { minEmptyColPx })) // first data column (c=1)
                    for (c in 2 until totalCols) addView(makeSectionHeaderBodyCell("", colWidthsPx[c]))
                })
            }

            // Data rows
            for (r in sec.table.rows) {
                rowHd.addView(FixedHeaderTableRow(ctx).apply {
                    addView(makeRowHeaderCell(r[0], colWidthsPx[0]))
                })
                main.addView(FixedHeaderTableRow(ctx).apply {
                    for (c in 1 until totalCols) addView(makeBodyCell(r[c], colWidthsPx[c]))
                })
            }

            // tiny gap between sections (skip after last)
            if (idx < padded.lastIndex) {
                rowHd.addView(FixedHeaderTableRow(ctx).apply { addView(makeGapCell(colWidthsPx[0])) })
                main.addView(FixedHeaderTableRow(ctx).apply {
                    for (c in 1 until totalCols) addView(makeGapCell(colWidthsPx[c]))
                })
            }
        }

        // 5) Add in required order
        container.addViews(main, colHd, rowHd, corner)
    }

    // ---- cell factories ----
    private fun makeHeaderCell(t: String, w: Int) = baseCell(t, w).apply {
        setBackgroundResource(R.drawable.bg_header_cell_black); gravity = Gravity.CENTER_VERTICAL
    }
    private fun makeCornerCell(t: String, w: Int) = baseCell(t, w).apply {
        setBackgroundResource(R.drawable.bg_header_cell_black); gravity = Gravity.CENTER_VERTICAL
    }
    private fun makeRowHeaderCell(t: String, w: Int) = baseCell(t, w).apply {
        setBackgroundResource(R.drawable.bg_header_cell_black); gravity = Gravity.CENTER_VERTICAL
    }
    private fun makeBodyCell(t: String, w: Int) = baseCell(t, w).apply {
        setBackgroundResource(R.drawable.bg_cell_black)
    }
    private fun makeSectionHeaderCell(t: String, w: Int) = baseCell(t, w).apply {
        setBackgroundResource(R.drawable.bg_header_cell_black); gravity = Gravity.CENTER_VERTICAL
    }
    private fun makeSectionHeaderBodyCell(t: String, w: Int) = baseCell(t, w).apply {
        setBackgroundResource(R.drawable.bg_header_cell_black)
    }
    private fun makeGapCell(w: Int) = baseCell("", w).apply {
        setBackgroundResource(android.R.color.transparent)
        minHeight = dp(8)
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

    private fun computeColumnWidthsPx(sections: List<UiTableSection>, totalCols: Int): List<Int> =
        List(totalCols) { col ->
            val hintChars = sections.firstNotNullOfOrNull { it.table.colWidths?.getOrNull(col) }
            hintChars?.let { measureWidthFromChars(it) } ?: measureColumnPx(sections, col)
        }

    private fun measureWidthFromChars(chars: Int): Int {
        if (chars <= 0) return minEmptyColPx
        val sample = "0".repeat(chars.coerceAtLeast(1))
        val width = paint.measureText(sample)
        return (width + padH * 2).roundToInt()
    }

    private fun measureColumnPx(sections: List<UiTableSection>, col: Int): Int {
        var maxPx = 0f
        sections.forEach { sec ->
            sec.table.header.forEach { row ->
                maxPx = max(maxPx, paint.measureText(row.getOrNull(col).orEmpty()))
            }
            sec.table.rows.forEach { row ->
                maxPx = max(maxPx, paint.measureText(row.getOrNull(col).orEmpty()))
            }
        }
        val withPad = if (maxPx > 0f) maxPx + padH * 2 else minEmptyColPx.toFloat()
        return withPad.roundToInt()
    }
}
