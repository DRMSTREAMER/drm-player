package com.drm.player

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.drm.player.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupUI()
        setupSampleStreams()
    }
    
    private fun setupUI() {
        binding.playButton.setOnClickListener {
            val url = binding.streamUrlInput.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a stream URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val drmType = when (binding.drmTypeSpinner.selectedItemPosition) {
                1 -> "widevine"
                2 -> "clearkey"
                else -> "none"
            }
            
            val licenseUrl = binding.licenseUrlInput.text.toString().trim()
            val keyId = binding.keyIdInput.text.toString().trim()
            val keyValue = binding.keyValueInput.text.toString().trim()
            
            startPlayer(url, drmType, licenseUrl, keyId, keyValue)
        }
        
        // Toggle DRM config visibility
        binding.drmTypeSpinner.setOnItemSelectedListener { position ->
            binding.widevineConfig.visibility = if (position == 1) android.view.View.VISIBLE else android.view.View.GONE
            binding.clearkeyConfig.visibility = if (position == 2) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
    
    private fun setupSampleStreams() {
        val samples = listOf(
            SampleStream("Clear Stream (Akamai)", 
                "https://dash.akamaized.net/envivio/EnvisivoDash3/manifest.mpd", "none"),
            SampleStream("Angel One (Clear)", 
                "https://storage.googleapis.com/shaka-demo-assets/angel-one/dash.mpd", "none"),
            SampleStream("Angel One (Widevine)", 
                "https://storage.googleapis.com/shaka-demo-assets/angel-one-widevine/dash.mpd", 
                "widevine", "https://cwip-shaka-proxy.appspot.com/no_auth")
        )
        
        val adapter = SampleAdapter(samples) { sample ->
            binding.streamUrlInput.setText(sample.url)
            binding.drmTypeSpinner.setSelection(
                when (sample.drmType) {
                    "widevine" -> 1
                    "clearkey" -> 2
                    else -> 0
                }
            )
            sample.licenseUrl?.let { binding.licenseUrlInput.setText(it) }
        }
        binding.sampleList.adapter = adapter
    }
    
    private fun startPlayer(
        url: String, 
        drmType: String, 
        licenseUrl: String,
        keyId: String,
        keyValue: String
    ) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("STREAM_URL", url)
            putExtra("DRM_TYPE", drmType)
            putExtra("LICENSE_URL", licenseUrl)
            putExtra("KEY_ID", keyId)
            putExtra("KEY_VALUE", keyValue)
        }
        startActivity(intent)
    }
    
    data class SampleStream(
        val name: String,
        val url: String,
        val drmType: String,
        val licenseUrl: String? = null
    )
}

// Extension for spinner
private fun android.widget.Spinner.setOnItemSelectedListener(callback: (Int) -> Unit) {
    onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
            callback(position)
        }
        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
    }
}

// Simple RecyclerView adapter placeholder
class SampleAdapter(
    private val items: List<MainActivity.SampleStream>,
    private val onClick: (MainActivity.SampleStream) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<SampleAdapter.ViewHolder>() {
    
    class ViewHolder(val view: android.widget.TextView) : 
        androidx.recyclerview.widget.RecyclerView.ViewHolder(view)
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val tv = android.widget.TextView(parent.context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(32, 24, 32, 24)
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
        }
        return ViewHolder(tv)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.view.text = item.name
        holder.view.setOnClickListener { onClick(item) }
    }
    
    override fun getItemCount() = items.size
}
