/**
 * Globally accessible in-memory repository for the currently geladene ProtokollConstruct Instanz.
 */
package com.eno.protokolle.data

import com.eno.protokolle.newmodel.ProtokollConstruct

object ProtokollRepo {
    @Volatile var construct: ProtokollConstruct? = null
}
