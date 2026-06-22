package com.doomhamsters.mascot.presentation

/**
 * Registry of mascot animation states and the GIF that backs each one.
 *
 * Adding a reaction is local to the mascot feature:
 *   1. Drop `<name>.gif` into `app/src/main/assets/mascot/`.
 *   2. Add an entry here.
 *   3. Add a branch in [MascotPresentation.resolve].
 */
enum class MascotAnimation(
    /** File name inside `assets/mascot/`. */
    val asset: String,
    /**
     * If non-null, this is a transient one-shot reaction: it plays for this many
     * ms and then control reverts to the resting animation. If null, it is a
     * persistent state that stays until the game state changes.
     */
    val holdMs: Long? = null,
    /**
     * When true, swapping IN to this animation plays a squash-and-stretch: the
     * mascot squashes down, the sprite is swapped at the bottom of the squash to
     * hide the seam, then it stretches back out. Set per animation.
     */
    val squashOnSwap: Boolean = false,
    /** When true, swapping OUT of this animation also plays the squash-and-stretch. */
    val squashOnExit: Boolean = false,
) {
    /** Resting state shown when it is not the local player's turn. */
    IDLE("idle.gif"),

    /** Resting state shown while it is the local player's turn. */
    YOUR_TURN("your_turn.gif"),

    /** Reaction shown while a Doom card is pending for the local player. */
    DOOM("doom.gif", squashOnSwap = true),

    /** Transient reaction played whenever the local player activates any card. */
    CARD_PLAYED("card_played.gif", holdMs = 1500, squashOnSwap = true, squashOnExit = true),

    /** State shown while the connection is not [com.doomhamsters.viewmodel.ConnectionStatus.Connected]. */
    DISCONNECTED("disconnected.gif", squashOnSwap = true),

    /** Transition played going IDLE -> YOUR_TURN. */
    TURNING("turning.gif", holdMs = 550),

    /** Reverse transition played going YOUR_TURN -> IDLE. */
    TURNING_BACK("turning_back.gif", holdMs = 550);

    /** Full Coil model URI for this animation's GIF under `assets/mascot/`. */
    val assetUri: String get() = "file:///android_asset/mascot/$asset"
}
