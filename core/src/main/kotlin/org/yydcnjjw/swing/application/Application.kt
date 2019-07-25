package org.yydcnjjw.swing.application

import org.yydcnjjw.swing.mxml.MXMLLoader
import org.yydcnjjw.swing.utils.BeanUtil
import org.yydcnjjw.swing.utils.ClassManager
import org.yydcnjjw.swing.utils.getPath
import java.awt.Component
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths


class Application(
    val commandArgs: Array<String>,
    rootDir: String, // xml root dir
    var mainClassWindow: Class<out BaseWindow>? = null
) {
    private val windowConfigures = mutableMapOf<Class<out BaseWindow>, WindowConfigure>()

    val xmlRootDir = Paths.get(rootDir).also {
        if (!it.toFile().isDirectory) throw ApplicationException("xml root path must be a dir")
    }

    init {
        loadWindowConfigures()
        if (mainClassWindow == null) {
            val window = (windowConfigures.toList().firstOrNull { (_, conf) ->
                conf.isMain
            } ?: throw ApplicationException("no main window")).first

            if (!BaseWindow::class.java.isAssignableFrom(window)) {
                throw ApplicationException("not window type $window")
            }
            mainClassWindow = window
        }
    }

    fun start() {
        loadWindowConfigures()
        getMainWindow().show()
    }

    private fun loadWindowConfigures() {
        windowConfigures.putAll(
            ClassManager.getTypesAnnotatedWith(WindowConfigure::class.java).map { classType ->
                if (BaseWindow::class.java.isAssignableFrom(classType)) {
                    @Suppress("UNCHECKED_CAST")
                    classType as Class<out BaseWindow> to classType.getAnnotation(WindowConfigure::class.java)
                } else {
                    throw ApplicationException("WindowConfigure annotation must set at class that is extend BaseWindow")
                }
            })
    }

    private fun getMainWindow(): BaseWindow {
        return newWindowClass(mainClassWindow!!)
    }

    private fun newWindowClass(windowClassType: Class<out BaseWindow>): BaseWindow {
        val window = BeanUtil.build(windowClassType) as BaseWindow
        window.application = this
        return window
    }

}

class ApplicationException(override val message: String?) :
    Exception()

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class WindowConfigure(
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

    private var innerWindowInstance: Component? = null
    private var xmlPath: Path? = null

    internal lateinit var application: Application

    fun <T> getWindowInstance(): T? {
        @Suppress("UNCHECKED_CAST")
        return innerWindowInstance as T?
    }

    fun getApplication() = application

    fun setView(path: String) {
        xmlPath = path.getPath(application.xmlRootDir.toString())
        updateView()
    }

    fun updateView() {
        if (xmlPath == null) {
            return
        }

        val loader = MXMLLoader()
        innerWindowInstance = (loader.load(xmlPath?.toFile()!!) ?: throw ApplicationException("mxml load failure")) as Component

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

    open fun show() {
        innerWindowInstance?.isVisible = true
    }

}