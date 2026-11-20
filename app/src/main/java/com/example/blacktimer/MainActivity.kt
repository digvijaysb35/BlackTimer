package com.example.blacktimer

import android.app.*
import android.content.*
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import android.app.AlertDialog

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("timer", MODE_PRIVATE) }
    private lateinit var hours: EditText
    private lateinit var minutes: EditText
    private lateinit var seconds: EditText
    private lateinit var status: TextView
    private lateinit var volume: SeekBar
    private val green = Color.rgb(184, 243, 107)
    private val muted = Color.rgb(150, 150, 150)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK; window.navigationBarColor = Color.BLACK
        createChannels()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 20, 24, 20); setBackgroundColor(Color.BLACK) }
        val header = TextView(this).apply { text = "BLACK TIMER"; textSize = 14f; letterSpacing = .18f; setTextColor(green); gravity = Gravity.CENTER; setPadding(0, 12, 0, 26) }
        root.addView(header)
        status = TextView(this).apply { text = "READY"; textSize = 12f; setTextColor(muted); gravity = Gravity.CENTER; setPadding(0, 0, 0, 14) }
        root.addView(status)
        val timeRow = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        hours = timeInput("00"); minutes = timeInput("05"); seconds = timeInput("00")
        timeRow.addView(hours); timeRow.addView(separator(":")); timeRow.addView(minutes); timeRow.addView(separator(":")); timeRow.addView(seconds)
        root.addView(timeRow)
        val labels = TextView(this).apply { text = "HOURS                 MINUTES                SECONDS"; textSize = 9f; setTextColor(muted); gravity = Gravity.CENTER; setPadding(0, 4, 0, 26) }
        root.addView(labels)
        root.addView(section("QUICK PRESETS"))
        val presetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, 8, 0, 8) }
        listOf("1 min" to 60L, "5 min" to 300L, "10 min" to 600L, "15 min" to 900L).forEach { (label, sec) -> presetRow.addView(smallButton(label) { setTime(sec) }) }
        root.addView(presetRow)
        val customRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        customRow.addView(smallButton("+ Save preset") { savePreset() }); customRow.addView(smallButton("My presets") { showPresets() })
        root.addView(customRow)
        root.addView(section("RINGTONE"))
        val ringRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        ringRow.addView(smallButton("System ring") { chooseSystemRing() }, LinearLayout.LayoutParams(0, -2, 1f))
        ringRow.addView(smallButton("Custom audio") { chooseCustomRing() }, LinearLayout.LayoutParams(0, -2, 1f))
        ringRow.addView(smallButton("Use default") { prefs.edit().remove("ring_uri").apply(); toast("Default alarm selected") }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(ringRow)
        val volumeLabel = TextView(this).apply { text = "APP RING VOLUME   ${prefs.getInt("ring_volume", 80)}%"; textSize = 11f; setTextColor(muted); setPadding(2, 16, 0, 2) }
        root.addView(volumeLabel)
        volume = SeekBar(this).apply { max = 100; progress = prefs.getInt("ring_volume", 80); progressTintList = android.content.res.ColorStateList.valueOf(green); thumbTintList = android.content.res.ColorStateList.valueOf(green) }
        volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) { volumeLabel.text = "APP RING VOLUME   $p%"; if (user) prefs.edit().putInt("ring_volume", p).apply() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        root.addView(volume)
        root.addView(section("TIMER CONTROLS"))
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL; setPadding(0, 18, 0, 8) }
        controls.addView(actionButton("START", green, Color.BLACK) { startTimer() }, LinearLayout.LayoutParams(0, 58, 1f))
        controls.addView(actionButton("STOP", Color.rgb(40,40,40), Color.WHITE) { stopTimer() }, LinearLayout.LayoutParams(0, 58, 1f).apply { leftMargin = 10 })
        root.addView(controls)
        val secondary = LinearLayout(this).apply { gravity=Gravity.CENTER; orientation=LinearLayout.HORIZONTAL }
        secondary.addView(smallButton("PAUSE") { startService(Intent(this,TimerService::class.java).setAction(TimerService.ACTION_PAUSE)); status.text="PAUSED" }, LinearLayout.LayoutParams(0,-2,1f))
        secondary.addView(smallButton("RESUME") { startService(Intent(this,TimerService::class.java).setAction(TimerService.ACTION_RESUME)); status.text="COUNTDOWN RUNNING" }, LinearLayout.LayoutParams(0,-2,1f))
        secondary.addView(smallButton("SNOOZE 5M") { startService(Intent(this,AlarmService::class.java).setAction(AlarmService.ACTION_SNOOZE)); }, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(secondary)
        root.addView(actionButton("TEST RING", Color.rgb(24,24,24), green) { startService(Intent(this, AlarmService::class.java).setAction(AlarmService.ACTION_START)) }.apply { setPadding(0, 8, 0, 8) })
        root.addView(section("VOLUME BUTTON CONTROL"))
        val help = TextView(this).apply { text = "Enable the accessibility service below to use Volume Up or Down to stop the timer or silence the alarm, including with the screen off. Android may restrict key capture on some devices."; textSize = 12f; setTextColor(muted); setPadding(0, 6, 0, 8) }
        root.addView(help)
        root.addView(actionButton("ENABLE VOLUME BUTTON CONTROL", Color.rgb(24,24,24), green) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        val scroll = ScrollView(this); scroll.addView(root); setContentView(scroll)
    }

    private fun timeInput(value: String) = EditText(this).apply {
        setText(value); setTextColor(Color.WHITE); textSize = 34f; gravity = Gravity.CENTER; setSingleLine(); inputType = 2
        background = null; setSelectAllOnFocus(true); filters = arrayOf(android.text.InputFilter.LengthFilter(2)); minWidth = 70
    }
    private fun separator(s: String) = TextView(this).apply { text=s; textSize=32f; setTextColor(muted); gravity=Gravity.CENTER; setPadding(2,0,2,0) }
    private fun section(s: String) = TextView(this).apply { text=s; textSize=11f; letterSpacing=.12f; setTextColor(green); setPadding(0,22,0,4) }
    private fun smallButton(s: String, click: () -> Unit) = Button(this).apply { text=s; textSize=10f; isAllCaps=false; setTextColor(Color.WHITE); backgroundTintList=android.content.res.ColorStateList.valueOf(Color.rgb(28,28,28)); setOnClickListener { click() } }
    private fun actionButton(s:String, bg:Int, fg:Int, click:()->Unit) = Button(this).apply { text=s; textSize=12f; isAllCaps=false; setTextColor(fg); backgroundTintList=android.content.res.ColorStateList.valueOf(bg); setOnClickListener { click() } }
    private fun setTime(total: Long) { hours.setText("%02d".format(total/3600)); minutes.setText("%02d".format((total%3600)/60)); seconds.setText("%02d".format(total%60)) }
    private fun getDuration(): Long { val h=hours.text.toString().toLongOrNull()?:0; val m=minutes.text.toString().toLongOrNull()?:0; val s=seconds.text.toString().toLongOrNull()?:0; return (h*3600+m*60+s).coerceAtMost(99L*3600+59*60+59) }
    private fun startTimer() { val duration=getDuration(); if(duration<=0){toast("Set a time first");return}; val i=Intent(this,TimerService::class.java).setAction(TimerService.ACTION_START).putExtra("duration",duration); if(Build.VERSION.SDK_INT>=26) startForegroundService(i) else startService(i); status.text="COUNTDOWN RUNNING"; toast("Timer started") }
    private fun stopTimer() { startService(Intent(this,TimerService::class.java).setAction(TimerService.ACTION_STOP)); startService(Intent(this,AlarmService::class.java).setAction(AlarmService.ACTION_STOP)); status.text="STOPPED" }
    private fun chooseCustomRing() { val i=Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,43) }
    private fun chooseSystemRing() { val i=Intent(RingtoneManager.ACTION_RINGTONE_PICKER).putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM).putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,"Choose alarm sound").putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false); startActivityForResult(i,42) }
    @Deprecated("Deprecated in Android API") override fun onActivityResult(req:Int,res:Int,data:Intent?) { super.onActivityResult(req,res,data); if(req==42 && res==RESULT_OK){ val uri=data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI); prefs.edit().putString("ring_uri",uri?.toString()).apply(); toast("System ringtone saved") }; if(req==43 && res==RESULT_OK){ val uri=data?.data; if(uri!=null){ try{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){}; prefs.edit().putString("ring_uri",uri.toString()).apply(); toast("Custom ringtone saved") } } }
    private fun savePreset() { val d=getDuration(); if(d<=0){toast("Set a time first");return}; val name=EditText(this).apply { hint="Preset name"; setTextColor(Color.WHITE); setHintTextColor(muted) }; AlertDialog.Builder(this).setTitle("Save preset").setView(name).setPositiveButton("Save"){_,_-> val p=prefs.getStringSet("presets", emptySet())!!.toMutableSet(); p.add("${name.text.toString().ifBlank { "${d}s" }}|$d"); prefs.edit().putStringSet("presets",p).apply(); toast("Preset saved") }.setNegativeButton("Cancel",null).show() }
    private fun showPresets() { val items=prefs.getStringSet("presets", emptySet())!!.toList(); if(items.isEmpty()){toast("No saved presets yet");return}; AlertDialog.Builder(this).setTitle("Your presets").setItems(items.map{it.substringBefore('|')}.toTypedArray()){_,which-> setTime(items[which].substringAfter('|').toLong()) }.setNeutralButton("Manage"){_,_-> managePresets(items)}.setNegativeButton("Close",null).show() }
    private fun managePresets(items:List<String>) { if(items.isEmpty())return; AlertDialog.Builder(this).setTitle("Delete a preset").setItems(items.map{it.substringBefore('|')}.toTypedArray()){_,which-> val p=items.toMutableSet(); p.remove(items[which]); prefs.edit().putStringSet("presets",p).apply(); toast("Deleted") }.show() }
    private fun createChannels(){ if(Build.VERSION.SDK_INT>=26){ val nm=getSystemService(NotificationManager::class.java); nm.createNotificationChannel(NotificationChannel("timer","Timer",NotificationManager.IMPORTANCE_LOW)); nm.createNotificationChannel(NotificationChannel("alarm","Alarm",NotificationManager.IMPORTANCE_HIGH)) } }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
