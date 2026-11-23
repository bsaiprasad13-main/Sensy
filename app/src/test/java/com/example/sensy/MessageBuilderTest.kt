package com.example.sensy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageBuilderTest {

    private val status = "is playing cricket"
    private val start = System.currentTimeMillis()
    private val duration = 30

    @Test
    fun savedContact_getsPersonalMessageWithStatus() {
        val msg = MessageBuilder.messageFor(true, false, status, start, duration)
        assertTrue(msg!!.contains(status))
    }

    @Test
    fun savedContact_getsPersonalMessage_evenWhenSkipUnknownIsOn() {
        val msg = MessageBuilder.messageFor(true, true, status, start, duration)
        assertTrue(msg!!.contains(status))
    }

    @Test
    fun unknownNumber_getsGenericMessage() {
        val msg = MessageBuilder.messageFor(false, false, status, start, duration)
        assertEquals(MessageBuilder.GENERIC_MESSAGE, msg)
    }

    @Test
    fun unknownNumber_getsNothing_whenSkipUnknownIsOn() {
        assertNull(MessageBuilder.messageFor(false, true, status, start, duration))
    }

    @Test
    fun genericMessage_revealsNoPersonalDetails() {
        val generic = MessageBuilder.GENERIC_MESSAGE
        assertFalse(generic.contains(status))
        assertFalse(generic.contains(BuildConfig.USER_NAME))
        assertFalse(generic.contains("Sensy"))
        assertFalse(generic.contains("mins"))
    }

    @Test
    fun genericMessage_fitsInOneSms() {
        assertTrue(MessageBuilder.GENERIC_MESSAGE.length <= 160)
    }
}
