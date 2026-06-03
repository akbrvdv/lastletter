package com.kelompok6.lastletter.data.model

data class GameRoom(
    var roomId: String = "",
    var status: String = "WAITING", // Status bisa berupa: WAITING, PLAYING, FINISHED
    var player1Id: String = "",
    var player2Id: String = "",
    var currentTurnId: String = "",
    var lastWord: String? = null,
    var usedWords: Map<String, Boolean> = emptyMap(),
    var winnerId: String? = null,
    var players: Map<String, Long> = emptyMap()
)
