package org.yydcnjjw.swing.mxml

import org.yydcnjjw.swing.utils.BeanUtil
import org.yydcnjjw.swing.utils.ClassManager
import org.yydcnjjw.swing.utils.Import
import org.yydcnjjw.swing.utils.getPath

import java.awt.Container
import java.io.File
import java.io.InputStream
import java.lang.reflect.Method
import javax.swing.BoxLayout
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader
import kotlin.reflect.KClass

private const val IMPORT_PROCESSING_INSTRUCTION = "import"
private const val INCLUDE_PROCESSING_INSTRUCTION = "include"

private const val CONSTRUCTOR_ARG_TAG = "constructor-arg"

private const val INCLUDE_TAG = "include"
private const val INCLUDE_TAG_ATTR_PATH = "path"

private const val SLOT_TAG = "slot"
private const val SLOT_TAG_ATTR_NAME = "name"

private const val BLOCK_TAG = "block"
private const val BLOCK_TAG_ATTR_NAME = "name"

private const val PREFIX = "mx"

private const val PREFIX_ID = "$PREFIX:id"

// TODO: single object to constructor args
// TODO: log system
class MXMLLoader {
    private lateinit var xmlStreamReader: XMLStreamReader

    private var current: Element? = null

    private var parseFinished = false

    private val slots = mutableMapOf<String, List<Element>>()

    private val imports = mutableListOf<Import>(Import("java.lang.*"))
    

    private val idTemplates = mutableMapOf<String, Any>()

    val idElems = mutableMapOf<String, Element>()
    lateinit var parentDir: String

    fun load(file: File): Any? {
        parentDir = file.parent
        return load(file.inputStream())
    }

    private fun load(inputStream: InputStream): Any? {
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
        return current?.build()
    }

    fun load(inputStream: InputStream, slots: Map<String, List<Element>>, idTemplates: Map<String, Any>): Any? {
        this.slots.putAll(slots)
        this.idTemplates.putAll(idTemplates)
        return load(inputStream)
    }

    private fun processProcessingInstruction() {
        val piData = xmlStreamReader.piData
        when (xmlStreamReader.piTarget.trim()) {
            IMPORT_PROCESSING_INSTRUCTION -> processImport(piData)
            INCLUDE_PROCESSING_INSTRUCTION -> processInclude(piData)
        }
    }

    private fun processImport(piData: String) {
        val import = Import(piData)

        ClassManager.load(import)

        imports.add(import)
    }

    private fun processInclude(piData: String) {
        val loader = MXMLLoader()
        val include: Any? = loader.load(piData.getPath(parentDir).toFile())
        idElems.putAll(loader.idElems)

        if (include != null) {
            current = InstanceElement(current, include::class.java)
            current?.value = include
            current?.parent?.addSubElem(current!!)
        } else {
            throw MXMLLoadException("include error build failure")
        }
    }


    private fun getType(name: String): Class<*>? =
        if (name.isNotEmpty() && name[0].isLowerCase()) {
            // fully-qualified class name
            try {
                ClassManager.load(name)
            } catch (e: ClassNotFoundException) {
                null
            }
        } else {
            var type: Class<*>? =
                imports.firstOrNull { it.className == name }
                    .let {
                        if (it != null) ClassManager.getType(it)
                        else null
                    }

            if (type == null) {
                for (import in imports.filter { it.isPackage() }) {
                    val packageName = Import(import.packageName, name)
                    type = ClassManager.load(packageName)
                    if (type != null) {
                        imports.add(packageName)
                        break
                    }
                }
            }
            type
        }

    private fun getStaticValue(name: String): Any? {
        getType(name)
        return imports.firstOrNull { it.className == name }
            .let {
                if (it != null) ClassManager.getStaticValue(it)
                else null
            }
    }

    private fun processStartElement() {
        current = newElement()

        for (i in 0 until xmlStreamReader.attributeCount) {
            val prefix = xmlStreamReader.getAttributePrefix(i)
            val localName = xmlStreamReader.getAttributeLocalName(i)
            val value = xmlStreamReader.getAttributeValue(i)

            if (prefix.isEmpty()) {
                val values = splitFunctionParam(value).map {
                    getStaticValue(it.toString()) ?: it
                }

                val templateValue = getTemplateBlockValue(value)
                if (templateValue == null) {
                    current?.setPropertyAttr(Attr(localName, values))
                } else {
                    current?.setPropertyAttr(
                        Attr(
                            localName,
                            idTemplates[templateValue] ?: throw MXMLLoadException("template params is not passed")
                        )
                   )
                }
            } else {
                when ("$prefix:$localName") {
                    PREFIX_ID -> idElems[value] = current!!
                }
            }
        }
    }

    private fun processEndElement() {
        current?.parent?.addSubElem(current!!)
        val parent = current?.parent
        if (parent != null) {
            current = parent
        } else {
            parseFinished = true
        }
    }

