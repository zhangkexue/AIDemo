package com.zkx.aidemo.entertainment

import android.content.Intent
import android.os.Bundle
import com.zkx.aidemo.entertainment.game1024.Game1024Activity
import com.zkx.aidemo.entertainment.music.MusicPlayerActivity
import com.zkx.aidemo.entertainment.tetris.TetrisActivity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class EntertainmentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EntertainmentScreen(
                onGame1024Click = {
                    startActivity(Intent(this, Game1024Activity::class.java))
                },
                onTetrisClick = {
                    startActivity(Intent(this, TetrisActivity::class.java))
                },
                onMusicClick = {
                    startActivity(Intent(this, MusicPlayerActivity::class.java))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntertainmentScreen(
    onGame1024Click: () -> Unit,
    onTetrisClick: () -> Unit,
    onMusicClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("娱乐") })
        }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            item { GameListItem(title = "1024", onClick = onGame1024Click) }
            item { GameListItem(title = "俄罗斯方块", onClick = onTetrisClick) }
            item { GameListItem(title = "影音娱乐", onClick = onMusicClick) }
        }
    }
}

@Composable
fun GameListItem(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        ListItem(headlineContent = { Text(title) })
    }
}
