package com.lg.jsontodart

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.project.Project
import com.lg.helper.YamlHelper
import com.lg.jsontodart.utils.camelCase
import com.lg.utils.GsonUtil.MapTypeAdapter
import com.lg.utils.JsonUtils
import com.lg.utils.toUpperCaseFirstOne


class ModelGenerator(
    private val collectInfo: CollectInfo,
    val project: Project
) {
    private var isFirstClass = true
    private var allClasses = mutableListOf<ClassDefinition>()

    //parentType 父类型 是list 或者class
    private fun generateClassDefinition(
        className: String,
        parentName: String,
        jsonRawData: Any,
        parentType: String = ""
    ): MutableList<ClassDefinition> {
        val newClassName = parentName + className
        if (jsonRawData is List<*>) {
            // if first element is an array, start in the first element.
            generateClassDefinition(newClassName, newClassName, jsonRawData[0]!!)
        } else if (jsonRawData is Map<*, *>) {
            val keys = jsonRawData.keys
            //如果是list,就把名字修改成单数
            val classDefinition = ClassDefinition(
                when {
                    "list" == parentType -> {
                        newClassName
                    }

                    isFirstClass -> {//如果是第一个类
                        isFirstClass = false
                        newClassName + collectInfo.modelSuffix().toUpperCaseFirstOne()
                    }

                    else -> {
                        newClassName
                    }
                }
            )
            keys.forEach { key ->
                val typeDef = TypeDefinition.fromDynamic(jsonRawData[key])
                if (typeDef.name == "Class") {
                    typeDef.name = newClassName + camelCase(key as String)
                }
                if (typeDef.subtype != null && typeDef.subtype == "Class") {
                    typeDef.subtype = newClassName + camelCase(key as String)
                }
                classDefinition.addField(key as String, typeDef)
            }
            if (allClasses.firstOrNull { cd -> cd == classDefinition } == null) {
                allClasses.add(classDefinition)
            }
            val dependencies = classDefinition.dependencies
            dependencies.forEach { dependency ->
                if (dependency.typeDef.name == "List") {
                    if (((jsonRawData[dependency.name]) as? List<*>)?.isNotEmpty() == true) {
                        val names = (jsonRawData[dependency.name] as List<*>)
                        generateClassDefinition(dependency.className, newClassName, names[0]!!, "list")
                    }
                } else {
                    generateClassDefinition(dependency.className, newClassName, jsonRawData[dependency.name]!!)
                }
            }
        }
        return allClasses
    }

    fun generateDartClassesToString(): String {
        //用阿里的防止int变为double 已解决 还是用google的吧 https://www.codercto.com/a/73857.html
//        val jsonRawData = JSON.parseObject(collectInfo.userInputJson)
        val originalStr = collectInfo.userInputJson.trim()
        val gson = GsonBuilder()
            .registerTypeAdapter(object : TypeToken<Map<String, Any>>() {}.type, MapTypeAdapter()).create()

        val jsonRawData = if (originalStr.startsWith("[")) {
            val list: List<Any> = gson.fromJson(originalStr, object : TypeToken<List<Any>>() {}.type)
            try {
                (JsonUtils.jsonMapMCompletion(list) as List<*>).first()
            } catch (e: Exception) {
                mutableMapOf<String, Any>()
            }

        } else {
            gson.fromJson<Map<String, Any>>(originalStr, object : TypeToken<Map<String, Any>>() {}.type)
        }
//        val jsonRawData = gson.fromJson<Map<String, Any>>(collectInfo.userInputJson, HashMap::class.java)
        val pubSpecConfig = YamlHelper.getPubSpecConfig(project)
        val classContentList = generateClassDefinition(
            collectInfo.firstClassName(), "", JsonUtils.jsonMapMCompletion(jsonRawData)
                ?: mutableMapOf<String, Any>()
        )
        val classContent = classContentList.joinToString("\n")
        classContentList.fold(mutableListOf<TypeDefinition>()) { acc, de ->
            acc.addAll(de.fields.map { it.value })
            acc
        }
        val stringBuilder = StringBuilder()
        //导包
        stringBuilder.append("import 'package:${pubSpecConfig?.name}/generated/json/base/json_convert_content.dart';")
        stringBuilder.append("\n")
        //说明需要导包json_field.dart
        if (classContent.contains("@JSONField(")) {
            stringBuilder.append("import 'package:${pubSpecConfig?.name}/generated/json/base/json_field.dart';")
            stringBuilder.append("\n")
        }
        stringBuilder.append("\n")
        stringBuilder.append(classContent)
        //生成helper类

        //生成
        return stringBuilder.toString()
    }


}