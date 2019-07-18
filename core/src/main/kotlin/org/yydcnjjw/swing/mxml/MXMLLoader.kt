package org.yydcnjjw.swing.mxml

import org.yydcnjjw.swing.BeanUtil
import java.io.InputStream
import java.lang.Exception
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader

import java.awt.Container

object MXMLLoader {

    private const val IMPORT_PROCESSING_INSTRUCTION = "import"

    // private val xmlSource: Map<String, Any> = mutableMapOf()
    private lateinit var xmlStreamReader: XMLStreamReader

    private var current: Element? = null

    private var parseFinished = false

    private val imports = mutableListOf<Import>()

    init {

    }

    fun <T> load(inputStream: InputStream): T? {
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

        @Suppress("UNCHECKED_CAST")
        return current?.build() as T?
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
            val packageImport = imports.firstOrNull {it.className == name}
            var type: Class<*>? = if (packageImport != null) {
                ClassManager.getType(packageImport)
            } else null

            if (type == null) {
                for (import in imports.filter { it.isPackage() }) {
                    type = ClassManager.load(Import(import.packageName, name))
                    if (type != null) break
                }
            }
            type
        } ?: throw MXMLLoadException("not exist class")

    private fun processStartElement() {
        current = newElement()

        for (i in 0 until xmlStreamReader.attributeCount) {
            val prefix = xmlStreamReader.getAttributePrefix(i)
            val localName = xmlStreamReader.getAttributeLocalName(i)
            val value = xmlStreamReader.getAttributeValue(i)

            if (prefix.isEmpty()) {
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

        return if (localName[i + 1].isLowerCase()) {
            // property element process
            PropertyElement(localName, current!!)
        } else {
            val type = getType(localName)
            InstanceElement(current, type)
        }
    }
}

class MXMLLoadException(override val message: String?) :
    Exception()

private class Attr(
    val name: String?,
    value: Any,
    val sourceType: Class<*>?
) {
    // TODO: class new instance
    val values: List<Any> =
        if (value is String && Regex("#\\(.*\\)").matches(value)) {
            // multi params
            // format #(p1,p2,p3,...)
            // param support number, string
            (Regex("#\\((.*)\\)")
                .find(value)
                ?.groupValues
                ?.firstOrNull { it != value } ?: throw MXMLLoadException("no param"))
                .split(',')
        } else {
            listOf(value)
        }

}

private abstract class Element(
    open val parent: Element?
) {

    open var value: Any? = null

    abstract fun addAttr(name: String, value: Any)

    abstract fun addInstanceAttr(sourceType: Class<*>, value: Any)

    abstract fun build(): Any?

    abstract fun add(elem: Element)
}

private class InstanceElement(
    parent: Element?,
    private val type: Class<*>
) : Element(parent) {

    companion object {
        // TODO: scan annotation

    }

    private val propertyAttrs = mutableListOf<Attr>()
    private val instanceAttrs = mutableListOf<Attr>()

    override fun addAttr(name: String, value: Any) {
        propertyAttrs.add(Attr(name, value, null))
    }

    override fun addInstanceAttr(sourceType: Class<*>, value: Any) {
        instanceAttrs.add(Attr(null, value, sourceType))
    }

    override fun build(): Any? {
        if (value == null) {
            value = type.getDeclaredConstructor().newInstance() ?: throw MXMLLoadException("instance failure")

            propertyAttrs.forEach { attr ->
                BeanUtil.invokeSetMethod(
                    value!!,
                    type,
                    attr.name!!,
                    attr.values)
            }

            instanceAttrs.forEach { attr ->
                BeanUtil.invoke(value!!, type, "add", attr.values)

            }
        }
        return value
    }

    override fun add(elem: Element) {
        val attrValue = elem.build()
        if (elem is PropertyElement) {
            if (attrValue != null) {
                addAttr(elem.name, attrValue)
            } else {
                // log warn null property
            }
        } else if (elem is InstanceElement) {
            if (attrValue != null) {
                addInstanceAttr(attrValue::class.java, attrValue)
            }
        }
    }
}

private class PropertyElement(
    val name: String,
    override val parent: Element
) : Element(parent) {

    override fun addAttr(name: String, value: Any) {
        // nop
    }

    override fun addInstanceAttr(sourceType: Class<*>, value: Any) {
        // nop
    }


    override fun build(): Any? {
        return value
    }

    override fun add(elem: Element) {
        value = elem.build()
    }
}

interface ElementHandler {
    fun subElementHandler()
}