/**
 * Fragment zur Anzeige einer Anlageseite mit Melder- und Hardware-Tabellen im FixedHeader-Layout.
 */
package com.eno.protokolle.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.eno.protokolle.R
import com.eno.protokolle.newmodel.ColumnWidthDefinitions
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableContainer
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableLayout
import com.eno.protokolle.newmodel.UiTable

class AnlagePageFragmentFixed : Fragment(R.layout.frag_anlage_page_fixed) {

    companion object {
        private const val KEY_INDEX = "anlage_index"
        fun new(index: Int) = AnlagePageFragmentFixed().apply {
            arguments = bundleOf(KEY_INDEX to index)
        }
    }

    private val vm: ProtokollViewModel by activityViewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val construct = vm.construct ?: return
        val index = requireArguments().getInt(KEY_INDEX)
        val anlage = construct.anlagen.getOrNull(index) ?: return
        val pType = construct.pType
        val isBma = pType.equals("BMA", ignoreCase = true)

        val container = view.findViewById<FixedHeaderTableContainer>(R.id.tableContainer)

        // Helper: Spaltenanzahl bestimmen
        fun colCountOf(t: UiTable): Int {
            val h = t.header.maxOfOrNull { it.size } ?: 0
            val r = t.rows.maxOfOrNull { it.size } ?: 0
            return maxOf(h, r).coerceAtLeast(1)
        }

        // Breiten-Regeln (in "Zeichen" = colWidths-Hints)
        fun resolveColWidths(table: UiTable, cols: Int, fallback: List<Int>?): List<Int>? {
            val base = table.colWidths
            if (base == null || base.isEmpty()) return fallback
            if (base.size >= cols) return base.take(cols)
            val pad = fallback?.drop(base.size)
                ?: List(cols - base.size) { base.lastOrNull() ?: 3 }
            return base + pad
        }

        fun melderColWidths(table: UiTable, cols: Int): List<Int>? {
            val fallback = ColumnWidthDefinitions.melderWidths(pType, cols)
            return if (isBma) fallback else resolveColWidths(table, cols, fallback)
        }

        fun hardwareColWidths(table: UiTable, cols: Int): List<Int>? {
            val fallback = ColumnWidthDefinitions.hardwareWidths(pType, cols)
            return if (isBma) fallback else resolveColWidths(table, cols, fallback)
        }

        val renderer = MultiSectionFixedTable(
            requireContext(),
            textSizeSp = 14f,
            rowHeightDp = 40,
            padHDp = 8,
            quarterProvider = { vm.selectedQuarter }
        )

        // 1) Melder/Auslöseliste als eigene Tabelle
        val melderTable = FixedHeaderTableLayout(requireContext()).apply {
            setMinScale(0.5f)
            setMaxScale(2.0f)
        }

        val melderCols = colCountOf(anlage.melder)
        val melderUi = anlage.melder.copy(colWidths = melderColWidths(anlage.melder, melderCols))

        container.addSubTable(melderTable)
        renderer.renderInto(melderTable, listOf(UiTableSection(null, melderUi)))

        // 2) Hardware als eigene Tabelle (unabhängige Breiten)
        anlage.hardware?.let { hw ->
            val hardwareTable = FixedHeaderTableLayout(requireContext()).apply {
                setMinScale(0.5f)
                setMaxScale(2.0f)
            }

            val hwCols = colCountOf(hw)
            val hwUi = hw.copy(colWidths = hardwareColWidths(hw, hwCols))

            container.addSubTable(hardwareTable)
            renderer.renderInto(hardwareTable, listOf(UiTableSection(null, hwUi)))
        }

        container.post {
            container.scaleToFitWidth()
        }
    }
}
