import com.google.common.collect.Sets
import org.apache.commons.csv.CSVFormat
import kotlin.math.abs

fun main(args: Array<String>) {
    val CSVReader = CSVFormat.Builder.create(CSVFormat.DEFAULT).apply {
        setIgnoreSurroundingSpaces(true)
    }.build()

    val jsonResource = object {}.javaClass.getResource("PP_stars.csv")

    val planets: List<Planet> = jsonResource?.let { url ->
        CSVReader.parse(url.openStream().reader())
            .drop(1) // Dropping the header
            .asSequence()
            .map {
                Planet(
                    name = it[1],
                    player = it[2],
                    economy = it[3].toInt(),
                    industry = it[4].toInt(),
                    science = it[5].toInt(),
                    resources = it[6].toInt(),
                    naturalResources = it[7].toInt(),
                    ships = it[8].toInt(),
                    y = it[9].toDouble(),
                    x = it[10].toDouble(),
                )
            }
            .toList()
    } ?: emptyList()

    val top5 = getTopNSplits(planets, 5)

    println()
    top5.forEach { combo ->
        println()
        println(combo.first.toString())
        print("(${combo.second.first.sumOf { it.naturalResources }}, ")
        print("${combo.second.first.sumOf { it.industry }}, ")
        print("${combo.second.first.sumOf { it.science }}) ")
        println(combo.second.first.map { "${it.name}(${it.naturalResources}, ${it.industry}, ${it.science})" })

        print("(${combo.second.second.sumOf { it.naturalResources }}, ")
        print("${combo.second.second.sumOf { it.industry }}, ")
        print("${combo.second.second.sumOf { it.science }}) ")
        println(combo.second.second.map { "${it.name}(${it.naturalResources}, ${it.industry}, ${it.science})" })
    }
}

fun getTopNSplits(planets: List<Planet>, n: Int): Collection<Pair<Double, Pair<Set<Planet>, Set<Planet>>>> {
    val splits: Sequence<Pair<Set<Planet>, Set<Planet>>> = preScreenSplits(generateSplits(planets))

    val results: MutableList<Pair<Double, Pair<Set<Planet>, Set<Planet>>>> = mutableListOf()

    for (potential in splits) {
        results.addAndKeepTopN(potential, n)
    }

    return results
}

fun preScreenSplits(splits: Sequence<Pair<Set<Planet>, Set<Planet>>>): Sequence<Pair<Set<Planet>, Set<Planet>>> {
    return sequence {
        splits.forEach { split ->
            if (
                split.getDistance { it.naturalResources } < 10
                && (split.getDistance { it.industry } < 2 && split.getDistance { it.science } < 2)
            ) {
                yield(split)
            }
        }
    }
}

fun generateSplits(planets: List<Planet>): Sequence<Pair<Set<Planet>, Set<Planet>>> {
    val allPlanets: LinkedHashSet<Planet> = LinkedHashSet(planets)
    val combinations: Sequence<Pair<Set<Planet>, Set<Planet>>> = sequence {
        for (i in 2..planets.size) {
            for (combo in Sets.combinations(allPlanets, i)) {
                val remainder = allPlanets.subtract(combo)
                if (combo.size <= remainder.size) {
                    yield(Pair(combo, remainder))
                } else {
                    yield(Pair(remainder, combo))
                }
            }
        }
    }

    return combinations
}

fun Pair<Set<Planet>, Set<Planet>>.getDistance(): Double {
    return this.getDistance { it.naturalResources } / 10.0 +
            this.getDistance { it.industry } +
            this.getDistance { it.science }
}

fun Pair<Set<Planet>, Set<Planet>>.getDistance(sumField: (Planet) -> Int): Int {
    val sum1 = this.first.sumOf { sumField(it) }
    val sum2 = this.second.sumOf { sumField(it) }

    return abs(sum1 - sum2)
}

fun MutableList<Pair<Double, Pair<Set<Planet>, Set<Planet>>>>.addAndKeepTopN(
    element: Pair<Set<Planet>, Set<Planet>>,
    n: Int
) {
    this.add(Pair(element.getDistance(), element))
    this.sortBy { it.first }
    while (this.size > n) {
        this.removeLast()
    }
}

data class Planet(
    val name: String,
    val player: String,
    val economy: Int,
    val industry: Int,
    val science: Int,
    val resources: Int,
    val naturalResources: Int,
    val ships: Int,
    val y: Double,
    val x: Double
)