package com.lg.dart_to_helper.node

import com.lg.jsontodart.utils.getListSubType
import com.lg.jsontodart.utils.getListSubTypeCanNull
import com.lg.jsontodart.utils.isListType
import com.lg.jsontodart.utils.isPrimitiveType
import com.lg.utils.toLowerCaseFirstOne


/**
 * User: zhangruiyu
 * Date: 2019/12/23
 * Time: 11:32
 */
class HelperFileGeneratorInfo(
    val imports: MutableList<String> = mutableListOf(),
    val classes: MutableList<HelperClassGeneratorInfo> = mutableListOf()
)

class HelperClassGeneratorInfo {
    //协助的类名
    lateinit var className: String

    //属性列表
    private val fields: MutableList<Filed> = mutableListOf()


    fun addFiled(type: String, name: String, isLate: Boolean, annotationValue: List<AnnotationValue>?) {
        fields.add(Filed(type, name, isLate).apply {
            this.annotationValue = annotationValue
        })
    }


    override fun toString(): String {
        val sb = StringBuffer()
        sb.append(jsonParseFunc())
        sb.append("\n")
        sb.append("\n")
        sb.append(jsonGenFunc())
        return sb.toString()
    }

    //生成fromJson方法
    private fun jsonParseFunc(): String {
        val sb = StringBuffer()
        sb.append("\n")
        sb.append("${className.toLowerCaseFirstOne()}FromJson(${className} data, Map<String, dynamic> json) {\n")
        fields.forEach { k ->
            //如果deserialize不是false,那么就解析,否则不解析
            if (k.getValueByName<Boolean>("deserialize") != false) {
                sb.append("  ${jsonParseExpression(k)}\n")
            }
        }
        sb.append("  return data;\n")
        sb.append("}")
        return sb.toString()
    }

    //json解析单个属性
    private fun jsonParseExpression(filed: Filed): String {
        val type = filed.type.replace("?", "")
        val name = filed.name
        //从json里取值的名称
        val jsonName = filed.getValueByName("name") ?: name
        //是否是list
        val isListType = isListType(type)
        return when {
            //是否是基础数据类型
            isPrimitiveType(type) -> {
                when {
                    isListType -> {
                        "if (json['$jsonName'] != null) {\n" +
                                "    data.$name = (json['$jsonName'] as List).map((v) => ${
                                    buildToType(
                                        getListSubType(type),
                                        "v"
                                    )
                                }).toList().cast<${getListSubType(type)}>();\n" +
                                "  }"
                    }

                    type == "DateTime" -> {
                        "if(json['$jsonName'] != null){\n    data.$name = DateTime.parse(json['$jsonName']);\n  }"
                    }

                    else -> {
                        "if (json['$jsonName'] != null) {\n    data.$name = ${
                            buildToType(
                                type,
                                "json['$jsonName']"
                            )
                        };\n  }"
                    }
                }
            }

            isListType -> { // list of class  //如果是list,就把名字修改成单数
                //类名
                val listSubType = getListSubType(type)
                "if (json['$jsonName'] != null) {\n" +
                        "    data.$name = (json['$jsonName'] as List).map((v) => ${listSubType}().fromJson(v)).toList();\n" +
                        "  }"
            }

            else -> // class
                "if (json['$jsonName'] != null) {\n    data.$name = ${
                    type.replace(
                        "?",
                        ""
                    )
                }().fromJson(json['$jsonName']);\n  }"
        }
    }

    //生成toJson方法
    private fun jsonGenFunc(): String {
        val sb = StringBuffer()
        sb.append("Map<String, dynamic> ${className.toLowerCaseFirstOne()}ToJson(${className} entity) {\n")
        sb.append("  final Map<String, dynamic> data = {};\n")
        fields.forEach { k ->
            //如果serialize不是false,那么就解析,否则不解析
            if (k.getValueByName<Boolean>("serialize") != false) {
                sb.append("  ${toJsonExpression(k)}\n")
            }
        }
        sb.append("  return data;\n")
        sb.append("}")
        return sb.toString()

    }

    private fun toJsonExpression(filed: Filed): String {
        val type = filed.type
        val name = filed.name
        //从json里取值的名称
        val getJsonName = filed.getValueByName("name") ?: name
        //是否是list
        val isListType = isListType(type)
        val isLate = filed.isLate
        val thisKey = "entity.$name"
        when {
            //是否是基础数据类型
            isPrimitiveType(type) -> {
                return when {
                    isListType && getListSubType(type) == "DateTime" -> {
                        "${thisKey}\n" +
                                "        ${getCallSymbol(filed.isLate)}map((v) => v?.toString())\n" +
                                "        .toList()\n" +
                                "        .cast<String>();"
                    }

                    (type == "DateTime" && isLate) -> {
                        "data['${getJsonName}'] = ${thisKey}.toString();"
                    }

                    type == "DateTime" || type == "DateTime?" -> {
                        "data['${getJsonName}'] = ${thisKey}?.toString();"
                    }

                    else -> "data['$getJsonName'] = $thisKey;"
                }
            }

            isListType -> {
                //如果泛型里包含?,那么就需要?调用了
                val nullType = if (getListSubTypeCanNull(type).last() == '?') "?." else "."
                //类名
                val value =
                    "$thisKey${getCallSymbol(filed.isLate)}map((v) => v${nullType}toJson()).toList()"
                // class list
                return "data['$getJsonName'] =  $value;"
            }

            else -> {
                // class
                return "data['$getJsonName'] = ${thisKey}${getCallSymbol(filed.isLate)}toJson();"
            }
        }
    }

    private fun getCallSymbol(isLate: Boolean): String {
        return if (isLate) {
            return "."
        } else "?."
    }


    private fun buildToType(typeName: String, value: String): String {
        return when {
            typeName.equals("int", true) -> {
                "$value is String\n" +
                        "        ? int.tryParse(${value})\n" +
                        "        : ${value}.toInt()"
            }

            typeName.equals("double", true) -> {
                "$value is String\n" +
                        "        ? double.tryParse(${value})\n" +
                        "        : ${value}.toDouble()"
            }

            typeName.equals("num", true) -> {
                "$value is String\n" +
                        "        ? num.tryParse(${value})\n" +
                        "        : $value"
            }

            typeName.equals("string", true) -> {
                "${value}.toString()"
            }

            typeName.equals("DateTime", true) -> {
                "DateTime.parse(${value})"
            }

            else -> value
        }
    }


}

class Filed(
    /// 字段类型
    var type: String,
    /// 字段名字
    var name: String,
    /// 是否是late修饰
    var isLate: Boolean
) {

    //注解的值
    var annotationValue: List<AnnotationValue>? = null

    fun <T> getValueByName(name: String): T? {
        return annotationValue?.firstOrNull { it.name == name }?.getValueByName()
    }
}

@Suppress("UNCHECKED_CAST")
class AnnotationValue(val name: String, private val value: Any) {
    fun <T> getValueByName(): T {
        return value as T
    }
}
