package com.rigel.mp3p.data

import android.net.Uri

object DemoSongProvider {
    val demoSongs = listOf(
        Song(
            id = -101L,
            title = "Midnight Synthwave Horizon",
            artist = "Neon Cyber Echoes",
            album = "Futuristic Beats",
            duration = 212000L,
            contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
            albumArtUri = Uri.parse("https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=500&auto=format&fit=crop"),
            isDemo = true
        ),
        Song(
            id = -102L,
            title = "Starlight Chill Grooves",
            artist = "Lofi Stellar Drift",
            album = "Cosmic Nights",
            duration = 195000L,
            contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
            albumArtUri = Uri.parse("https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&auto=format&fit=crop"),
            isDemo = true
        ),
        Song(
            id = -103L,
            title = "Acoustic Sunset Serenade",
            artist = "Aura Breeze Trio",
            album = "Golden Hour Sessions",
            duration = 245000L,
            contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
            albumArtUri = Uri.parse("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop"),
            isDemo = true
        ),
        Song(
            id = -104L,
            title = "Electro Pulse Vibrations",
            artist = "Digital Mindscape",
            album = "Cybernetic Dreams",
            duration = 310000L,
            contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
            albumArtUri = Uri.parse("https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop"),
            isDemo = true
        ),
        Song(
            id = -105L,
            title = "Velvet Jazz Atmosphere",
            artist = "The Midnight Ensemble",
            album = "Urban Cafe Melodies",
            duration = 180000L,
            contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"),
            albumArtUri = Uri.parse("https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop"),
            isDemo = true
        )
    )
}
