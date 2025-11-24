/**
 * Datenmodell der heruntergeladenen Protokolle inklusive Anlagen, Meldern und Hardwaretabellen.
 */
package com.eno.protokolle.newmodel

data class UiTable(
    val header: List<List<String>>,
    val spans: List<Span>?,
    val rows: List<List<String>>,
    val editors: Map<Int, ProtokollCodec.EditKind>,
    val qStartCol: Int?,           // NEU: Grenze zwischen Descriptor und Quartal
    val itemsEditable: Boolean,    // NEU: aus Server-Meta
    val colWidths: List<Int>? = null // optionale Vorgaben für Spaltenbreiten (Zeichenanzahl)
)

data class UiAnlage(
    val name: String,
    val melder: UiTable,
    val hardware: UiTable? = null
)

data class ProtokollConstruct(
    val pType: String,               // meta.PType
    val wType: String,               // meta.WType
    val vN: String,                  // VN nummer
    val kdn: String,                  // Kundenname
    val melderTypes: List<String>,   // für Editor-Auswahl
    val anlagen: List<UiAnlage>
)
