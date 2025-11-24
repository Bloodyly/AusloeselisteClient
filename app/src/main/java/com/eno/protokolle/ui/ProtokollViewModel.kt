// com/eno/protokolle/ui/ProtokollViewModel.kt
/**
 * ViewModel hält das aktuell geladene Protokoll und stellt Hilfsmethoden für das UI bereit.
 */
package com.eno.protokolle.ui

import androidx.lifecycle.ViewModel
import com.eno.protokolle.newmodel.ProtokollConstruct

class ProtokollViewModel : ViewModel() {
    var construct: ProtokollConstruct? = null
    var selectedQuarter: String = "Q1"
}
