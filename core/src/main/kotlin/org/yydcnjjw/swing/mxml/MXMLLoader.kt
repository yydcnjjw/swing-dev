package org.yydcnjjw.swing.mxml

import java.io.InputStream
import java.lang.Exception
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader

object MXMLLoader {

    private const val IMPORT_PROCESSING_INSTRUCTION = "import"

    private val xmlSource: Map<String, Any> = mutableMapOf()
    private lateinit var xmlStreamReader: XMLStreamReader

    private var root: Any? = null
    private var current: Element? = null
        get() {
            return field ?: throw MXMLLoadException("xml is not loaded!")
        }

    private val imports = mutableListOf<Import>()

    fun <T> load(inputStream: InputStream) {
        xmlStreamReader = XMLInputFactory
            .newInstance()
            .createXMLStreamReader(inputStream)

        while (xmlStreamReader.hasNext()) {
            when (xmlStreamReader.next()) {
                XMLStreamConstants.PROCESSING_INSTRUCTION -> processProcessingInstruction()
                XMLStreamConstants.START_ELEMENT -> processStartElement()
                XMLStreamConstants.END_ELEMENT -> processEndElement()

            }
        }
    }

    private fun processProcessingInstruction() {
        val piTarget = xmlStreamReader.piTarget.trim()

        if (piTarget == IMPORT_PROCESSING_INSTRUCTION) {
            processImport()
        }
    }

    private fun processImport() {
        val import = Import(xmlStreamReader.piData.trim())

        ClassManager.load(import)

        imports.add(import)
    }

    private fun getType(name: String): Class<*>? =
        ClassManager.getType(imports.first { it.className == name })


    private fun processStartElement() {
        if (current == null && root != null) {
            throw MXMLLoadException("multi root!")
        }

        current?.handleStartElement()
    }

    private fun processEndElement() {

    }

    private fun newElement() {
        val localName = xmlStreamReader.localName
        // current = Element.newElement(localName)
    }
}

class MXMLLoadException(override val message: String?) :
    Exception()

private data class Attr(
    val name: String,
    val value: String,
    val sourceType: Class<*>
)

private open class Element(
    open val parent: Element?
) {

    private val propertyAttrs = listOf<Attr>()

    companion object {
        fun newElement(name: String, parent: Element?): Element {
//            val i = name.lastIndexOf('.')
//
//            if (name[i + 1].isLowerCase()) {
//                // property element process
//                return PropertyElement(parent!!)
//            } else {
//
//            }
            return Element(null)
        }
    }


    fun handleStartElement() {

    }

}

private class PropertyElement(
    override val parent: Element
) : Element(parent) {


}