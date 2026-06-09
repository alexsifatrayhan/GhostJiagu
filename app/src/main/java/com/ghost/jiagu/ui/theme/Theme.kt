package com.ghost.jiagu.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val CyberDarkColorScheme = darkColorScheme(
  primary = CyberCyan,
  secondary = CyberPurple,
  tertiary = CyberGreen,
  background = CyberDarkBase,
  surface = CyberDarkBase,
  onPrimary = Color.Black,
  onSecondary = Color.White,
  onBackground = Color.White,
  onSurface = Color.White,
  primaryContainer = Color(0x3300F0FF),
  secondaryContainer = Color(0x33BD00FF)
)

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme by default
  dynamicColor: Boolean = false, // Force custom cyberpunk style
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) CyberDarkColorScheme else CyberDarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
