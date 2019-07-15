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

    private var current: Element? = null

    private var parseFinished = false

    private val imports = mutableListOf<Import>()

    fun <T> load(inputStream: InputStream): T {
        xmlStreamReader = XMLInputFactory
            .newInstance()
            .createXMLStreamReader(inputStream)

        while (xmlStreamReader.hasNext() && !parseFinished) {
            when (xmlStreamReader.next()) {
                XMLStreamConstants.PROCESSING_INSTRUCTION -> processProcessingInstruction()
                XMLStreamConstants.START_ELEMENT -> processStartElement()
                XMLStreamConstants.END_ELEMENT -> processEndElement()
            }
        }

        return current?.build() as T
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

    private fun getType(name: String): Class<*> =
        if (name.isNotEmpty() && name[0].isLowerCase()) {
            // fully-qualified class name
            ClassManager.load(name)
        } else {
            var type = ClassManager.getType(imports.first { it.className == name })
            if (type == null) {
                for (import in imports.filter { it.isPackage() }) {
                    type = ClassManager.load(Import(import.packageName, name))
                    if (type != null) break
                }
            }
            type
        } ?: throw MXMLLoadException("is not exist class")

    private fun processStartElement() {
        // if (current == null && root != null) {
        //     throw MXMLLoadException("multi root!")
        // }

        current = newElement()

        for (i in 0..xmlStreamReader.attributeCount-1) {
            val prefix = xmlStreamReader.getAttributePrefix(i)
            val localName = xmlStreamReader.getAttributeLocalName(i)
            val value = xmlStreamReader.getAttributeValue(i)

            if (prefix.length == 0) {
                current?.addAttr(localName, value)
            } else {
                throw MXMLLoadException("property can not have prefix: $prefix")
            }
        }
    }

    private fun processEndElement() {
        current?.parent?.add(current!!)
        val parent = current?.parent
        if (parent != null) {
            current = parent
        } else {
            parseFinished = true
        }
    }

    private fun newElement(): Element {
        val localName = xmlStreamReader.localName
        val i = localName.lastIndexOf('.')

        if (localName[i + 1].isLowerCase()) {
            // property element process
            return PropertyElement(localName, current!!)
        } else {
             val type = getType(localName)
            return InstanceElement(current, type)
        }        
    }
}

class MXMLLoadException(override val message: String?) :
    Exception()

private data class Attr(
    val name: String,
    val value: Any,
    val sourceType: Class<*>?
) {
    
}

private abstract class Element(
    open val parent: Element?
) {

    open lateinit var value: Any

    abstract fun addAttr(name: String, value: Any)
    
    abstract fun build(): Any

    abstract fun add(elem: Element)
}

private class InstanceElement(
    parent: Element?,
    private val type: Class<*>
) : Element(parent) {
    private val propertyAttrs = mutableListOf<Attr>()

    override fun addAttr(name: String, value: Any) {
        propertyAttrs.add(Attr(name, value, null))
    }

    override fun build() : Any {
        value = type.newInstance()
        propertyAttrs.forEach { attr ->
            type.getMethods().first {
                method ->
                method.name == "set${attr.name.capitalize()}"
            }.invoke(value, attr.value)
        }
        return value
    }

    override fun add(elem: Element) {
        val property = elem as PropertyElement
        addAttr(property.name, property.build())
    }
}

private class PropertyElement(
    val name: String,
    override val parent: Element
) : Element(parent) {

    override fun addAttr(name: String, value: Any) {
        // nop
    }

    override fun build(): Any {
        return value
    } 

    override fun add(elem: Element) {
        value = elem.build()
    }
}