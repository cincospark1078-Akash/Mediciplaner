package com.mediciplanner.app

import com.mediciplanner.app.core.theme.AppThemeMode
import com.mediciplanner.app.core.theme.FemalePrimary
import com.mediciplanner.app.core.theme.FemaleThemeTokens
import com.mediciplanner.app.core.theme.MalePrimary
import com.mediciplanner.app.core.theme.MaleThemeTokens
import com.mediciplanner.app.core.theme.NeutralPrimary
import com.mediciplanner.app.core.theme.NeutralThemeTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MediciThemeUnitTest {

    @Test
    fun verifyThemeModesResolution() {
        assertEquals(AppThemeMode.MALE, AppThemeMode.fromKey("male"))
        assertEquals(AppThemeMode.FEMALE, AppThemeMode.fromKey("female"))
        assertEquals(AppThemeMode.NEUTRAL, AppThemeMode.fromKey("neutral"))
        assertEquals(AppThemeMode.NEUTRAL, AppThemeMode.fromKey("unknown"))
        assertEquals(AppThemeMode.NEUTRAL, AppThemeMode.fromKey(null))
    }

    @Test
    fun verifyThemeTokensColors() {
        assertEquals(MalePrimary, MaleThemeTokens.primary)
        assertEquals(FemalePrimary, FemaleThemeTokens.primary)
        assertEquals(NeutralPrimary, NeutralThemeTokens.primary)

        assertNotNull(MaleThemeTokens.primaryContainer)
        assertNotNull(FemaleThemeTokens.primaryContainer)
        assertNotNull(NeutralThemeTokens.primaryContainer)

        assertNotNull(MaleThemeTokens.surface)
        assertNotNull(FemaleThemeTokens.surface)
        assertNotNull(NeutralThemeTokens.surface)

        assertNotNull(MaleThemeTokens.textPrimary)
        assertNotNull(FemaleThemeTokens.textPrimary)
        assertNotNull(NeutralThemeTokens.textPrimary)
    }
}
