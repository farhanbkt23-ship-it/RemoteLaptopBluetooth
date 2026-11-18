package com.example.remotelaptop

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private val spp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null
    private var output: OutputStream? = null
    private lateinit var status: TextView
    private lateinit var deviceSpinner: Spinner
    private var devices: List<BluetoothDevice> = emptyList()
    private var lastX = 0f
    private var lastY = 0f
    private var connected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 20)
        }
        buildUi()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 20) refreshDevices()
    }

    private fun buildUi() {
        val bg = 0xFFF4F6F8.toInt()
        val ink = 0xFF17212B.toInt()
        val blue = 0xFF2563EB.toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 18)
            setBackgroundColor(bg)
        }
        status = TextView(this).apply {
            text = "● Tidak terhubung"
            textSize = 14f; setTextColor(0xFFB45309.toInt())
        }
        layout.addView(TextView(this).apply {
            text = "Remote Laptop"; textSize = 27f; setTextColor(ink); setPadding(0,0,0,5)
        })
        layout.addView(TextView(this).apply {
            text = "Kendali Windows melalui Bluetooth"; textSize = 14f; setTextColor(0xFF64748B.toInt())
        })
        layout.addView(status)
        deviceSpinner = Spinner(this)
        layout.addView(deviceSpinner)
        val connect = Button(this).apply { text = "Hubungkan perangkat"; setOnClickListener { connectSelected() } }
        layout.addView(connect)
        layout.addView(section("TOUCHPAD"))
        val pad = TextView(this).apply {
            text = "Geser jari di area ini untuk menggerakkan kursor"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(0xFF475569.toInt())
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(8, 55, 8, 55)
            setOnTouchListener { _, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { lastX=e.x; lastY=e.y }
                    MotionEvent.ACTION_MOVE -> {
                        val dx=(e.x-lastX); val dy=(e.y-lastY)
                        lastX=e.x; lastY=e.y
                        send("MOVE", "dx" to (dx*1.7f).roundToInt(), "dy" to (dy*1.7f).roundToInt())
                    }
                }; true
            }
        }
        layout.addView(pad, LinearLayout.LayoutParams(-1, 180).apply { topMargin=8 })
        val clicks = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        clicks.addView(actionButton("Klik kiri") { send("CLICK", "button" to "left") }, LinearLayout.LayoutParams(0, -2, 1f))
        clicks.addView(actionButton("Klik kanan") { send("CLICK", "button" to "right") }, LinearLayout.LayoutParams(0, -2, 1f))
        layout.addView(clicks)
        val scroll = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        scroll.addView(actionButton("↑ Scroll") { send("SCROLL", "amount" to 4) }, LinearLayout.LayoutParams(0,-2,1f))
        scroll.addView(actionButton("↓ Scroll") { send("SCROLL", "amount" to -4) }, LinearLayout.LayoutParams(0,-2,1f))
        layout.addView(scroll)
        layout.addView(section("KEYBOARD"))
        val entry = EditText(this).apply { hint="Ketik teks untuk dikirim ke laptop"; setSingleLine(false); minLines=2 }
        layout.addView(entry)
        layout.addView(actionButton("Kirim teks") {
            if (entry.text.isNotBlank()) { send("TYPE", "text" to entry.text.toString()); entry.text.clear() }
        })
        val keys = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        keys.addView(actionButton("Enter") { send("KEY", "key" to "enter") }, LinearLayout.LayoutParams(0,-2,1f))
        keys.addView(actionButton("Backspace") { send("KEY", "key" to "backspace") }, LinearLayout.LayoutParams(0,-2,1f))
        keys.addView(actionButton("Esc") { send("KEY", "key" to "esc") }, LinearLayout.LayoutParams(0,-2,1f))
        layout.addView(keys)
        layout.addView(section("MEDIA & SISTEM"))
        val media = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        media.addView(actionButton("Volume −") { send("MEDIA", "action" to "vol_down") }, LinearLayout.LayoutParams(0,-2,1f))
        media.addView(actionButton("Mute") { send("MEDIA", "action" to "mute") }, LinearLayout.LayoutParams(0,-2,1f))
        media.addView(actionButton("Volume +") { send("MEDIA", "action" to "vol_up") }, LinearLayout.LayoutParams(0,-2,1f))
        layout.addView(media)
        layout.addView(actionButton("Kunci laptop") { send("SYSTEM", "action" to "lock") })
        layout.addView(actionButton("Putuskan koneksi") { disconnect() })
        val scrollView = ScrollView(this); scrollView.addView(layout); setContentView(scrollView)
        refreshDevices()
    }

    private fun section(s: String) = TextView(this).apply {
        text=s; textSize=12f; setTextColor(0xFF64748B.toInt()); setPadding(0,18,0,5)
    }
    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text=label; textSize=12f; setOnClickListener { action() }
    }

    private fun refreshDevices() {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            devices = adapter?.bondedDevices?.toList()?.sortedBy { it.name ?: it.address } ?: emptyList()
            deviceSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
                devices.map { "${it.name ?: "Bluetooth"} (${it.address})" })
            if (devices.isEmpty()) status.text = "Pasangkan laptop dari pengaturan Bluetooth Android terlebih dahulu"
        } catch (_: SecurityException) { status.text = "Izinkan akses Bluetooth lalu buka ulang aplikasi" }
    }

    private fun connectSelected() {
        if (devices.isEmpty()) { refreshDevices(); return }
        val device = devices.getOrNull(deviceSpinner.selectedItemPosition) ?: return
        status.text = "Menghubungkan..."
        thread {
            try {
                val s = device.createRfcommSocketToServiceRecord(spp)
                s.connect()
                socket=s; output=s.outputStream; connected=true
                runOnUiThread { status.text="● Terhubung ke ${device.name}"; status.setTextColor(0xFF15803D.toInt()) }
            } catch (e: Exception) {
                connected=false
                runOnUiThread { status.text="Gagal terhubung. Periksa COM port/server laptop."; status.setTextColor(0xFFB91C1C.toInt()) }
            }
        }
    }

    private fun send(command: String, vararg args: Pair<String, Any>) {
        if (!connected || output == null) { status.text="Hubungkan ke laptop terlebih dahulu"; return }
        val obj = org.json.JSONObject().put("cmd", command)
        args.forEach { (k,v) -> obj.put(k,v) }
        thread {
            try { synchronized(this) { output?.write((obj.toString()+"\n").toByteArray(Charsets.UTF_8)); output?.flush() } }
            catch (_: Exception) { connected=false; runOnUiThread { status.text="Koneksi terputus" } }
        }
    }

    private fun disconnect() {
        connected=false
        thread { try { socket?.close() } catch (_: Exception) {} }
        socket=null; output=null; status.text="● Tidak terhubung"; status.setTextColor(0xFFB45309.toInt())
    }
    override fun onDestroy() { disconnect(); super.onDestroy() }
}
