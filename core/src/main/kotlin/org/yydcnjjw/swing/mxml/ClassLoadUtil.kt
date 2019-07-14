package org.yydcnjjw.swing.mxml

class Import(
    import: String
) {
    val className: String
    val packageName: String

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

    fun isPackage(): Boolean {
        return className == ".*"
    }

    fun getClassLoadPath() = "$packageName.${className.replace('.', '$')}"

}

object ClassManager {
    private val packages: MutableList<Import> = mutableListOf()
    private val classes: MutableMap<Import, Class<*>> = mutableMapOf()

    fun load(name: String): Class<*>? = load(Import(name))

    fun load(import: Import): Class<*>? {
        if (import.isPackage()) {
            packages.add(import)
            return null
        }

        val type = javaClass
            .classLoader
            .loadClass(import.getClassLoadPath())

        classes[import] = type

        return type
    }

    fun getType(import: Import): Class<*>? = classes[import]
}