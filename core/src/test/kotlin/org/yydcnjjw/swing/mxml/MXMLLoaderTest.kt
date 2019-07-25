package org.yydcnjjw.swing.mxml

import java.io.File
import javax.swing.JFrame
import javax.swing.JRootPane
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MXMLLoaderTest {

    @Test
    fun loadTest() {
        val frame: JFrame? = MXMLLoader()
            .load(File("/home/yydcnjjw/workspace/code/project/hyron/swing-dev/core/src/test/resources/test.xml")) as JFrame?
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

    @Test
    fun checkTemplateBlockTest() {
        val s = "\${1111}"
        assertEquals(Regex("\\$\\{(.*)}").find(s)
            ?.groupValues?.firstOrNull { it != s }, "1111"
        )
    }
}