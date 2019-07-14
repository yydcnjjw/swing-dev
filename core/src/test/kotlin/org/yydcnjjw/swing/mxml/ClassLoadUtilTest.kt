package org.yydcnjjw.swing.mxml

import kotlin.test.Test
import kotlin.test.assertNotNull

class ClassLoadUtilTest {
    @Test
    fun classLoadTest() {
        assertNotNull(ClassManager.load(Import("javax.swing.JFrame")))
        assertNotNull(ClassManager.load("javax.swing.JLabel"))
    }

    @Test
    fun getTypeTest() {
        val import = Import("javax.swing.JButton")
        ClassManager.load(import)
        assertNotNull(ClassManager.getType(import))
    }
}