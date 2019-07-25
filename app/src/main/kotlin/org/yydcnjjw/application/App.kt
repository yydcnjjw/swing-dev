package org.yydcnjjw.application

import java.awt.Color
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.*

class App : JFrame() {

    companion object {
        val BACKGROUND_COLOR = Color(235, 235, 235)
        val BTN_COLOR = Color(238, 126, 33)
        val BTN2_COLOR = Color(0, 141, 171)

        const val WIDTH = 1024
        const val HEIGHT = 720
    }

    var mouseAtX = 0
    var mouseAtY = 0

    init {
        setSize(WIDTH, HEIGHT)

        // hide title bar
        isUndecorated = true

        rootPane.windowDecorationStyle = JRootPane.NONE

        // mouse dragged
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                mouseAtX = e.x
                mouseAtY = e.y
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                setLocation(e.xOnScreen - mouseAtX, e.yOnScreen - mouseAtY)
            }
        })

        background = BACKGROUND_COLOR
        layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)
        add(JLabel("aaa"))
        add(JLabel("bbb"))
        add(JLabel("ccc"))

//        val titlePanel = JPanel()
//        titlePanel.layout = BoxLayout(titlePanel, BoxLayout.X_AXIS)
//        titlePanel.add(JLabel("计算条件设定"))
//        titlePanel.add(Box.createHorizontalGlue())
//        titlePanel.add(JButton("X"))
//        add(titlePanel)
//
//        add(JPanel().also {
//            val size = Dimension(WIDTH, 10)
//            it.preferredSize = size
//            it.maximumSize = size
//            it.background = BTN_COLOR
//        })
//
//        add(JPanel().also {
//            val size = Dimension(WIDTH, 10)
//            it.preferredSize = size
//            it.maximumSize = size
//
//            it.add(JLabel("契约情报"))
//            it.add(Box.createHorizontalGlue())
//        })
//
//        add(JPanel().also {
//            val size = Dimension(WIDTH, 30)
//            it.preferredSize = size
//            it.maximumSize = size
//
//            it.add(JLabel("计算基准日"))
//            it.add(Box.createHorizontalStrut(100))
//            it.add(JLabel("(必须)"))
//            it.add(JTextField())
//            it.add(JLabel("年"))
//            it.add(JTextField())
//            it.add(JLabel("月"))
//            it.add(JTextField())
//            it.add(JLabel("日"))
//
//            it.add(JButton("测试"))
//        })
//
//        add(JPanel().also {
//            val size = Dimension(WIDTH, 30)
//            it.preferredSize = size
//            it.maximumSize = size
//
//            it.add(JLabel("计算基准日"))
//            it.add(Box.createHorizontalStrut(100))
//            it.add(JLabel("(必须)"))
//            it.add(JTextField())
//            it.add(JLabel("年"))
//            it.add(JTextField())
//            it.add(JLabel("月"))
//            it.add(JTextField())
//            it.add(JLabel("日"))
//
//            it.add(JButton("测试"))
//        })

    }

    fun start() {
        isVisible = true
    }
}