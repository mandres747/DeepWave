package de.binauralbeats.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The marker is written once and read back much later, by a version that does
 * not exist yet. Getting it wrong is not visible until then, so the rules are
 * pinned down here.
 */
class FirstRunMarkerTest {

    @Test
    fun `fresh install records the version it started on`() {
        assertEquals(
            4,
            FirstRunMarker.resolve(stored = null, hasEarlierData = false, currentVersionCode = 4)
        )
    }

    @Test
    fun `install that predates the marker is recorded as legacy`() {
        // Someone who ran an earlier version and only now updates arrives with
        // no marker but with settings. Recording the current version code here
        // would file a long-standing user as brand new.
        assertEquals(
            FirstRunMarker.PRE_MARKER,
            FirstRunMarker.resolve(stored = null, hasEarlierData = true, currentVersionCode = 9)
        )
    }

    @Test
    fun `an existing marker is never overwritten`() {
        assertNull(
            FirstRunMarker.resolve(stored = 4, hasEarlierData = false, currentVersionCode = 12)
        )
        assertNull(
            FirstRunMarker.resolve(stored = FirstRunMarker.PRE_MARKER, hasEarlierData = true, currentVersionCode = 12)
        )
    }

    @Test
    fun `legacy marker sorts below every real version code`() {
        // The point of PRE_MARKER: "did this user predate version X" must
        // answer yes for every X a future release could ask about.
        assertEquals(true, FirstRunMarker.PRE_MARKER < 1)
    }
}
