package com.marusys.hesap.presentation.screen

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marusys.hesap.R
import com.marusys.hesap.presentation.SettingsActivity
import com.marusys.hesap.presentation.viewmodel.MainViewModel

@Composable
fun AudioScreen(
    viewModel: MainViewModel,
) {
    val resultText by viewModel.resultText.collectAsState()
    val context = LocalContext.current
    val isAudioServiceRunning by viewModel.isAudioServiceRunning.collectAsState()

    val backgroundColor =
        if (isAudioServiceRunning) {
            Brush.verticalGradient(listOf(Color.White, Color.Gray))
        } else {
            Brush.verticalGradient(listOf(Color.White, Color.White))
        }

    Box(
        modifier = Modifier
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        // 설정 아이콘을 오른쪽 위에 배치
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "설정 아이콘",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clickable {
                    val intent = Intent(context, SettingsActivity::class.java)
                    context.startActivity(intent)
                }
                .size(24.dp)
        )

        // Content를 중앙 정렬
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(R.drawable.marusys), contentDescription = "마르시스")
            Spacer(modifier = Modifier.padding(32.dp))
            Text(
                "무엇을 도와드릴까요?",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Text(resultText, fontSize = 32.sp, fontWeight = FontWeight.Normal)
        }
    }
}
