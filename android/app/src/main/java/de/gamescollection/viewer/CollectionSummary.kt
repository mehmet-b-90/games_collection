package de.gamescollection.viewer

/** Calculated once per collection on Default, not during composition. */
data class CollectionSummary(
    val count: Int = 0, val averageRating: Double? = null, val platformCount: Int = 0,
    val engines: List<String> = emptyList(), val ageRatings: List<String> = emptyList(),
    val distributions: List<GameDistribution> = emptyList()
) {
    companion object {
        fun from(games: List<Game>, checkpoint: () -> Unit = {}): CollectionSummary {
            val platforms = mutableMapOf<String, Int>()
            val statuses = mutableMapOf<String, Int>()
            val formats = mutableMapOf<String, Int>()
            val engineCounts = mutableMapOf<String, Int>()
            val engines = sortedSetOf<String>()
            val ages = sortedSetOf<String>()
            var ratingSum = 0L
            var ratingCount = 0
            fun MutableMap<String, Int>.count(value: String) { this[value] = getOrDefault(value, 0) + 1 }
            games.forEachIndexed { index, game ->
                if (index % 128 == 0) checkpoint()
                game.platforms.forEach { platforms.count(it) }
                statuses.count(game.status); formats.count(game.format)
                engineCounts.count(game.engine.ifEmpty { "Unbekannt" })
                game.engine.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { engines.add(it) }
                if (game.ageRating.isNotEmpty()) ages.add(game.ageRating)
                game.rating?.let { ratingSum += it; ratingCount++ }
            }
            val groups = listOf("Plattformen · Besitzzuordnungen" to platforms, "Status" to statuses,
                "Format" to formats, "Engine" to engineCounts).map { (title, counts) ->
                GameDistribution(title, counts.entries.sortedByDescending { it.value }.map { it.key to it.value }, counts.values.sum())
            }
            checkpoint()
            return CollectionSummary(games.size, if (ratingCount == 0) null else ratingSum.toDouble() / ratingCount,
                platforms.size, engines.toList(), ages.toList(), groups)
        }
    }
}

data class GameDistribution(val title: String, val rows: List<Pair<String, Int>>, val total: Int)
