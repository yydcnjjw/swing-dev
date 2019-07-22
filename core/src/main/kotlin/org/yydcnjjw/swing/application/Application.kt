package org.yydcnjjw.swing.application

import javax.swing.JLabel
import javax.swing.JPanel
import java.awt.Component

import org.yydcnjjw.swing.mxml.MXMLLoader
import org.yydcnjjw.swing.utils.ClassManager
import org.yydcnjjw.swing.utils.BeanUtil
import org.reflections.ReflectionUtils

class Application {
    private val windowConfigures = mutableMapOf<Class<*>, WindowConfigure>()

    fun start() {
        loadWindowConfigures()
        val window = getMainWindow() as TestWindow

        println(window.label)
        println(window.panel)
    }

    private fun loadWindowConfigures() {
        windowConfigures.putAll(
            ClassManager.getTypesAnnotatedWith(WindowConfigure::class.java).map { classType ->
            classType to classType.getAnnotation(WindowConfigure::class.java)
        })
        windowConfigures.forEach(::println)
    }

    private fun getMainWindow(): BaseWindow {
        val (windowClassType, windowConfigure) = windowConfigures.toList().firstOrNull {
            (_, conf) ->
            conf.isMain
        } ?: throw ApplicationException("no main window")

        return newWindowClass(windowClassType, windowConfigure)
    }

    private fun newWindowClass(windowClassType: Class<*>, windowConfigure: WindowConfigure) : BaseWindow {
        if (!BaseWindow::class.java.isAssignableFrom(windowClassType)) {
            throw ApplicationException("not window type $windowClassType")
        }

        val instance = BeanUtil.build(windowClassType)

        val loader = MXMLLoader()
        val windowInstance = loader.load(javaClass
            .getResourceAsStream(windowConfigure.xmlPath)!!) 
            ?: throw ApplicationException("mxml load failure")

        windowClassType.getField("innerWindowInstance").set(instance, windowInstance)

        windowClassType.fields.filter {
            it.isAnnotationPresent(Id::class.java)
        }.forEach {
            val id = it.getAnnotation(Id::class.java)
            it.set(instance, loader.idElems[id.value]?.value 
                ?: throw ApplicationException("mxml load failure: ${id.value} is not present"))
        }

        return instance as BaseWindow
    }

}

class ApplicationException(override val message: String?) :
    Exception()

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class WindowConfigure (
    val xmlPath: String,
    val isMain: Boolean = false
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Id(val value: String) {}

// TODO use annotation represent innerWindowInstance
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class InnerWindowInstance(val value: String) {}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Window() {}

@Window
abstract class BaseWindow {
    @
    lateinit var innerWindowInstance: Component

    fun <T> getWindowInstance() : T {
        @Suppress("UNCHECKED_CAST")
        return innerWindowInstance as T
    }

    fun show() {
        innerWindowInstance.show()    
    }

}

@WindowConfigure("/test.xml", true)
class TestWindow: BaseWindow() {

    @Id("label")
    lateinit var label: JLabel

    @Id("panel")
    lateinit var panel: JPanel

    val s = ""

    init {

    }
}
