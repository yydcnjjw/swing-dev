package org.yydcnjjw.swing

import java.lang.reflect.Method
import java.math.BigDecimal
import java.math.BigInteger

import java.lang.NoSuchFieldException

object BeanUtil {

    private const val GET_PREFIX = "get"
    private const val SET_PREFIX = "set"

    fun build(classType: Class<*>, args: List<Any> = listOf()): Any {
        val argsList = mutableListOf<Any>()
        return (classType.constructors
            .firstOrNull { constructor ->
                if (constructor.parameterCount == args.size) {
                    constructor.parameterTypes.forEachIndexed { i, classType ->
                        try {
                            argsList.add(coerce(args[i], classType))
                        } catch (e: IllegalArgumentException) {
                            println("warn coerce failure")
                            return@firstOrNull false
                        }
                    }
                    return@firstOrNull true
                }
                return@firstOrNull false
            } ?: throw ClassNotFoundException("constructors is not exist"))
            .newInstance(*argsList.toTypedArray()) ?: throw ClassNotFoundException("class build failure")
    }

    fun getFieldType(classType: Class<*>, key: String): Class<*> {
        return try {
            classType.getField(key).type
        } catch (e: NoSuchFieldException) {
            getGetterMethod(classType, key).first().returnType
        }
    }

    private fun getGetterMethod(classType: Class<*>, key: String): List<Method> {
        return getMethod(classType, "$GET_PREFIX${key.capitalize()}")
    }

    fun getSetterMethod(classType: Class<*>, key: String): List<Method> {
        return getMethod(classType, "$SET_PREFIX${key.capitalize()}")
    }

    private fun getMethod(classType: Class<*>, methodName: String): List<Method> {
        return classType.methods.filter { it.name == methodName }
    }

    private fun getDeclaredMethod(classType: Class<*>, key: String): List<Method> {
        return classType.declaredMethods.filter { it.name == key }
    }

    fun invokeGetMethod(obj: Any, classType: Class<*>, key: String): Any? {
        return invoke(obj, classType, "$GET_PREFIX${key.capitalize()}", listOf())
    }

    fun invokeSetMethod(obj: Any, classType: Class<*>, key: String, params: List<Any>) {
        invoke(obj, classType, "$SET_PREFIX${key.capitalize()}", params)
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

                    if (method.parameterTypes[i] != coerceValueClass
                        && !method.parameterTypes[i].isAssignableFrom(
                            coerceValueClass!!
                        )
                    ) {
                        return@firstOrNull false
                    }
                }
                true
            }
    }

    fun invoke(obj: Any, classType: Class<*>, methodName: String, params: List<Any>): Any? =
        getMethod(classType, methodName)
            .let { methods ->
                filterMethodWithParam(methods, params)
                    ?.let { method ->
                        method.invoke(
                            obj,
                            *params.mapIndexed { i, param ->
                                coerce(param, method.parameterTypes[i])
                            }.toTypedArray()
                        )
                    }
            }

    private fun coerce(value: Any, classType: Class<*>): Any {
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