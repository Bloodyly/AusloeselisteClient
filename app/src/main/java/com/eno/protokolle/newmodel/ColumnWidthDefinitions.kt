package com.eno.protokolle.newmodel

/**
 * Feste Spaltenbreiten pro Protokolltyp für Melder- und Hardwaretabellen.
 * Werte sind Zeichenanzahlen; alle nicht aufgeführten Spalten erhalten den Default-Wert des jeweiligen Typs.
 */
object ColumnWidthDefinitions {

    private val melder = mapOf(
        "BMA" to TableWidthDefinition(
            defaultChars = 2,
            overrides = mapOf(0 to 4, 1 to 2, 2 to 6)
        ),
        "EMA" to TableWidthDefinition(
            defaultChars = 3,
            overrides = mapOf(0 to 5, 1 to 3, 2 to 6)
        ),
        "ELA" to TableWidthDefinition(
            defaultChars = 3,
            overrides = mapOf(0 to 5, 1 to 3, 2 to 6)
        ),
        "RWA" to TableWidthDefinition(
            defaultChars = 3,
            overrides = mapOf(0 to 5, 1 to 3, 2 to 6)
        ),
        "LR" to TableWidthDefinition(
            defaultChars = 3,
            overrides = mapOf(0 to 5, 1 to 3, 2 to 6)
        )
    )

    private val hardware = mapOf(
        "BMA" to TableWidthDefinition(
            defaultChars = 3,
            overrides = mapOf(0 to 6, 1 to 4)
        ),
        "EMA" to TableWidthDefinition(
            defaultChars = 3,
            overrides = mapOf(0 to 6, 1 to 4)
        ),
        "ELA" to TableWidthDefinition(
            defaultChars = 3,
            overrides = mapOf(0 to 6, 1 to 4)
        ),
        "RWA" to TableWidthDefinition(
            defaultChars = 3,
            overrides = mapOf(0 to 6, 1 to 4)
        ),
        "LR" to TableWidthDefinition(
            defaultChars = 3,
            overrides = mapOf(0 to 6, 1 to 4)
        )
    )

    fun melderWidths(pType: String, colCount: Int): List<Int>? =
        melder[pType.uppercase()]?.toWidthList(colCount)

    fun hardwareWidths(pType: String, colCount: Int): List<Int>? =
        hardware[pType.uppercase()]?.toWidthList(colCount)

    data class TableWidthDefinition(
        val defaultChars: Int,
        val overrides: Map<Int, Int> = emptyMap()
    )
}

private fun ColumnWidthDefinitions.TableWidthDefinition.toWidthList(colCount: Int): List<Int> =
    List(colCount) { idx -> overrides[idx] ?: defaultChars }
