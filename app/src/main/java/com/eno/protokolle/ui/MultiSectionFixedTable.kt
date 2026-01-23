/**
 * Renderer, der mehrere UiTable-Sektionen in einem gemeinsamen FixedHeaderTableLayout stapelt.
 */
package com.eno.protokolle.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.eno.protokolle.R
import com.eno.protokolle.newmodel.UiTable
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderSubTableLayout
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableLayout
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableRow
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
    private val padHDp: Int = 8,
    private val quarterProvider: () -> String = { "" }
) {
    private val dm = ctx.resources.displayMetrics
    private fun dp(v: Int) = (v * (if (dm.density > 0f) dm.density else 1f)).roundToInt()
    private val rowH = dp(rowHeightDp)
    private val padH = dp(padHDp)
    private val minEmptyColPx = dp(40)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = textSizeSp * (if (dm.scaledDensity > 0) dm.scaledDensity else 1f)
    }
    private val placeholderColor = ContextCompat.getColor(ctx, android.R.color.darker_gray)
    private val textColor = ContextCompat.getColor(ctx, android.R.color.black)

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
        fun padTypeRow(row: List<String?>) =
            if (row.size >= totalCols) row else row + List(totalCols - row.size) { null }

        val padded = sections.map { sec ->
            val h = sec.table.header.map(::padRow)
            val r = sec.table.rows.map(::padRow)
            UiTableSection(
                sec.title,
                UiTable(
                    h,
                    sec.table.spans,
                    r,
                    sec.table.cellTypes?.map(::padTypeRow),
                    sec.table.editors,
                    sec.table.qStartCol,
                    sec.table.itemsEditable,
                    sec.table.colWidths
                )
            )
        }

        // Column widths computed once (char-hints supported via UiTable.colWidths)
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
                // Main title row (fill cells)
                main.addView(FixedHeaderTableRow(ctx).apply {
                    addView(makeSectionHeaderBodyCell(title, colWidthsPx.getOrElse(1) { minEmptyColPx }))
                    for (c in 2 until totalCols) addView(makeSectionHeaderBodyCell("", colWidthsPx[c]))
                })
            }

            val types = sec.table.cellTypes
            sec.table.rows.forEachIndexed { rIdx, r ->
                val typeRow = types?.getOrNull(rIdx)
                // Row header
                rowHd.addView(FixedHeaderTableRow(ctx).apply {
                    addView(makeHeaderCell(r[0], colWidthsPx[0]))
                })
                // Body row
                main.addView(FixedHeaderTableRow(ctx).apply {
                    for (c in 1 until totalCols) {
                        val type = typeRow?.getOrNull(c)
                        addView(makeBodyCell(r[c], type, colWidthsPx[c]))
                    }
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

        // 5) Add in required order: (main, colHd, rowHd, corner)
// Add in required order: (main, colHd, rowHd, corner)
        container.addViews(main, colHd, rowHd, corner)

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
            val rows = sec.table.header + sec.table.rows
            rows.forEach { row ->
                val text = row.getOrNull(col).orEmpty()
                if (text.isNotEmpty()) {
                    maxPx = maxOf(maxPx, paint.measureText(text))
                }
            }
        }
        val w = (maxPx + padH * 2).roundToInt()
        return if (w <= 0) minEmptyColPx else w
    }

    private fun baseCell(widthPx: Int): TextView =
        TextView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(widthPx, rowH)
            gravity = Gravity.CENTER
            setPadding(padH, 0, padH, 0)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            setSingleLine()
            ellipsize = TextUtils.TruncateAt.END
        }

    private fun makeHeaderCell(text: String, widthPx: Int): TextView =
        baseCell(widthPx).apply {
            this.text = text
            setTextColor(textColor)
            setBackgroundResource(R.drawable.bg_header_cell_black)
        }

    private fun makeCornerCell(text: String, widthPx: Int): TextView =
        baseCell(widthPx).apply {
            this.text = text
            setTextColor(textColor)
            setBackgroundResource(R.drawable.bg_header_cell_black)
        }

    private fun makeBodyCell(text: String, type: String?, widthPx: Int): FrameLayout {
        val cell = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(widthPx, rowH)
        }
        val valueText = baseCell(widthPx).apply {
            setTextColor(textColor)
            setSingleLine()
            ellipsize = TextUtils.TruncateAt.END
            setPadding(padH, 0, padH, 0)
            gravity = Gravity.CENTER
        }
        val hintText = baseCell(widthPx).apply {
            setTextColor(placeholderColor)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp * 0.75f)
            setSingleLine()
            ellipsize = TextUtils.TruncateAt.END
            setPadding(padH, 0, padH, 0)
            gravity = Gravity.CENTER
        }

        val hasType = !type.isNullOrBlank()
        val hasValue = text.isNotBlank()

        when {
            hasValue -> {
                valueText.text = text
                hintText.visibility = TextView.GONE
                cell.setBackgroundResource(R.drawable.table_cell_background)
            }
            hasType -> {
                valueText.text = ""
                hintText.text = type
                hintText.visibility = TextView.VISIBLE
                cell.setBackgroundResource(R.drawable.table_cell_background)
            }
            else -> {
                valueText.text = "-"
                hintText.visibility = TextView.GONE
                cell.setBackgroundColor(Color.WHITE)
            }
        }

        if (hasType) {
            cell.isClickable = true
            cell.isFocusable = true
            cell.setOnClickListener {
                val currentValue = valueText.text?.toString().orEmpty()
                if (currentValue.isNotBlank()) {
                    valueText.text = ""
                    hintText.text = type
                    hintText.visibility = TextView.VISIBLE
                } else {
                    valueText.text = quarterProvider()
                    hintText.visibility = TextView.GONE
                }
            }
        }

        cell.addView(valueText)
        cell.addView(hintText)
        return cell
    }

    private fun makeGapCell(widthPx: Int): TextView =
        baseCell(widthPx).apply {
            text = ""
            setBackgroundResource(R.drawable.bg_header_cell_black)
        }

    private fun makeSectionHeaderCell(text: String, widthPx: Int): TextView =
        baseCell(widthPx).apply {
            this.text = text
            setTextColor(textColor)
            setBackgroundResource(R.drawable.bg_header_cell_black)
        }

    private fun makeSectionHeaderBodyCell(text: String, widthPx: Int): TextView =
        baseCell(widthPx).apply {
            this.text = text
            setTextColor(textColor)
            setBackgroundResource(R.drawable.bg_header_cell_black)
        }
}
