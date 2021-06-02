package chat.concept_repository_message

import chat.sphinx.concept_repository_message.SendMessage
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import org.junit.After
import org.junit.Assert
import org.junit.Test
import java.io.File

class SendMessageUnitTest {

    private val builder = SendMessage.Builder()
    private val assertFalse
        get() = Assert.assertFalse(builder.isValid)

    private val assertTrue
        get() = Assert.assertTrue(builder.isValid)

    @After
    fun tearDown() {
        builder.clear()
    }

    @Test
    fun `no contact or chat id fails`() {
        builder.setText("some text")
        assertFalse

        builder.setFile(File(System.getProperty("user.dir")))
        assertFalse
    }

    @Test
    fun `contact and or chat id succeeds`() {
        builder.setText("some text")

        // only contact succeeds
        builder.setContactId(ContactId(1))
        assertTrue

        // both contact & chat succeeds
        builder.setChatId(ChatId(1))
        assertTrue

        // only chat succeeds
        builder.setContactId(null)
        assertTrue

        builder.setChatId(null)
        assertFalse
    }

    @Test
    fun `file DNE fails`() {
        val path = System.getProperty("user.dir")
        // invalid file path
        builder.setFile(File(path.dropLast(3)))
        builder.setContactId(ContactId(1))
        assertFalse

        // fails even with text b/c file is invalid
        builder.setText("some text")
        assertFalse

        // file exists
        builder.setFile(File(path))
        assertTrue
    }

    @Test
    fun `build() returns null if invalid`() {
        builder.setText("some text")
        Assert.assertNull(builder.build())
        builder.setChatId(ChatId(1))
        Assert.assertNotNull(builder.build())
    }
}