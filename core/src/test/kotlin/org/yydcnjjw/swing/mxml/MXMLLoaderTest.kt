package org.yydcnjjw.swing.mxml

import kotlin.test.Test
import kotlin.test.assertNotNull
import javax.swing.JLabel
class MXMLLoaderTest {
   @Test
   fun loadTest() {
       val obj: Any = MXMLLoader.load(javaClass.getResourceAsStream("test.xml")!!)
       println(obj)
       val label = JLabel()
       label.height
       assertNotNull(obj)
    }
}