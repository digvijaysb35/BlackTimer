package com.example.blacktimer

import android.app.*
import android.content.*
import android.os.*
import android.view.*

class TimerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var endAt = 0L
    private var wakeLock: PowerManager.WakeLock? = null
    private val tick = object : Runnable { override fun run() { val left=endAt-SystemClock.elapsedRealtime(); if(left<=0){ finishTimer(); return }; updateNotification(left); handler.postDelayed(this,1000) } }
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){ ACTION_STOP -> stopTimer(); ACTION_PAUSE -> pauseTimer(); ACTION_RESUME -> { val left=getSharedPreferences("timer",MODE_PRIVATE).getLong("remaining_ms",0L); if(left>0) beginMillis(left) }; ACTION_START -> { val duration=intent.getLongExtra("duration",60L); begin(duration) }; else -> { val saved=getSharedPreferences("timer",MODE_PRIVATE).getLong("end_at",0L); if(saved>SystemClock.elapsedRealtime()){endAt=saved; startForeground(10,notification("Timer running",true)); handler.removeCallbacks(tick); handler.post(tick)} else stopSelf() } }
        return START_STICKY
    }
    private fun begin(seconds:Long){ beginMillis(seconds*1000) }
    private fun beginMillis(ms:Long){ handler.removeCallbacks(tick); endAt=SystemClock.elapsedRealtime()+ms; getSharedPreferences("timer",MODE_PRIVATE).edit().remove("remaining_ms").apply(); getSharedPreferences("timer",MODE_PRIVATE).edit().putLong("end_at",endAt).apply(); acquireWakeLock(); startForeground(10,notification("Timer running",true)); scheduleExactAlarm(seconds); handler.post(tick) }
    private fun scheduleExactAlarm(seconds:Long){ val am=getSystemService(ALARM_SERVICE) as AlarmManager; val pi=PendingIntent.getBroadcast(this,7,Intent(this,AlarmReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); try { if(Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,endAt,pi) else am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,endAt,pi) }catch(_:Exception){ am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,endAt,pi) } }
    private fun updateNotification(ms:Long){ val h=ms/3600000; val m=(ms%3600000)/60000; val s=(ms%60000)/1000; val txt=if(h>0) "%02d:%02d:%02d".format(h,m,s) else "%02d:%02d".format(m,s); (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(10,notification(txt,true)) }
    private fun notification(text:String,running:Boolean):Notification { val stop=PendingIntent.getService(this,8,Intent(this,TimerService::class.java).setAction(ACTION_STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); return Notification.Builder(this,"timer").setContentTitle("Black Timer").setContentText(text).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setOngoing(running).addAction(Notification.Action.Builder(null,"Stop",stop).build()).build() }
    private fun finishTimer(){ handler.removeCallbacks(tick); getSharedPreferences("timer",MODE_PRIVATE).edit().remove("end_at").apply(); cancelExactAlarm(); releaseWakeLock(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); val i=Intent(this,AlarmService::class.java).setAction(AlarmService.ACTION_START); if(Build.VERSION.SDK_INT>=26) startForegroundService(i) else startService(i) }
    private fun pauseTimer(){ val left=(endAt-SystemClock.elapsedRealtime()).coerceAtLeast(0L); handler.removeCallbacks(tick); getSharedPreferences("timer",MODE_PRIVATE).edit().putLong("remaining_ms",left).putLong("end_at",0L).apply(); cancelExactAlarm(); releaseWakeLock(); (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(10,notification("Paused",false)) }
    private fun stopTimer(){ handler.removeCallbacks(tick); getSharedPreferences("timer",MODE_PRIVATE).edit().remove("end_at").apply(); cancelExactAlarm(); releaseWakeLock(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    private fun cancelExactAlarm(){ val pi=PendingIntent.getBroadcast(this,7,Intent(this,AlarmReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); (getSystemService(ALARM_SERVICE) as AlarmManager).cancel(pi) }
    private fun acquireWakeLock(){ if(wakeLock?.isHeld!=true) wakeLock=(getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"BlackTimer:Countdown").apply{acquire(10*60*60*1000L)} }
    private fun releaseWakeLock(){ if(wakeLock?.isHeld==true) wakeLock?.release(); wakeLock=null }
    override fun onDestroy(){handler.removeCallbacks(tick); releaseWakeLock(); super.onDestroy()}
    companion object { const val ACTION_START="blacktimer.START"; const val ACTION_STOP="blacktimer.STOP"; const val ACTION_PAUSE="blacktimer.PAUSE"; const val ACTION_RESUME="blacktimer.RESUME" }
}
