package org.yydcnjjw.swing.utils

import kotlin.test.Test
import kotlin.test.assertNotNull

class B {
    companion object {
        @JvmField
        val b = 0
    }
}
class A {
    companion object {
        @JvmField
        val b = B()
    }
}

class ClassLoadUtilTest {
    @Test
    fun classLoadTest() {
        assertNotNull(ClassManager.load(Import("javax.swing.JFrame")))
        assertNotNull(ClassManager.load("javax.swing.JLabel"))
        assertNotNull(ClassManager.load("org.yydcnjjw.swing.utils.A.b"))
        assertNotNull(ClassManager.load("org.yydcnjjw.swing.utils.A.b.b"))
        assertNotNull(ClassManager.load("javax.swing.BoxLayout.X_AXIS"))
        assertNotNull(ClassManager.load("javax.swing.Box.Filler"))
    }
}