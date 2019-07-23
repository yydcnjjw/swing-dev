package org.yydcnjjw.swing.tool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.yydcnjjw.swing.application.Application
import org.yydcnjjw.swing.application.BaseWindow
import org.yydcnjjw.swing.application.WindowConfigure
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.io.File
import java.lang.Exception
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds
import javax.swing.JFrame


@WindowConfigure(true)
class XMLToolWindow : BaseWindow() {
    var mouseAtX = 0
    var mouseAtY = 0

    var frame = getWindowInstance<JFrame>()

    private fun commandParse() : ToolCommand {
        val args = getApplication().commandArgs
        val toolCommand = ToolCommand()
        toolCommand.parse(args)
        return toolCommand
    }

    private fun fileWatch() = GlobalScope.launch(Dispatchers.IO) {
        val file = File(javaClass.getResource(commandParse().path).path)
        val path = file
            .parentFile
            .toPath()
        println(file)
        println(path)

        val watcher = FileSystems.getDefault().newWatchService()
        path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY)
        while (true) {
            val watchKey = watcher.take()
            watchKey.pollEvents().forEach { event ->
                if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    resetView()
                }
            }

            if (!watchKey.reset()) {
                watchKey.cancel()
                watcher.close()
                break
            }
        }
    }

    fun init() {
        fileWatch()
    }

    fun resetView() {
        try {
            setView(commandParse().path)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        frame?.dispose()
        frame = getWindowInstance()

        frame?.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                mouseAtX = e.x
                mouseAtY = e.y
            }
        })

        frame?.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                frame?.setLocation(e.xOnScreen - mouseAtX, e.yOnScreen - mouseAtY)
            }
        })

        frame?.isVisible = true
    }

    override fun show() {
        init()
        resetView()
    }
}

class ToolCommand : CliktCommand() {
    val path by argument(help="xml file path")
    override fun run() = Unit
}


fun main(args: Array<String>) {
    Application(arrayOf("/xml_tool.xml")).start()
}