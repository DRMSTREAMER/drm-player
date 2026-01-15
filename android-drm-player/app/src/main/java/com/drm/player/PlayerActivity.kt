package com.drm.player

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.PlayerView
import com.drm.player.databinding.ActivityPlayerBinding
import java.util.UUID

class PlayerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    
    private var streamUrl: String = ""
    private var drmType: String = "none"
    private var licenseUrl: String = ""
    private var keyId: String = ""
    private var keyValue: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fullscreen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get intent extras
        streamUrl = intent.getStringExtra("STREAM_URL") ?: ""
        drmType = intent.getStringExtra("DRM_TYPE") ?: "none"
        licenseUrl = intent.getStringExtra("LICENSE_URL") ?: ""
        keyId = intent.getStringExtra("KEY_ID") ?: ""
        keyValue = intent.getStringExtra("KEY_VALUE") ?: ""
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.backButton.setOnClickListener { finish() }
        
        binding.drmBadge.text = when (drmType) {
            "widevine" -> "Widevine"
            "clearkey" -> "ClearKey"
            else -> "Clear"
        }
    }
    
    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }
    
    override fun onResume() {
        super.onResume()
        hideSystemUI()
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer()
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }
    
    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }
    
    private fun initializePlayer() {
        if (streamUrl.isEmpty()) {
            binding.statusText.text = "No stream URL"
            return
        }
        
        binding.loadingView.visibility = View.VISIBLE
        binding.statusText.text = "Loading..."
        
        try {
            val mediaSource = buildMediaSource()
            
            player = ExoPlayer.Builder(this)
                .build()
                .also { exo ->
                    binding.playerView.player = exo
                    
                    exo.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            when (state) {
                                Player.STATE_BUFFERING -> {
                                    binding.loadingView.visibility = View.VISIBLE
                                    binding.statusText.text = "Buffering..."
                                }
                                Player.STATE_READY -> {
                                    binding.loadingView.visibility = View.GONE
                                    binding.statusText.text = "Playing"
                                    updateVideoInfo()
                                }
                                Player.STATE_ENDED -> {
                                    binding.statusText.text = "Ended"
                                }
                                Player.STATE_IDLE -> {
                                    binding.statusText.text = "Idle"
                                }
                            }
                        }
                        
                        override fun onPlayerError(error: PlaybackException) {
                            binding.loadingView.visibility = View.GONE
                            binding.statusText.text = "Error: ${error.message}"
                        }
                    })
                    
                    exo.setMediaSource(mediaSource)
                    exo.prepare()
                    exo.playWhenReady = true
                }
                
        } catch (e: Exception) {
            binding.loadingView.visibility = View.GONE
            binding.statusText.text = "Error: ${e.message}"
        }
    }
    
    private fun buildMediaSource(): MediaSource {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("DRMPlayer/1.0")
        
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(Uri.parse(streamUrl))
        
        // Configure DRM
        when (drmType) {
            "widevine" -> {
                if (licenseUrl.isNotEmpty()) {
                    mediaItemBuilder.setDrmConfiguration(
                        MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                            .setLicenseUri(licenseUrl)
                            .build()
                    )
                }
            }
            "clearkey" -> {
                if (keyId.isNotEmpty() && keyValue.isNotEmpty()) {
                    // ClearKey uses a specific format
                    val clearKeyJson = """{"keys":[{"kty":"oct","k":"$keyValue","kid":"$keyId"}],"type":"temporary"}"""
                    mediaItemBuilder.setDrmConfiguration(
                        MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                            .setLicenseRequestHeaders(mapOf("Content-Type" to "application/json"))
                            .build()
                    )
                }
            }
        }
        
        return DashMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItemBuilder.build())
    }
    
    private fun updateVideoInfo() {
        player?.let { exo ->
            val format = exo.videoFormat
            if (format != null) {
                binding.resolutionText.text = "${format.width}x${format.height}"
                binding.bitrateText.text = "${format.bitrate / 1000} kbps"
            }
        }
    }
    
    private fun releasePlayer() {
        player?.let { exo ->
            exo.release()
        }
        player = null
    }
    
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }
}
