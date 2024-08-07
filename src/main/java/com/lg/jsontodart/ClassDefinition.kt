package com.lg.jsontodart

import com.intellij.openapi.application.ApplicationManager
import com.lg.jsontodart.utils.camelCase
import com.lg.jsontodart.utils.fixFieldName
import com.lg.jsontodart.utils.getTypeName
import com.lg.jsontodart.utils.isPrimitiveType
import com.lg.setting.Settings
import com.lg.utils.toUpperCaseFirstOne

class ClassDefinition(private val name: String, private val privateFields: Boolean = false) {
    val fields = mutableMapOf<String, TypeDefinition>()
    val dependencies: List<Dependency>
        get() {
            val dependenciesList = mutableListOf<Dependency>()
            val keys = fields.keys
            keys.forEach { k ->
                if (fields[k]!!.isPrimitive.not()) {
                    dependenciesList.add(Dependency(k, fields[k]!!))
                }
            }
            return dependenciesList
        }

    fun addField(key: String, typeDef: TypeDefinition) {
        fields[key] = typeDef
    }

    fun hasField(otherField: TypeDefinition): Boolean {
        return fields.keys.firstOrNull { k -> fields[k] == otherField } != null
    }

    override operator fun equals(other: Any?): Boolean {
        if (other is ClassDefinition) {
            if (name != other.name) {
                return false
            }
            return fields.keys.firstOrNull { k ->
                other.fields.keys.firstOrNull { ok ->
                    fields[k] == other.fields[ok]
                } == null
            } == null
        }
        return false
    }

    private fun addTypeDef(typeDef: TypeDefinition, sb: StringBuffer) {
        if (typeDef.name == "Null") {
            sb.append("dynamic")
        } else {
            sb.append(typeDef.name)
        }

        if (typeDef.subtype != null) {
            //如果是list,就把名字修改成单数
            sb.append("<${typeDef.subtype!!}>")
        }
    }

    //字段的集合
    private val fieldList: String
        get() {
            val settings = ApplicationManager.getApplication()
                .getService(Settings::class.java)// ServiceManager.getService(Settings::class.java)
            val isOpenNullSafety = settings.isOpenNullSafety == true
            val isOpenNullAble = settings.isOpenNullAble == true
            val prefix = if (isOpenNullSafety && !isOpenNullAble) "late " else ""
            val suffix = if (isOpenNullSafety && isOpenNullAble) "?" else ""
            return fields.keys.map { key ->
                val f = fields[key]
                val fieldName = fixFieldName(key, f, privateFields)
                val sb = StringBuffer()
                //如果驼峰命名后不一致,才这样
                if (fieldName != key) {
                    sb.append("  ")
                    sb.append("@JSONField(name: \"${key}\")\n")
                }
                sb.append("  ")
                sb.append(prefix)
                addTypeDef(f!!, sb)
                sb.append(suffix)
                sb.append(" $fieldName;")
                return@map sb.toString()
            }.joinToString("\n")
        }


    override fun toString(): String {
        return if (privateFields) {
//            "class $name {\n$_fieldList\n\n$_defaultPrivateConstructor\n\n$_gettersSetters\n\n$_jsonParseFunc\n\n$_jsonGenFunc\n}\n";
            ""
        } else {
            "class $name with JsonConvert<${name}> {\n$fieldList\n}\n"
        }
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + privateFields.hashCode()
        result = 31 * result + fields.hashCode()
        result = 31 * result + dependencies.hashCode()
        result = 31 * result + fieldList.hashCode()
        return result
    }
}


class Dependency(var name: String, var typeDef: TypeDefinition) {
    val className: String
        get() {
            return camelCase(name)
        }

    override fun toString(): String {
        return "name = $name ,typeDef = $typeDef"
    }
}

class TypeDefinition(var name: String, var subtype: String? = null) {


    val isPrimitive: Boolean = if (subtype == null) {
        isPrimitiveType(name)
    } else {
        isPrimitiveType("$name<${subtype!!.toUpperCaseFirstOne()}>")
    }
    private val isPrimitiveList: Boolean = isPrimitive && name == "List"

    companion object {
        fun fromDynamic(obj: Any?): TypeDefinition {
            val type = getTypeName(obj)
            if (type == "List") {
                val list = obj as List<*>
                val firstElementType = if (list.isNotEmpty()) {
                    getTypeName(list[0])
                } else {
                    // when array is empty insert Null just to warn the user
                    "dynamic"
                }
                return TypeDefinition(type, firstElementType)
            }
            return TypeDefinition(type)
        }
    }


    override operator fun equals(other: Any?): Boolean {
        if (other is TypeDefinition) {
            return (name == other.name) && (subtype == other.subtype)
        }
        return false
    }


    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (subtype?.hashCode() ?: 0)
        result = 31 * result + isPrimitive.hashCode()
        result = 31 * result + isPrimitiveList.hashCode()
        return result
    }

    override fun toString(): String {
        return "TypeDefinition(name='$name', subtype=$subtype, isPrimitive=$isPrimitive, isPrimitiveList=$isPrimitiveList)"
    }


}