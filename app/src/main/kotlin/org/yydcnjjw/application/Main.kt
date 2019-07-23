package org.yydcnjjw.application

import org.yydcnjjw.swing.application.Application
import org.yydcnjjw.swing.application.BaseWindow
import org.yydcnjjw.swing.application.Id
import org.yydcnjjw.swing.application.WindowConfigure
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel

@WindowConfigure(true)
class TestWindow : BaseWindow("/test.xml") {

    @Id("label")
    lateinit var label: JLabel

    @Id("panel")
    lateinit var panel: JPanel

    val frame = getWindowInstance<JFrame>()!!
    var mouseAtX = 0
    var mouseAtY = 0

    init {
        // mouse dragged
        frame.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                mouseAtX = e.x
                mouseAtY = e.y
            }
        })

        frame.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                frame.setLocation(e.xOnScreen - mouseAtX, e.yOnScreen - mouseAtY)
            }
        })

        label.text = "asfddddddddddddddddddddd"
    }
}

fun main(args: Array<String>) {
    Application(args).start()
}