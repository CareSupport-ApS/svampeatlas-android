package com.noque.svampeatlas.models


import com.noque.svampeatlas.extensions.AppLanguage
import com.noque.svampeatlas.extensions.appLanguage
import com.noque.svampeatlas.extensions.capitalized
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class SubstrateGroup(val dkName: String,
                          val enName: String,
                          val czName: String? = null,
                          val substrates: MutableList<Substrate>) {

    val id: Int get() {
        when (dkName) {
            "jord" -> return 0
            "ved" -> return 1
            "plantemateriale" -> return 2
            "mosser" -> return 3
            "dyr" -> return 4
            "svampe og svampedyr" -> return 5
            "sten" -> return 6
            else -> return 100
        }
    }

    val localizedName: String get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> dkName.capitalized()
            AppLanguage.English -> enName.capitalized()
            AppLanguage.Czech -> czName?.capitalized() ?: enName.capitalized()
        }
    }

    fun appendSubstrate(substrate: Substrate) {
        substrates.add(substrate)
    }

    companion object {
        fun createFromSubstrates(substrates: List<Substrate>): List<SubstrateGroup> {
            val substrateGroups = mutableListOf<SubstrateGroup>()

            substrates.forEach { substrate ->
                if (substrate.hide) return@forEach
                val substrateGroup =
                    substrateGroups.firstOrNull { it.dkName == substrate.groupDkName }

                if (substrateGroup != null) {
                    substrateGroup.appendSubstrate(substrate)
                } else {
                    substrateGroups.add(
                        SubstrateGroup(
                            substrate.groupDkName,
                            substrate.groupEnName,
                            substrate.czName,
                            mutableListOf(substrate)
                        )
                    )
                }
            }
            return substrateGroups.sortedBy { it.id }
        }
    }
}