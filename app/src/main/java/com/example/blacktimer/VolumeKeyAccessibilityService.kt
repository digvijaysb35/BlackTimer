package com.example.blacktimer

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.os.Build

class VolumeKeyAccessibilityService:AccessibilityService(){
    override fun onAccessibilityEvent(event:AccessibilityEvent?){}
    override fun onInterrupt(){}
    override fun onKeyEvent(event:KeyEvent):Boolean {
        if(event.action!=KeyEvent.ACTION_DOWN || event.repeatCount>0)return false
        if(event.keyCode==KeyEvent.KEYCODE_VOLUME_UP || event.keyCode==KeyEvent.KEYCODE_VOLUME_DOWN){
            val p=getSharedPreferences("timer",MODE_PRIVATE); val ringing=p.getBoolean("alarm_active",false)
            if(ringing){ startService(Intent(this,AlarmService::class.java).setAction(AlarmService.ACTION_STOP)); return true }
            val end=p.getLong("end_at",0L)
            if(end>android.os.SystemClock.elapsedRealtime()){ startService(Intent(this,TimerService::class.java).setAction(TimerService.ACTION_STOP)); return true }
        }
        return false
    }
}
