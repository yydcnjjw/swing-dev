package org.yydcnjjw.swing.mxml

import java.awt.Dimension
import java.lang.Exception
import kotlin.test.Test
import kotlin.test.assertNotNull
import javax.swing.JLabel
class MXMLLoaderTest {
   @Test
   fun loadTest() {
       val obj: Any? = MXMLLoader.load(javaClass.getResourceAsStream("test.xml")!!)
       println(obj)
       val label = JLabel()
       label.setSize(Dimension())
       assertNotNull(obj)
    }


    @Test
    fun regexTest() {
        println(Regex("#\\((.*)\\)")
            .find("#(1,2,3)"))
    }
}