package com.grasell

val emptyTeam = Team(null, 0, 0, 0)

fun solveDraftsack(players: List<Player>, budget: Int, slots: List<Slot>, numPlayersCallback: (Int) -> Unit = {}, memoizedSizeCallback: (Int) -> Unit = {}): Team? {
    val slotDefs = slots.toTypedArray()
    val culledPlayerList = cullPlayers(players, slots)
        .sortedBy { it.score.toDouble() / it.cost } // ascending so best value at end (processed first)
        .toTypedArray()

    if (culledPlayerList.isEmpty()) return emptyTeam

    val maxScorePerCost = culledPlayerList.map { it.score.toDouble() / it.cost }.max()!!
    val initialSizes = IntArray(slotDefs.size) { slotDefs[it].size }
    val memoization = HashMap<Long, Team?>()
    val bestScore = intArrayOf(0) // shared mutable best score for pruning

    return solveRecursive(
        culledPlayerList, slotDefs, initialSizes, budget,
        culledPlayerList.size - 1, memoization, bestScore, maxScorePerCost,
        numPlayersCallback, memoizedSizeCallback
    )
}

private fun encodeMemoKey(slotSizes: IntArray, playerIndex: Int, budget: Int): Long {
    // Layout (64-bit Long):
    //   bits [0..19]  = budget (supports up to 1,048,575)
    //   bits [20..31] = playerIndex (supports up to 4095)
    //   bits [32..63] = slot sizes, 4 bits each (supports up to 8 slots)
    var key = (budget.toLong() and 0xFFFFF) or ((playerIndex.toLong() and 0xFFF) shl 20)
    var shift = 32
    for (size in slotSizes) {
        key = key or ((size.toLong() and 0xF) shl shift)
        shift += 4
    }
    return key
}

private fun solveRecursive(
    players: Array<Player>,
    slotDefs: Array<Slot>,
    slotSizes: IntArray,
    budget: Int,
    playerIndex: Int,
    memoization: HashMap<Long, Team?>,
    bestScore: IntArray,
    maxScorePerCost: Double,
    numPlayersCallback: (Int) -> Unit,
    memoizedSizeCallback: (Int) -> Unit
): Team? {
    if (budget < 0) return null
    if (playerIndex < 0) return emptyTeam
    numPlayersCallback(playerIndex + 1)

    // Upper-bound pruning: can we possibly beat the best score found so far?
    val upperBound = (budget * maxScorePerCost).toInt()
    if (upperBound <= 0 && bestScore[0] > 0) return emptyTeam

    val key = encodeMemoKey(slotSizes, playerIndex, budget)

    memoization[key]?.let { return it }
    if (key in memoization) return null // cached null result

    val currentPlayer = players[playerIndex]
    val nextIndex = playerIndex - 1

    // Option 1: Skip this player
    val teamFromSkipping = solveRecursive(
        players, slotDefs, slotSizes, budget, nextIndex,
        memoization, bestScore, maxScorePerCost,
        numPlayersCallback, memoizedSizeCallback
    )

    // Option 2: Try slotting this player into each compatible slot
    var teamFromSlotting: Team? = null
    val budgetAfterPlayer = budget - currentPlayer.cost
    for (i in slotDefs.indices) {
        if (slotSizes[i] > 0 && slotDefs[i].fitsPlayer(currentPlayer)) {
            val newSizes = slotSizes.copyOf()
            newSizes[i]--
            val potentialTeam = solveRecursive(
                players, slotDefs, newSizes, budgetAfterPlayer, nextIndex,
                memoization, bestScore, maxScorePerCost,
                numPlayersCallback, memoizedSizeCallback
            )
            if (potentialTeam != null) {
                val withPlayer = potentialTeam.withPlayer(currentPlayer)
                if (teamFromSlotting == null || withPlayer.score > teamFromSlotting!!.score) {
                    teamFromSlotting = withPlayer
                }
            }
        }
    }

    var result = getBest(teamFromSkipping, teamFromSlotting)
    if (result == null) result = emptyTeam

    // Update best score for pruning
    if (result.score > bestScore[0]) {
        bestScore[0] = result.score
    }

    memoizedSizeCallback(memoization.size)
    memoization[key] = result

    return result
}

private fun getBest(t1: Team?, t2: Team?): Team? {
    if (t1 == null) return t2
    if (t2 == null) return t1
    return if (t1.score > t2.score) t1 else t2
}

fun cullPlayers(players: List<Player>, slots: List<Slot>): List<Player> {
    val maxPossiblePerPosition = slots.asSequence()
        .flatMap { slot ->
            slot.positionsAllowed.asSequence()
                .map { position -> Pair(position, slot.size) }
        }
        .groupingBy { it.first }
        .fold(0) { accum, element -> accum + element.second }

    return players.asSequence()
        .filter { it.score > 0 }
        .groupBy { Pair(it.position, it.cost) }
        .asSequence()
        .filter { (key, _) -> maxPossiblePerPosition.containsKey(key.first) }
        .flatMap { (key, playerGroup) ->
            playerGroup
                .sortedByDescending { it.score }
                .take(maxPossiblePerPosition[key.first]!!)
                .asSequence()
        }
        .toList()
}

// Linked-list node for O(1) team construction
class PlayerNode(val player: Player, val next: PlayerNode?)

data class Team(val head: PlayerNode?, val size: Int, val score: Int, val cost: Int) {
    fun withPlayer(player: Player) = Team(
        head = PlayerNode(player, head),
        size = size + 1,
        score = score + player.score,
        cost = cost + player.cost
    )

    val players: List<Player>
        get() {
            val result = mutableListOf<Player>()
            var node = head
            while (node != null) {
                result.add(node.player)
                node = node.next
            }
            return result
        }
}

data class Slot(val positionsAllowed: Set<String>, val size: Int) {
    fun fitsPlayer(player: Player) = positionsAllowed.contains(player.position)
}

data class Player(val name: String, val score: Int, val cost: Int, val position: String)
