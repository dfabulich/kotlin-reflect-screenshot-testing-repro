package com.example.kotlinreflectrepro

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.kotlinreflectrepro.ui.theme.KotlinReflectReproTheme

@PreviewTest
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KotlinReflectReproTheme {
        Surface {
            Greeting("Android")
        }
    }
}
