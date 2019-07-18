package org.yydcnjjw.swing

import java.lang.reflect.Method
import java.math.BigDecimal
import java.math.BigInteger

object BeanUtil {

    private const val GET_PREFIX = "get"
    private const val SET_PREFIX = "set"

    fun build(classType: Class<*>): Any {
        return classType.getDeclaredConstructor().newInstance()
    }

    private fun getGetterMethod(classType: Class<*>, key: String): List<Method> {
        return getMethod(classType, "$GET_PREFIX${key.capitalize()}")
    }

    private fun getSetterMethod(classType: Class<*>, key: String): List<Method> {
        return getMethod(classType, "$SET_PREFIX${key.capitalize()}")
    }

    private fun getMethod(classType: Class<*>, methodName: String): List<Method> {
        val methods = classType.methods.filter { it.name == methodName }

        if (methods.isEmpty()) {
            throw UnsupportedOperationException("$classType have not the method: $methodName")
        }
        return methods
    }

    private fun getDeclaredMethod(classType: Class<*>, key: String): List<Method> {
        return classType.declaredMethods.filter { it.name == key }
    }

    fun invokeGetMethod(obj: Any, classType: Class<*>, key: String): Any {
        return invoke(obj, classType, "$GET_PREFIX${key.capitalize()}", listOf())
    }

    fun invokeSetMethod(obj: Any, classType: Class<*>, key: String, params: List<Any>): Any {
        return invoke(obj, classType, "$SET_PREFIX${key.capitalize()}", params)
    }

    private fun filterMethodWithParam(methods: List<Method>, params: List<Any>): Method? {
        return methods
            .filter { it.parameterCount == params.size }
            .firstOrNull { method ->
                for ((i, param) in params.withIndex()) {
                    val coerceValue = coerce(param, method.parameterTypes[i])

                    val coerceValueClass =
                    if (method.parameterTypes[i].isPrimitive) {
                        coerceValue::class.javaPrimitiveType
                    } else {
                        coerceValue::class.java
                    }

                    if (method.parameterTypes[i] != coerceValueClass && !method.parameterTypes[i].isAssignableFrom(coerceValueClass!!)) {
                        println("${method.parameterTypes[i]}, $coerceValueClass")
                        return@firstOrNull false
                    }
                }
                true
            }
    }

    fun invoke(obj: Any, classType: Class<*>, methodName: String, params: List<Any>): Any =
        getMethod(classType, methodName)
            .also { methods ->
                (filterMethodWithParam(methods, params)
                    ?: throw UnsupportedOperationException("$classType have not the method: $methodName"))
                    .also { method ->
                        method.invoke(obj, *params.mapIndexed { i, param ->
                            coerce(param, method.parameterTypes[i])
                        }.toTypedArray())
                    }
            }

    fun coerce(value: Any, classType: Class<*>): Any {
        return if (classType.isAssignableFrom(value::class.java)) value
        else {
            when (classType) {
                Boolean::class.java -> value.toString().toBoolean()
                Char::class.java -> value.toString().toCharArray()[0]
                Byte::class.java -> value.toString().toByte()
                Short::class.java -> value.toString().toShort()
                Int::class.java -> value.toString().toInt()
                Long::class.java -> value.toString().toLong()
                BigInteger::class.java -> value.toString().toBigInteger()
                Float::class.java -> value.toString().toFloat()
                Double::class.java -> value.toString().toDouble()
                BigDecimal::class.java -> value.toString().toBigDecimal()
                // TODO: value to classType
                else -> {
                    var valueClassType: Class<*>? = value::class.java
                    val valueOfMethods = mutableListOf<Method>()
                    while (valueClassType != null && valueOfMethods.isEmpty()) {
                        valueOfMethods.clear()
                        valueOfMethods.addAll(getDeclaredMethod(valueClassType, "valueOf"))

                        if (valueOfMethods.isEmpty()) {
                            valueClassType = valueClassType.superclass
                        }
                    }

                    if (valueOfMethods.isEmpty()) {
                        throw IllegalArgumentException("Unable to coerce $value to $classType")
                    }

                    try {
                        (filterMethodWithParam(valueOfMethods, listOf(value))
                            ?: throw UnsupportedOperationException("$classType have not the method: valueOf"))
                            .invoke(value, listOf(value))
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }

                }
            }

        }
    }


}