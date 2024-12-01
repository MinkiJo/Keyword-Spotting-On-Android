package com.marusys.hesap.feature

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// 메모리 관련 함수 모음
class MemoryUsageManager {
    fun getMemoryUsage(context: Context): String {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val pid = Process.myPid()
        val pInfo = activityManager.getProcessMemoryInfo(intArrayOf(pid))[0]

        val totalPss = pInfo.totalPss / 1024 // KB to MB
        val privateDirty = pInfo.totalPrivateDirty / 1024 // KB to MB

        return "현재 앱 메모리 사용량:\n" +
                "앱이 사용하는 총 메모리양, 공유 메모리 포함: $totalPss MB\n" +
                "앱이 독점적으로 사용하는 메모리양: $privateDirty MB"
    }
}