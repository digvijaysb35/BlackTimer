package com.example.blacktimer

import android.app.*
import android.content.*
import android.media.*
import android.net.Uri
import android.os.*

class AlarmService : Service() {
    private var player: MediaPlayer?=null
    private var wakeLock:PowerManager.WakeLock?=null
    override fun onBind(intent:Intent?)=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action==ACTION_STOP){ stopAlarm(); return START_NOT_STICKY }; if(intent?.action==ACTION_SNOOZE){ stopAlarm(); val t=Intent(this,TimerService::class.java).setAction(TimerService.ACTION_START).putExtra("duration",300L); if(Build.VERSION.SDK_INT>=26)startForegroundService(t) else startService(t); return START_NOT_STICKY }
        if(player?.isPlaying==true)return START_STICKY
        getSharedPreferences("timer",MODE_PRIVATE).edit().putBoolean("alarm_active",true).apply()
        startForeground(11,Notification.Builder(this,"alarm").setContentTitle("Timer finished").setContentText("Tap stop to silence the alarm").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setOngoing(true).build())
        wakeLock=(getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"BlackTimer:Alarm").apply{acquire(30*60*1000L)}
        try {
            val prefs=getSharedPreferences("timer",MODE_PRIVATE); val uri=prefs.getString("ring_uri",null)?.let{Uri.parse(it)} ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            player=MediaPlayer().apply { setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()); setDataSource(this@AlarmService,uri); isLooping=true; val v=prefs.getInt("ring_volume",80)/100f; setVolume(v,v); setOnPreparedListener{it.start()}; prepareAsync() }
        }catch(_:Exception){ stopAlarm() }
        return START_STICKY
    }
    private fun stopAlarm(){ getSharedPreferences("timer",MODE_PRIVATE).edit().putBoolean("alarm_active",false).apply(); try{player?.stop()}catch(_:Exception){}; player?.release(); player=null; if(wakeLock?.isHeld==true)wakeLock?.release(); wakeLock=null; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onDestroy(){player?.release(); player=null; if(wakeLock?.isHeld==true)wakeLock?.release(); super.onDestroy()}
    companion object { const val ACTION_START="blacktimer.ALARM_START"; const val ACTION_STOP="blacktimer.ALARM_STOP"; const val ACTION_SNOOZE="blacktimer.ALARM_SNOOZE" }
}
