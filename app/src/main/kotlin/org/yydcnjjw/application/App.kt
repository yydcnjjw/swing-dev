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
    }

    var mouseAtX = 0
    var mouseAtY = 0

    init {
        setSize(600, 400)

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
        val titlePanel = JPanel()
        titlePanel.layout = BoxLayout(titlePanel, BoxLayout.X_AXIS)
        titlePanel.add(with(JLabel("计算条件设定")) {
            this
        })
        titlePanel.add(Box.createHorizontalGlue())
        titlePanel.add(with(JButton("X")) {
            this
        })
        add(titlePanel)

        add(with(JPanel()) {
            val size = Dimension(600, 10)
            this.preferredSize = size
            this.maximumSize = size
            this.background = BTN_COLOR
            this
        })

        add(with(Box.createHorizontalBox()) {
            this.add(JLabel("契约情报"))
            this.add(Box.createHorizontalGlue())
            this
        })

        add(with(Box.createHorizontalBox()) {
            this.add(JLabel("计算基准日"))
            this.add(Box.createVerticalStrut(100))
            this.add(JLabel("(必须)"))
            this.add(JTextField())
            this.add(JLabel("年"))
            this.add(JTextField())
            this.add(JLabel("月"))
            this.add(JTextField())
            this.add(JLabel("日"))

            this
        })
        add(with(Box.createHorizontalBox()) {
            this.add(JLabel("计算基准日"))
            this.add(Box.createVerticalStrut(100))
            this.add(JLabel("(必须)"))
            this.add(JTextField())
            this.add(JLabel("年"))
            this.add(JTextField())
            this.add(JLabel("月"))
            this.add(JTextField())
            this.add(JLabel("日"))

            this
        })

    }

    fun start() {
        isVisible = true
    }
}