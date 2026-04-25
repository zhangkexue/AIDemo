package com.zkx.aidemo.entertainment.music

import android.content.ContentUris
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore

data class Song(val id: Long, val title: String, val artist: String, val duration: Long, val uri: Uri)

class MusicRepository(private val context: Context) {

    fun loadLocalSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                songs.add(
                    Song(
                        id = id,
                        title = cursor.getString(titleCol),
                        artist = cursor.getString(artistCol),
                        duration = cursor.getLong(durationCol),
                        uri = uri
                    )
                )
            }
        }
        return songs
    }

    fun createPlayer(song: Song): MediaPlayer? = MediaPlayer.create(context, song.uri)
}
