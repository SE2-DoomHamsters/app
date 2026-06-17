package com.doomhamsters.cards


/** Lists the supported activatable card command identifiers. */
enum class CardCommandId {
    POWER_NAP,
    SIGN_OF_FATE,
    HYPER_MODE,
    BEG_FOR_SNACKS,
    QUICK_PEEK,
    SNIFF_AHEAD,
    STEAL_CARD;




    companion object {
        /** Maps backend wire values to a known card command identifier. */
        fun fromWire(value: String?): CardCommandId? {
            if (value.isNullOrBlank()) return null
            val normalized = value
                .trim()
                .replace("-", "_")
                .replace(" ", "_")
                .uppercase()
            return entries.firstOrNull { commandId ->
                commandId.name == normalized ||
                    commandId.name.replace("_", "") == normalized.replace("_", "")
            }
        }
    }
}