    private fun newElement(): Element {
        val localName = xmlStreamReader.localName
        val prefix = xmlStreamReader.prefix

        return if (prefix == null || prefix.isEmpty()) {
            val i = localName.lastIndexOf('.')

            if (localName[i + 1].isLowerCase()) {
                if (current == null) {
                    throw MXMLLoadException("property must have a parent")                    
                }
                val classType = if (current is InstanceElement) {
                    try {
                        BeanUtil.getFieldType((current as InstanceElement).classType, localName)
                    } catch(e: NoSuchElementException) {
                        null
                    }
                } else null

                if (classType == null) {
                    PropertyElement(current!!, localName)
                } else {
                    ClassPropertyElement(current as InstanceElement, localName, classType)
                }
                
            } else {
                InstanceElement(current, getType(localName) ?: throw ClassNotFoundException(localName))
            }
        } else {
            if (prefix == PREFIX) {
                when (localName) {
                    INCLUDE_TAG -> IncludeElement(current, this)
                    SLOT_TAG -> SlotElement(current, slots)
                    BLOCK_TAG -> BlockElement(current ?: throw MXMLLoadException("block must have a parent"))
                    CONSTRUCTOR_ARG_TAG -> PropertyElement(current ?: throw MXMLLoadException("property must have a parent"), CONSTRUCTOR_ARG_TAG)
                    else -> throw MXMLLoadException("unknown tag $prefix:$localName")
                }
            } else {
                throw MXMLLoadException("unknown tag $prefix:$localName")
            }
        }
    }

    private fun getTemplateBlockValue(s: String) =
        Regex("\\$\\{(.*)}").find(s)
            ?.groupValues?.firstOrNull { it != s }

}

class MXMLLoadException(override val message: String?) :
    Exception()

fun splitFunctionParam(value: Any): List<Any> = if (value is String && Regex("#\\(.*\\)").matches(value)) {
    // multi params
    // format #(p1,p2,p3,...)
    // param support number, string
    (Regex("#\\((.*)\\)")
        .find(value)
        ?.groupValues
        ?.firstOrNull { it != value } ?: throw MXMLLoadException("no param"))
        .split(',')
        .map { it.trim() }
} else {
    listOf(value)
}

data class Attr(
    val name: String,
    val values: List<Any>
) {
    constructor(name: String, value: Any) : this(name, splitFunctionParam(value)) {}
}

abstract class Element(
    open val parent: Element?
) {
    open val propertyAttrs = mutableListOf<Attr>()
    abstract var value: Any?
    open fun setPropertyAttr(attr: Attr, isElem: Boolean = false) {
        propertyAttrs.add(attr)
    }

    abstract fun addSubElem(elem: Element)
    abstract fun build(): Any

    protected fun getAttrValue(name: String): String? =
        propertyAttrs
            .firstOrNull { it.name == name }
            ?.values?.first() as String?
}

abstract class ValueElement(
    parent: Element?
) : Element(parent) {

    override var value: Any? = null

    override fun build(): Any = value!!

    protected fun getHandler(handlerClassType: Class<out Annotation>) =
        ClassManager.getMethodsAnnotatedWith(handlerClassType)

    protected fun getSubElemHandler(classType: Class<*>): Method? {
        return getHandler(SubElemHandler::class.java)
            .firstOrNull { method ->
                val subElemHandlerAnnotation = method.getAnnotation(SubElemHandler::class.java)
                ((subElemHandlerAnnotation.extend
                        && subElemHandlerAnnotation.classType.java.isAssignableFrom(classType))
                        || classType == subElemHandlerAnnotation.classType)

            }
    }
}

class InstanceElement(
    parent: Element?,
    val classType: Class<*>
) : ValueElement(parent) {

    companion object {
        private const val CONSTRUCTOR_ARG = "constructor-arg"
    }

    // NOTE: sub elem
    // constructor-arg must in front of the property elem
    override var value: Any? = null

    private val constructorArgsAttrs = mutableListOf<Attr>()

    override fun build(): Any {
        if (value == null) {
            val handler = getConstructorHandler(classType)
            val args = constructorArgsAttrs.flatMap { it.values }

            value = if (handler != null) {
                handler(this, this, args)
            } else {
                BeanUtil.build(classType, args)
            }

            propertyAttrs.forEach {
                BeanUtil.invokeSetMethod(value!!, classType, it.name, it.values)
            }
        }
        return value ?: throw MXMLLoadException("instance build failure $classType")
    }

    override fun setPropertyAttr(attr: Attr, isElem: Boolean) {
        if (attr.name == CONSTRUCTOR_ARG) {
            constructorArgsAttrs.add(attr)
        } else {
            if (!isElem) {
                propertyAttrs.add(attr)
            } else {
                BeanUtil.invokeSetMethod(build(), classType, attr.name, attr.values)
            }
        }
    }

    override fun addSubElem(elem: Element) {
        val handler = getSubElemHandler(classType)
        when (elem) {
            is PropertyElement -> setPropertyAttr(Attr(elem.name, elem.build()), true)
            is SlotElement -> elem.slot?.forEach { handler?.invoke(this, this, it) }
            else -> handler?.invoke(this, this, elem)
        }
    }      

    private fun getConstructorHandler(classType: Class<*>): Method? {
        return getHandler(ConstructorHandler::class.java)
            .firstOrNull { method ->
                method.getAnnotation(ConstructorHandler::class.java).classType.java == classType
            }
    }

}

