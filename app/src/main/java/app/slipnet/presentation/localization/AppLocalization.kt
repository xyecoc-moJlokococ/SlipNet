package app.slipnet.presentation.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import app.slipnet.data.local.datastore.AppLanguage

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.RUSSIAN }

@Composable
fun tx(en: String, ru: String): String {
    return if (LocalAppLanguage.current == AppLanguage.RUSSIAN) ru else en
}
