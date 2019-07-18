package org.yydcnjjw.swing.mxml

import org.junit.Test
import org.yydcnjjw.swing.BeanUtil
import javax.swing.JLabel
import kotlin.test.assertEquals

class BeanTest {
    @Test
    fun methodInvokeTest() {
        val label = JLabel()
        BeanUtil.invokeSetMethod(label, label::class.java, "text", listOf("Hello World"))
        assertEquals(label.text, "Hello World")
    }
}