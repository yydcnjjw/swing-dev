package org.yydcnjjw.swing.utils

import org.junit.Test
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