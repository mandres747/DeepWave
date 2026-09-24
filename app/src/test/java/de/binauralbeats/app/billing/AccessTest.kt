package de.binauralbeats.app.billing

import de.binauralbeats.app.billing.Entitlements.Companion.PRODUCT_COMPLETE
import de.binauralbeats.app.billing.Entitlements.Companion.PRODUCT_PREMIUM
import de.binauralbeats.app.billing.Entitlements.Companion.PRODUCT_RHYTHM_LAYER
import de.binauralbeats.app.billing.Entitlements.Companion.PRODUCT_WAKE_ALARM
import de.binauralbeats.app.data.FirstRunMarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Getting this wrong either takes away what someone paid for or gives the
 * whole app away - both invisible until the price switch in Play Console.
 */
class AccessTest {

    private fun access(owned: Set<String> = emptySet(), firstSeen: Int? = Access.FREEMIUM_VERSION_CODE) =
        Access.resolve(owned, firstSeen, premiumInBuild = true, rhythmInBuild = true, wakeInBuild = true)

    @Test
    fun `a new free install gets nothing but the bundle offer`() {
        val a = access()
        assertFalse(a.premium)
        assertFalse(a.rhythm)
        assertFalse(a.wake)
        assertTrue(a.offerBundle)
    }

    @Test
    fun `everyone who ran the paid app keeps Premium`() {
        for (code in listOf(FirstRunMarker.PRE_MARKER, 4, 8, 9)) {
            assertTrue("firstSeen=$code", access(firstSeen = code).premium)
        }
        assertFalse(access(firstSeen = Access.FREEMIUM_VERSION_CODE).premium)
        assertFalse(access(firstSeen = 12).premium)
    }

    @Test
    fun `previous buyers keep Premium but not the add-ons they never bought`() {
        val a = access(firstSeen = 3)
        assertTrue(a.premium)
        assertFalse(a.rhythm)
        assertFalse(a.wake)
        assertFalse("no bundle for someone who already has Premium", a.offerBundle)
    }

    @Test
    fun `an unread marker is not mistaken for a previous buyer`() {
        assertFalse(access(firstSeen = null).premium)
    }

    @Test
    fun `each product unlocks only itself`() {
        assertEquals(Access(premium = true, rhythm = false, wake = false, offerBundle = false), access(setOf(PRODUCT_PREMIUM)))
        assertEquals(Access(premium = false, rhythm = true, wake = false, offerBundle = false), access(setOf(PRODUCT_RHYTHM_LAYER)))
        assertEquals(Access(premium = false, rhythm = false, wake = true, offerBundle = false), access(setOf(PRODUCT_WAKE_ALARM)))
    }

    @Test
    fun `the bundle unlocks everything`() {
        assertEquals(Access(premium = true, rhythm = true, wake = true, offerBundle = false), access(setOf(PRODUCT_COMPLETE)))
    }

    @Test
    fun `the FOSS build unlocks nothing, whatever is owned`() {
        val a = Access.resolve(
            setOf(PRODUCT_COMPLETE), FirstRunMarker.PRE_MARKER,
            premiumInBuild = false, rhythmInBuild = false, wakeInBuild = false
        )
        assertEquals(Access.NONE, a)
    }
}