open class PropertyElement(
    override val parent: Element,
    val name: String
) : ValueElement(parent) {

    override fun addSubElem(elem: Element) {
        value = elem.build()
    }

    override fun build(): Any {
        return value ?: getAttrValue("value") ?: {
            throw MXMLLoadException("prperty element value is null")
        }
    }
}

class ClassPropertyElement(
    parent: InstanceElement,
    name: String,
    val classType: Class<*>
) : PropertyElement(parent, name) {
    override var value: Any? = BeanUtil.invokeGetMethod(
        parent.build(),
        parent.classType, name
    )

    override fun setPropertyAttr(attr: Attr, isElem: Boolean) {
        if (value != null) {
            if (BeanUtil.getSetterMethod(classType, attr.name).isNotEmpty()) {
                BeanUtil.invokeSetMethod(value!!, classType, attr.name, attr.values)
            }
        } else {
            super.setPropertyAttr(attr, isElem)
        }
    }

    override fun addSubElem(elem: Element) {
        val handler = getSubElemHandler(classType)

        when (elem) {
            is PropertyElement -> setPropertyAttr(Attr(elem.name, elem.build()), true)
            is SlotElement -> elem.slot?.forEach { handler?.invoke(this, this, it) }
            else -> if (handler == null) {
                value = elem.build()
            } else {
                handler.invoke(this, this, elem)
            }
        }
    }

    override fun build() : Any {
        if (value == null) {
            throw MXMLLoadException("class property must have a sub elem")
        }
        propertyAttrs.forEach { attr ->
            setPropertyAttr(attr, true)
        }
        propertyAttrs.clear()
        return value!!
    }
}

class IncludeElement(
    parent: Element?,
    val loader: MXMLLoader
) : ValueElement(parent) {
    override var value: Any? = null
    private val path: String
        get() = getAttrValue(INCLUDE_TAG_ATTR_PATH)
            ?: throw MXMLLoadException("IncludeElement must have a path attr")

    private val blocks = mutableListOf<BlockElement>()

    override fun addSubElem(elem: Element) {
        when (elem) {
            is BlockElement -> blocks.add(elem)
            is PropertyElement -> setPropertyAttr(Attr(elem.name, elem.build()))
            else -> throw MXMLLoadException("IncludeElement must only include BlockElement")
        }
    }

    override fun build(): Any {
        val parentDir = this.loader.parentDir
        val loader = MXMLLoader()
        return (loader.load(
            path.getPath(parentDir).toFile().inputStream(),
            blocks.map { it.name to it.subElems.toList() }.toMap(),
            propertyAttrs.filter { it.name != INCLUDE_TAG_ATTR_PATH }
                .map { it.name to it.values.first() }.toMap()
        ) ?: throw MXMLLoadException("mxml include build failure include $path"))
            .also {
                this.loader.idElems.putAll(loader.idElems)
            }

    }
}

class BlockElement(
    override val parent: Element
) : ValueElement(parent) {
    override var value: Any? = null

    val name: String
        get() = getAttrValue(BLOCK_TAG_ATTR_NAME)
            ?: throw MXMLLoadException("BlockElement must have a name attr")

    val subElems = mutableListOf<Element>()

    init {
        if (parent !is IncludeElement) {
            throw MXMLLoadException("block's parent must be a IncludeElement")
        }
    }

    override fun addSubElem(elem: Element) {
        subElems.add(elem)
    }

    override fun build(): Any {
        throw MXMLLoadException("block element can not be build")
    }
}

class SlotElement(
    parent: Element?,
    private val slots: Map<String, List<Element>>
) : Element(parent) {
    override var value: Any? = null

    val name: String
        get() = getAttrValue(SLOT_TAG_ATTR_NAME) ?: throw MXMLLoadException("slot must have a name attr")

    val slot: List<Element>?
        get() = slots[name]

    override fun addSubElem(elem: Element) {}

    override fun build(): Any {
        throw MXMLLoadException("slot can not be build")
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SubElemHandler(
    val classType: KClass<*>,
    val extend: Boolean = false
)

@SubElemHandler(Container::class, true)
fun ContainerSubElemHandler(parent: Element, subElem: Element) {
    BeanUtil.invoke(
        parent.build(),
        Container::class.java, "add",
        listOf(subElem.build())
    )
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ConstructorHandler(
    val classType: KClass<*>
)

@ConstructorHandler(BoxLayout::class)
fun BoxLayoutConstructorHandler(
    elem: ValueElement, args: List<Any>
): Any {
    return BeanUtil.build(BoxLayout::class.java, mutableListOf<Any>().also {
        val target = elem.parent?.parent?.value!!
        if (target is Container) {
            it.add(target)
            it.addAll(args)
        } else {
            throw MXMLLoadException("box layout target must is a container, please set parent elem")
        }
    })
}