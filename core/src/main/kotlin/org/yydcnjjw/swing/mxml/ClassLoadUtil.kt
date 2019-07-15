package org.yydcnjjw.swing.mxml

class Import(
    import: String
) {
    val packageName: String
    val className: String

    init {
        if (import.endsWith(".*")) {
            packageName = import.substring(0, import.length - 2)
            className = ".*"
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
        return className == ".*"
    }

    fun getClassLoadPath() = "$packageName.${className.replace('.', '$')}"

}

object ClassManager {
    private val classes: MutableMap<Import, Class<*>> = mutableMapOf()

    fun load(name: String): Class<*>? = load(Import(name))

    fun load(import: Import): Class<*>? {
        if (import.isPackage()) {
            return null
        }

        val type = try {
            javaClass
                .classLoader
                .loadClass(import.getClassLoadPath())
        } catch (e: ClassNotFoundException) {
            return null
        }

        classes[import] = type

        return type
    }

    fun getType(import: Import): Class<*>? = classes[import]
}