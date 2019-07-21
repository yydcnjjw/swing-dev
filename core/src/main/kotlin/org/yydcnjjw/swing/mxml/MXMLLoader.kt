package org.yydcnjjw.swing.mxml

import org.yydcnjjw.swing.BeanUtil
import java.awt.Container
import java.io.InputStream
import java.lang.ClassCastException
import java.lang.reflect.Method
import javax.swing.BoxLayout
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader
import kotlin.reflect.KClass

object MXMLLoader {

    private const val IMPORT_PROCESSING_INSTRUCTION = "import"

    // private val xmlSource: Map<String, Any> = mutableMapOf()
    private lateinit var xmlStreamReader: XMLStreamReader

    private var current: Element? = null

    private var parseFinished = false

    private val imports = mutableListOf<Import>()

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
        return current?.value as T?
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
            val packageImport = imports.firstOrNull { it.className == name }
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
                current?.setPropertyAttr(Attr(localName, value))
            } else {
                throw MXMLLoadException("property can not have prefix: $prefix")
            }
        }
    }

    private fun processEndElement() {
        current?.appendToParentElement()
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
        println(localName)
        return if (localName[i + 1].isLowerCase()) {
            PropertyElement(localName, current ?: throw MXMLLoadException("property must have a parent"))
        } else {
            InstanceElement(current, getType(localName))
        }
    }
}

class MXMLLoadException(override val message: String?) :
    Exception()

class Attr(
    val name: String,
    value: Any
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

abstract class Element(
    open val parent: Element?
) {
    abstract val value: Any?
    abstract fun setPropertyAttr(attr: Attr, isElem: Boolean = false)
    abstract fun appendToParentElement()
    open fun build(): Any {
        return value!!
    }
}

abstract class ClassElement(
    parent: Element?,
    val classType: Class<*>
) : Element(parent) {

    override fun setPropertyAttr(attr: Attr, isElem: Boolean) {
        if (BeanUtil.getSetterMethod(classType, attr.name).isNotEmpty()) {
            BeanUtil.invokeSetMethod(this.value!!, classType, attr.name, attr.values)
        }
    }

    fun getHandler(handlerClassType: Class<out Annotation>) =
        ClassManager.getMethodsAnnotatedWith(handlerClassType)

    fun getSubElemHandler(classType: Class<*>): Method? {
        return getHandler(SubElemHandler::class.java)
            .firstOrNull { method ->
                val subElemHandlerAnnotation = method.getAnnotation(SubElemHandler::class.java)
                ((subElemHandlerAnnotation.extend
                        && subElemHandlerAnnotation.classType.java.isAssignableFrom(classType))
                        || classType == subElemHandlerAnnotation.classType)

            }
    }

    fun getConstructorHandler(classType: Class<*>): Method? {
        return getHandler(ConstructorHandler::class.java)
            .firstOrNull { method ->
                method.getAnnotation(ConstructorHandler::class.java).classType.java == classType
            }
    }
}

open class InstanceElement(
    parent: Element?,
    classType: Class<*>
) : ClassElement(parent, classType) {

    companion object {
        private const val CONSTRUCTOR_ARG = "constructor-arg"
        private const val CONSTRUCTOR = "constructor"
    }

    // TODO support constructor function with params
    // NOTE: sub elem
    // constructor-arg must in front of the property elem
    override var value: Any? = null

    private val propertyAttrs = mutableListOf<Attr>()
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
        return value!!
    }

    override fun setPropertyAttr(attr: Attr, isElem: Boolean) {
        if (attr.name == CONSTRUCTOR_ARG) {
            constructorArgsAttrs.add(attr)
        } else {
            if (!isElem) {
                propertyAttrs.add(attr)
            } else {
                BeanUtil.invokeSetMethod(value ?: build(), classType, attr.name, attr.values)
            }

        }
    }

    override fun appendToParentElement() {
        if (parent == null) {
            return
        }

        when (parent) {
            is InstanceElement -> {
                val classType = (parent as InstanceElement).classType
                getSubElemHandler(classType)?.invoke(this, parent, this)
            }
            is PropertyElement -> {
                // TODO support multi sub instance elem
                (parent as PropertyElement).value = value ?: build()
            }
            else -> throw MXMLLoadException("Not support Elem class")
        }
    }
}

class PropertyElement(
    val name: String,
    override val parent: Element
) : ClassElement(
    parent,
    if (parent is ClassElement) {
        BeanUtil.getFieldType(parent.classType, name)
    } else {
        throw MXMLLoadException("Not support Elem class")
    }
) {
    override var value: Any = BeanUtil.invokeGetMethod(
        parent.value ?: parent.build(),
        (parent as ClassElement).classType, name
    ) ?: throw MXMLLoadException("null property")

    override fun appendToParentElement() {
        parent.setPropertyAttr(Attr(name, value), true)
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
    BeanUtil.invoke(parent.value ?: parent.build(),
        Container::class.java, "add",
        listOf(subElem.value ?: subElem.build()))
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ConstructorHandler(
    val classType: KClass<*>
)

@ConstructorHandler(BoxLayout::class)
fun BoxLayoutConstructorHandler(
    elem: ClassElement, args: List<Any>
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