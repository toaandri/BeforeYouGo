package com.toaandri.beforeyougo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLanguageTest {
    @Test fun selectedLanguageIsSavedAndUsedAfterReinitialization() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val previous = AppText.locale.language
        try {
            AppText.selectLanguage(context, "fr")
            AppText.initialize(context)
            assertEquals("fr", AppText.locale.language)
            assertEquals("Mon départ", uiText(R.string.ui_my_departure))
            AppText.selectLanguage(context, "en")
            AppText.initialize(context)
            assertEquals("en", AppText.locale.language)
            assertEquals("My departure", uiText(R.string.ui_my_departure))
        } finally { AppText.selectLanguage(context, previous) }
    }

    @Test fun clearingTheAndroidLanguageChoiceReturnsToEnglish() {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val previous = AppText.locale.language
        try {
            AppText.selectLanguage(context, "fr")
            context.getSystemService(android.app.LocaleManager::class.java).applicationLocales = android.os.LocaleList.getEmptyLocaleList()
            AppText.initialize(context)
            assertEquals("en", AppText.locale.language)
            assertEquals("My departure", uiText(R.string.ui_my_departure))
        } finally { AppText.selectLanguage(context, previous) }
    }

    @Test fun translatedCatalogRecognizesExistingNamesInBothLanguages() {
        val umbrella = objectCatalog.single { it.key == "umbrella" }
        assertTrue(umbrella.matchesTitle("Umbrella"))
        assertTrue(umbrella.matchesTitle("Parapluie"))
        assertFalse(umbrella.matchesTitle("Keys"))
    }
}
