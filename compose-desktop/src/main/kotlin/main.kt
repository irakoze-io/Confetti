import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import dev.johnoreilly.confetti.ConfettiRepository
import dev.johnoreilly.confetti.dev.johnoreilly.confetti.ui.SessionDetailViewSharedWrapper
import dev.johnoreilly.confetti.di.initKoin
import dev.johnoreilly.confetti.fragment.SessionDetails
import dev.johnoreilly.confetti.sessionSpeakerLocation
import dev.johnoreilly.confetti.utils.JvmDateService
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.datetime.TimeZone
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import org.koin.dsl.module

private fun mainModule() = module {
    factory {
        ApolloClient.Builder()
            .serverUrl("https://confetti-app.dev/graphql")
    }
}

private val koin = initKoin {
    modules(mainModule())
}.koin

private val dateService = JvmDateService()

fun main() = application {
    val windowState = rememberWindowState()

    LaunchedEffect(key1 = this) {
        Napier.base(DebugAntilog())
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Confetti"
    ) {
        val colorScheme = lightColorScheme(
            primary = Color(0xFF8C4190)
        )

        MaterialTheme(colorScheme = colorScheme) {
            MainLayout()
        }
    }
}

@Composable
fun MainLayout() {
    val repository = koin.get<ConfettiRepository>()
    val currentSession: MutableState<SessionDetails?> = remember { mutableStateOf(null) }

    val sessionList = remember { mutableStateOf<List<SessionDetails>?>(emptyList()) }
    val conference = "droidconberlin2023"

    LaunchedEffect(conference) {
        val conferenceData = repository.conferenceData(conference, FetchPolicy.CacheFirst)
        sessionList.value = conferenceData.data?.sessions?.nodes?.map { it.sessionDetails }
    }

    Row(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth(0.3f), contentAlignment = Alignment.Center) {
            sessionList.value?.let { sessionList ->
                SessionListView(sessionList) {
                    currentSession.value = it
                }
            }
        }
        Column(Modifier.padding(vertical = 16.dp)) {
            SessionDetailViewSharedWrapper(currentSession.value) {}
        }
    }
}


@Composable
fun SessionListView(
    sessionList: List<SessionDetails>,
    sessionSelected: (player: SessionDetails) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(3.dp)
            .background(color = Color.White)
            .clip(shape = RoundedCornerShape(3.dp))
    ) {
        LazyColumn {
            items(items = sessionList, itemContent = { session ->
                SessionView(session, sessionSelected)
            })
        }
    }
}


@Composable
fun SessionView(session: SessionDetails, sessionSelected: (session: SessionDetails) -> Unit) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = { sessionSelected(session) })
    ) {

        Column(modifier = Modifier.weight(1f)) {
            val sessionTime = getSessionTime(session, currentSystemDefault())
            Text(
                sessionTime,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = session.title, style = TextStyle(fontSize = 16.sp))
            }

            session.room?.let {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        session.sessionSpeakerLocation(),
                        style = TextStyle(fontSize = 14.sp), fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


private fun getSessionTime(session: SessionDetails, timeZone: TimeZone): String {
    return dateService.format(session.startsAt, timeZone, "MMM dd, HH:mm")
}
