package com.doomhamsters.cards


enum class CardCommandId {
    POWER_NAP,
    QUICK_PEEK;

    companion object {
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
