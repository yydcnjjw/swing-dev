package org.yydcnjjw.swing.application

import org.yydcnjjw.swing.mxml.MXMLLoader
import org.yydcnjjw.swing.utils.BeanUtil
import org.yydcnjjw.swing.utils.ClassManager
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel

class Application {
    private val windowConfigures = mutableMapOf<Class<*>, WindowConfigure>()

    fun start() {
        loadWindowConfigures()
        getMainWindow().show()
    }

    private fun loadWindowConfigures() {
        windowConfigures.putAll(
            ClassManager.getTypesAnnotatedWith(WindowConfigure::class.java).map { classType ->
                classType to classType.getAnnotation(WindowConfigure::class.java)
            })
    }

    private fun getMainWindow(): BaseWindow {
        val (windowClassType, _) = windowConfigures.toList().firstOrNull { (_, conf) ->
            conf.isMain
        } ?: throw ApplicationException("no main window")

        return newWindowClass(windowClassType)
    }

    private fun newWindowClass(windowClassType: Class<*>): BaseWindow {
        if (!BaseWindow::class.java.isAssignableFrom(windowClassType)) {
            throw ApplicationException("not window type $windowClassType")
        }

        return BeanUtil.build(windowClassType) as BaseWindow
    }

}

class ApplicationException(override val message: String?) :
    Exception()

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class WindowConfigure(
    val xmlPath: String,
    val isMain: Boolean = false
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Id(val value: String)

// TODO use annotation represent innerWindowInstance
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class InnerWindowInstance(val value: String)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Window

@Window
abstract class BaseWindow {

    private val innerWindowInstance: Component

    init {
        val windowConfigure = javaClass.getAnnotation(WindowConfigure::class.java)
            ?: throw ApplicationException("please set Window configure")

        val loader = MXMLLoader()
        innerWindowInstance = (loader.load(
            javaClass
                .getResourceAsStream(windowConfigure.xmlPath)!!
        )
            ?: throw ApplicationException("mxml load failure")) as Component

        javaClass.fields.filter {
            it.isAnnotationPresent(Id::class.java)
        }.forEach {
            val id = it.getAnnotation(Id::class.java)
            it.set(
                this, loader.idElems[id.value]?.value
                    ?: throw ApplicationException("mxml load failure: ${id.value} is not present")
            )
        }
    }

    fun <T> getWindowInstance(): T {
        @Suppress("UNCHECKED_CAST")
        return innerWindowInstance as T
    }

    fun show() {
        innerWindowInstance.isVisible = true
    }

}

@WindowConfigure("/test.xml", true)
class TestWindow : BaseWindow() {

    @Id("label")
    lateinit var label: JLabel

    @Id("panel")
    lateinit var panel: JPanel

    val frame = getWindowInstance<JFrame>()
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
