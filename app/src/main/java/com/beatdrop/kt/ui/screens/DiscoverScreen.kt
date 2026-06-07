package com.beatdrop.kt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.beatdrop.kt.data.Album
import com.beatdrop.kt.data.SampleData
import com.beatdrop.kt.ui.components.*
import com.beatdrop.kt.ui.theme.*

@Composable
fun DiscoverScreen(
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onTrackClick: () -> Unit = {},
    onOpenTrending: () -> Unit = {},
    onOpenOnlineCollection: (String) -> Unit = {},
    onOpenPlaylist: (String) -> Unit = {},
) {
    var filterIndex by remember { mutableIntStateOf(0) }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Background).padding(bottom = 200.dp)) {
        item {
            Spacer(Modifier.height(18.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                IdentityBar("Good evening,", "Alex", onSearch = onOpenSearch)
                Spacer(Modifier.height(22.dp))
                Text(
                    buildAnnotatedString {
                        append("Good ")
                        withStyle(SpanStyle(brush = Brush.horizontalGradient(listOf(Color(0xFFFF375F), Color(0xFFff7a94))))) { append("vibes") }
                    },
                    style = MaterialTheme.typography.displayLarge,
                )
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            FilterPills(
                pills = listOf("All", "Music", "Podcasts"),
                selectedIndex = filterIndex,
                onSelected = { filterIndex = it },
            )
        }
        item { Spacer(Modifier.height(22.dp)); QuickAccessGrid(SampleData.homeQuickAccess, onOpenAlbum) }
        item {
            Spacer(Modifier.height(34.dp)); SectionHeader("Recently played", onAction = onOpenTrending)
            Spacer(Modifier.height(14.dp)); AlbumCardRow(SampleData.recentlyPlayed, onOpenAlbum)
        }
        item {
            Spacer(Modifier.height(34.dp)); SectionHeader("Made for you")
            Spacer(Modifier.height(14.dp)); AlbumCardRow(SampleData.madeForYou.map { Album(it.id, it.title, it.subtitle, it.coverIndex) }, onOpenAlbum)
        }
        item {
            Spacer(Modifier.height(34.dp)); SectionHeader("Top podcasts")
            Spacer(Modifier.height(14.dp)); AlbumCardRow(SampleData.topPodcasts.map { Album(it.id, it.title, it.subtitle, it.coverIndex) }, onOpenAlbum)
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}
