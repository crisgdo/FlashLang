package com.cristiano.flashlang.data.model

import androidx.room.Embedded

data class DeckListItem(
    @Embedded val deck: Deck,
    val cardCount: Int,
    val pendingCount: Int,
)
