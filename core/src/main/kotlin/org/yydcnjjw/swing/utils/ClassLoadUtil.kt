package org.yydcnjjw.swing.utils

import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.scanners.SubTypesScanner
import java.lang.reflect.Method
import kotlin.reflect.typeOf

class Import(
    import: String
) {
    val packageName: String
    val className: String

    init {
        if (import.endsWith(".*")) {
            packageName = import.substring(0, import.length - 2)
            className = "*"
        } else {
            var i = import.indexOf('.')
            val len = import.length

            while (i != -1 && i < len && import[i + 1].isLowerCase()) {
                i = import.indexOf('.', i + 1)
            }

            if (i == -1 || i == len) {
                throw ClassNotFoundException();
            }

            packageName = import.substring(0, i)
            className = import.substring(i + 1)
        }
    }

    constructor(packageName: String, className: String) :
            this("$packageName.$className")

    fun isPackage(): Boolean {
        return className == "*"
    }

    fun getClassLoadPath() = "$packageName.${className.replace('.', '$')}"


    override fun toString(): String {
        return "$packageName.$className"
    }
}

object ClassManager {
    private val classes: MutableMap<Import, Class<*>> = mutableMapOf()
    private val staticValues: MutableMap<Import, Any> = mutableMapOf()
    private val reflection = Reflections(
        "org.yydcnjjw",
        MethodAnnotationsScanner(),
        TypeAnnotationsScanner(),
        SubTypesScanner()
    )
    fun load(name: String): Class<*>? = load(Import(name))

    fun load(import: Import): Class<*>? {
        if (import.isPackage()) {
            return null
        }

        var classLoadPath = import.getClassLoadPath()
        var classType: Class<*>?
        var i: Int
        while (true) {
            classType = try {
                javaClass
                    .classLoader
                    .loadClass(classLoadPath)
            } catch (e: ClassNotFoundException) {
                null
            }

            i = classLoadPath.lastIndexOf('$')

            if (i != -1 && classType == null)
                classLoadPath = classLoadPath.substring(0, i)
            else
                break
        }

        if (classType != null) {
            val classPath = import.getClassLoadPath()
            if (classPath.length != classLoadPath.length) {
                val fields = import.getClassLoadPath()
                    .substring(classLoadPath.length + 1)
                    .split('$')

                var type: Class<*> = classType
                var value: Any? = null
                fields.forEach { name ->
                    val field = type.getField(name)
                    value = field.get(type)
                    type = field.type
                }
                if (value != null) {
                    staticValues[import] = value!!
                }
                classType = type
            }
            classes[import] = classType
        }
        return classType
    }

    fun getType(import: Import): Class<*>? = classes[import]

    fun getStaticValue(import: Import): Any? = staticValues[import]

    fun getMethodsAnnotatedWith(classType: Class<out Annotation>): Set<Method> 
        = reflection.getMethodsAnnotatedWith(classType)
    
    fun getTypesAnnotatedWith(classType: Class<out Annotation>): Set<Class<*>>
        = reflection.getTypesAnnotatedWith(classType)
}