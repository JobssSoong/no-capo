package com.example.myapplication.ui.guitar.chord

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordEngineTest {

    @Test
    fun `parse common chord names`() {
        assertEquals(0 to ChordType.MAJOR, parseChordName("C"))
        assertEquals(0 to ChordType.MINOR, parseChordName("Cm"))
        assertEquals(0 to ChordType.MAJOR7, parseChordName("Cmaj7"))
        assertEquals(0 to ChordType.DOMINANT7, parseChordName("C7"))
        assertEquals(0 to ChordType.MINOR7, parseChordName("Cm7"))
        assertEquals(0 to ChordType.HALF_DIMINISHED, parseChordName("Cm7b5"))
        assertEquals(5 to ChordType.MAJOR7_SHARP9, parseChordName("Fmaj7#9"))
        assertEquals(1 to ChordType.MINOR, parseChordName("C#m"))
    }

    @Test
    fun `name C major open voicing`() {
        val voicing = ChordVoicing(listOf(
            ChordNote.Open,      // E
            ChordNote.Fretted(1), // C
            ChordNote.Open,      // G
            ChordNote.Fretted(2), // E
            ChordNote.Fretted(3), // C
            ChordNote.Muted
        ))
        val names = nameChord(voicing)
        assertTrue(names.contains("C"))
    }

    @Test
    fun `name Am open voicing`() {
        val voicing = ChordVoicing(listOf(
            ChordNote.Open,      // E
            ChordNote.Fretted(1), // C
            ChordNote.Fretted(2), // A
            ChordNote.Fretted(2), // E
            ChordNote.Open,      // A
            ChordNote.Muted
        ))
        val names = nameChord(voicing)
        assertTrue(names.contains("Am"))
    }

    @Test
    fun `name G7 voicing`() {
        val voicing = ChordVoicing(listOf(
            ChordNote.Fretted(3), // G
            ChordNote.Fretted(0), // B
            ChordNote.Open,       // D
            ChordNote.Open,       // G
            ChordNote.Fretted(2), // B
            ChordNote.Fretted(3)  // G
        ))
        val names = nameChord(voicing)
        assertTrue(names.contains("G"))
    }

    @Test
    fun `name Cmaj7 voicing`() {
        val voicing = ChordVoicing(listOf(
            ChordNote.Open,       // E
            ChordNote.Open,       // B
            ChordNote.Open,       // G
            ChordNote.Fretted(2), // E
            ChordNote.Fretted(3), // C
            ChordNote.Muted
        ))
        val names = nameChord(voicing)
        assertTrue("Expected Cmaj7 in $names", names.contains("Cmaj7"))
    }

    @Test
    fun `name Fmaj7 sharp 9`() {
        // Fmaj7#9 = F A C E G#
        // Use a generated voicing and verify naming
        val voicings = generateVoicings(5, ChordType.MAJOR7_SHARP9, maxFret = 12)
        assertTrue("Should generate at least one Fmaj7#9 voicing", voicings.isNotEmpty())

        val first = voicings.first()
        val names = nameChord(first)
        assertTrue("Should identify as Fmaj7#9: $names", names.contains("Fmaj7#9"))
    }

    @Test
    fun `single note returns note name`() {
        val voicing = ChordVoicing(listOf(
            ChordNote.Open,
            ChordNote.Muted,
            ChordNote.Muted,
            ChordNote.Muted,
            ChordNote.Muted,
            ChordNote.Muted
        ))
        assertEquals(listOf("E"), nameChord(voicing))
    }

    @Test
    fun `all muted returns empty`() {
        val voicing = ChordVoicing.muted()
        assertTrue(nameChord(voicing).isEmpty())
    }

    @Test
    fun `generated C major voicing contains required notes`() {
        val voicings = generateVoicings(0, ChordType.MAJOR, maxFret = 12)
        assertTrue(voicings.isNotEmpty())

        val required = setOf(0, 4, 7) // C E G
        voicings.forEach { voicing ->
            val actual = voicingToPitchClasses(voicing)
            assertTrue("Missing chord tones in $voicing", actual.containsAll(required))
        }
    }

    @Test
    fun `generated voicings have reasonable span`() {
        val voicings = generateVoicings(0, ChordType.MAJOR, maxFret = 12)
        voicings.forEach { voicing ->
            val frets = voicing.notes.mapNotNull { it.fretOrNull() }.filter { it > 0 }
            if (frets.size >= 2) {
                val span = frets.max() - frets.min()
                assertTrue("Span $span too large in $voicing", span <= 4)
            }
        }
    }

    @Test
    fun `common voicings includes standard C and G`() {
        assertTrue(commonVoicings("C").isNotEmpty())
        assertTrue(commonVoicings("G").isNotEmpty())
        assertTrue(commonVoicings("Am").isNotEmpty())
    }
}
