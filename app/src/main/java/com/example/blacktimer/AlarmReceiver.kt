package com.example.blacktimer

import android.content.*
import android.os.Build

class AlarmReceiver:BroadcastReceiver(){ override fun onReceive(context:Context,intent:Intent?){ val end=context.getSharedPreferences("timer",Context.MODE_PRIVATE).getLong("end_at",0L); if(end>0L && end<=android.os.SystemClock.elapsedRealtime()){ val i=Intent(context,AlarmService::class.java).setAction(AlarmService.ACTION_START); if(Build.VERSION.SDK_INT>=26)context.startForegroundService(i) else context.startService(i) } } }
