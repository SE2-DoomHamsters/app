# Adding Card Effects

This is the guide for adding a new card with its own tooltip, name, and effect.

## Where Things Live

### 1. Card type

File:

- `app/src/main/java/com/doomhamsters/model/Card.kt`

What to do:

- add a new `CardType`
- add any backend string aliases in `CardType.fromWire()`

Example:

```kotlin
enum class CardType {
    Doom,
    SnackStash,
    PowerNap,
    QuickPeek,
    StealCard,
    Normal
}
```

## 2. One file per card

Folder:

- `app/src/main/java/com/doomhamsters/cards/definitions/`

Each card gets its own file.

That file should contain:

- the card type
- the display name
- the tooltip text
- the command metadata
- the command implementation if the card is playable

Examples already in the project:

- `PowerNapCardDefinition.kt`
- `QuickPeekCardDefinition.kt`
- `DoomCardDefinition.kt`
- `SnackStashCardDefinition.kt`

## 3. Register the card

File:

- `app/src/main/java/com/doomhamsters/cards/CardRegistry.kt`

Add your new definition object to the `definitions` list.

Example:

```kotlin
val definitions: List<CardDefinition> = listOf(
    DoomCardDefinition,
    SnackStashCardDefinition,
    PowerNapCardDefinition,
    QuickPeekCardDefinition,
    StealCardDefinition,
    NormalCardDefinition
)
```

## 4. If the card is playable, give it a command

The command lives in the same file as the card definition.

Shared command types live here:

- `app/src/main/java/com/doomhamsters/logic/cardcommands/CardCommand.kt`

What the command does:

- receives the game state, player, and card
- applies the effect
- returns:
  - public message
  - optional private message
  - optional revealed card
  - whether the card ends the turn

## Example: playable card

```kotlin
object PowerNapCardDefinition : CardDefinition {
    override val type = CardType.PowerNap
    override val displayName = "Power Nap"
    override val description = "Skip your draw phase and end your turn."
    override val command = CardCommandDefinition(
        id = CardCommandId.POWER_NAP,
        endsTurn = true,
        executor = PowerNapCommand
    )

    private object PowerNapCommand : CardCommand {
        override val id = CardCommandId.POWER_NAP

        override fun execute(context: CardCommandContext): CardCommandOutcome {
            context.engine.discardFromHand(context.player, context.card)
            return CardCommandOutcome(
                publicMessage = "${context.player.name} activated Power Nap and skipped drawing.",
                endsTurn = true
            )
        }
    }
}
```

## Example: non-playable card

```kotlin
object SnackStashCardDefinition : CardDefinition {
    override val type = CardType.SnackStash
    override val displayName = "Snack Stash"
    override val description = "Automatically prevents one Doom. Keep it safe!"
    override val command = null
}
```

## What the UI uses automatically

The UI already reads card data from the registry.

That means once a card is registered, the app automatically gets:

- the card name on the face
- the tooltip title
- the tooltip description
- the `Activate card` button if `command != null`

Relevant files:

- `app/src/main/java/com/doomhamsters/cards/CardRegistry.kt`
- `app/src/main/java/com/doomhamsters/ui/gameboard/FannedHand.kt`
- `app/src/main/java/com/doomhamsters/ui/gameboard/CardFaces.kt`

## Multiplayer flow for playable cards

When a player presses `Activate card`, the client sends one generic action:

Destination:

```text
/app/game/{gameId}/card/activate
```

Payload:

```json
{
  "playerId": "player-1",
  "cardId": "qp-1",
  "cardType": "QuickPeek",
  "commandId": "QUICK_PEEK",
  "parameters": {}
}
```

The backend should:

1. validate the move
2. remove the card from the player hand
3. apply the effect
4. publish the updated game state
5. publish any public/private effect result messages

## Public event format

Use this when all players should know a card was played.

```json
{
  "type": "CARD_COMMAND_PLAYED",
  "playerId": "player-1",
  "playerName": "Alice",
  "commandId": "POWER_NAP",
  "card": {
    "id": "pn-1",
    "type": "PowerNap",
    "name": "Power Nap",
    "effectId": "POWER_NAP"
  },
  "message": "Alice activated Power Nap and skipped drawing."
}
```

## Private result format

Use this when only one player should see the result.

```json
{
  "type": "CARD_COMMAND_RESULT",
  "playerId": "player-1",
  "commandId": "QUICK_PEEK",
  "card": {
    "id": "qp-1",
    "type": "QuickPeek",
    "name": "Quick Peek",
    "effectId": "QUICK_PEEK"
  },
  "revealedCard": {
    "id": "doom-1",
    "type": "Doom",
    "name": "Doom"
  },
  "message": "Top card: Doom."
}
```

## If your card needs extra UI

Most cards do not need special UI work.

You only need extra frontend work if the effect needs:

- choosing another player
- choosing a card from a player
- choosing a deck position
- a private modal/result screen

The place to wire that is usually:

- `app/src/main/java/com/doomhamsters/viewmodel/GameBoardViewModel.kt`

## Checklist 

1. Add the new `CardType` in `Card.kt`
2. Create a new file in `cards/definitions/`
3. Put name, description, and command setup in that file
4. If playable, implement the command in that same file
5. Register the definition in `CardRegistry.kt`
6. If needed, add private/public event handling in `GameBoardViewModel.kt`
7. Add or update tests

## copy

Start from one of these:

- `PowerNapCardDefinition.kt` for a simple turn-ending effect
- `QuickPeekCardDefinition.kt` for a private information effect
