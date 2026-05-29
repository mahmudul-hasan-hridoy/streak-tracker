package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
      primary = White,
      secondary = Gray300,
      tertiary = Gray600,
      background = Black,
      surface = Gray900,
      onPrimary = Black,
      onSecondary = Black,
      onTertiary = Black,
      onBackground = White,
      onSurface = White,
  )

private val LightColorScheme =
  lightColorScheme(
      primary = Black,
      secondary = Gray800,
      tertiary = Gray600,
      background = White,
      surface = Gray300,
      onPrimary = White,
      onSecondary = White,
      onTertiary = White,
      onBackground = Black,
      onSurface = Black,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // disabled for pure monochrome
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
