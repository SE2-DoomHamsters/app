package com.doomhamsters.cards


/** Lists the supported activatable card command identifiers. */
enum class CardCommandId {
    POWER_NAP,
    TWO_HAMSTERS,
    QUICK_PEEK,
    FOUR_HAMSTERS,
    SIGN_OF_FATE,
    HYPER_MODE,
    BEG_FOR_SNACKS,
    SNIFF_AHEAD,
    HAMSTER_TRIO,
    CAGE_SWAP,
    TUNNEL_CHAOS,
    STEAL_CARD,
    SQUICK;


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
