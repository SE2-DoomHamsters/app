package com.doomhamsters.cards
import com.doomhamsters.cards.definitions.CageSwapCardDefinition
import com.doomhamsters.model.CardType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
class CageSwapCardDefinitionTest {
    @Test
    fun `verify CageSwap card properties are mapped correctly`() {
        assertEquals(CardType.CageSwap, CageSwapCardDefinition.type)
        assertEquals("Cage Swap", CageSwapCardDefinition.displayName)
        assertEquals("Swap your entire hand with the next player in turn order.", CageSwapCardDefinition.description)
    }

    @Test
    fun `verify CageSwap command definition sends correct targetless command`() {
        val command = CageSwapCardDefinition.command
        assertEquals(CardCommandId.CAGE_SWAP, command.id)
        assertFalse(command.privateResult)
        assertFalse(command.endsTurn)
    }
}