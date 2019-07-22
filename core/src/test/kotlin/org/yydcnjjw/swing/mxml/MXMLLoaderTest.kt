package org.yydcnjjw.swing.mxml

import javax.swing.JFrame
import javax.swing.JRootPane
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MXMLLoaderTest {

    @Test
    fun loadTest() {
        val frame: JFrame? = MXMLLoader().load(javaClass.getResourceAsStream("test.xml")!!)
        assertNotNull(frame)
        println(frame.contentPane)
        println(frame.contentPane.background)
        println()
        frame.contentPane.components.forEach {
            println(it)
            println(it.background)
        }
        assertEquals(frame.rootPane.windowDecorationStyle, JRootPane.NONE)
    }

}