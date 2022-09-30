package dev.johnoreilly.confetti.backend.import

import dev.johnoreilly.confetti.backend.datastore.*
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import net.mbonnin.bare.graphql.asList
import net.mbonnin.bare.graphql.asMap
import net.mbonnin.bare.graphql.asString
import net.mbonnin.bare.graphql.toAny
import okhttp3.OkHttpClient
import okhttp3.Request

object FrenchKit {
    val okHttpClient = OkHttpClient()

    private fun getUrl(url: String): String {
        return Request.Builder()
            .url(url)
            .build()
            .let {
                okHttpClient.newCall(it).execute().also {
                    check(it.isSuccessful) {
                        "Cannot get $url: ${it.body?.string()}"
                    }
                }
            }.body!!.string()
    }

    private fun String.toRoom() : String{
        return if (this.isBlank()) {
            "all"
        } else {
            this
        }
    }
    private fun getJsonUrl(url: String) = Json.parseToJsonElement(getUrl(url)).toAny()

    fun import() {
        val schedule = getJsonUrl("https://frenchkit.fr/schedule/schedule-14.json")
        val speakersJson = getJsonUrl("https://frenchkit.fr/speakers/speakers-8.json")

        val sessions = schedule.asList.map {
            it.asMap
        }.map {
            DSession(
                id = it.get("id").asString,
                type = it.get("type").asString,
                title = it.get("title").asString,
                description = it.get("summary")?.asString,
                language = "en-US",
                start = it.get("fromTime").asString.replace(" ", "T").let { LocalDateTime.parse(it) },
                end = it.get("toTime").asString.replace(" ", "T").let { LocalDateTime.parse(it) },
                complexity = null,
                feedbackId = null,
                tags = emptyList(),
                rooms = listOf(it.get("room").asString.toRoom()),
                speakers = it.get("speakers").asList.map { it.asMap.get("id").asString }
            )
        }

        val rooms = sessions.flatMap { it.rooms }.map { DRoom(it, it) }
        val speakers = speakersJson.asList.map { it.asMap }.map {
            DSpeaker(
                id = it.get("id").asString,
                name = it.get("firstName").asString,
                photoUrl = it.get("imageURL").asString,
                bio = null,
                city = null,
                company = null,
                companyLogoUrl = null,
                links = emptyList()
            )
        }
        DataStore().write(
            conf = "frenchkit2022",
            sessions = sessions.sortedBy { it.start },
            rooms = rooms,
            speakers = speakers,
            partnerGroups = emptyList(),
            config = DConfig(
                timeZone = "Europe/Paris"
            ),
            venues = listOf(
                DVenue(
                    id = "main",
                    name = "Cité des Congrès de Nantes",
                    address = "5 rue de Valmy, 44000 Nantes",
                    description = mapOf(
                        "en" to "Located in the center of Nantes, the event takes place in the \"Cité des Congrès\" with more than 3000m² of conference rooms, hand's on and networking space…",
                        "fr" to "Située en plein cœur de ville, La Cité des Congrès de Nantes propose pour le DevFest Nantes plus de 3000m² de salles de conférences, codelabs et lieu de rencontre…",
                    ),
                    latitude = 47.21308725112951,
                    longitude = -1.542622837466317,
                    imageUrl = "https://devfest.gdgnantes.com/static/6328df241501c6e31393e568e5c68d7e/efc43/amphi.webp"
                )
            )
        )
    }
}