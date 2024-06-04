package com.github.zhangruiyu.flutterjsonbeanfactory.action.dart_to_helper

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileImpl
import com.github.zhangruiyu.flutterjsonbeanfactory.file.FileHelpers
import com.github.zhangruiyu.flutterjsonbeanfactory.utils.YamlHelper
import com.github.zhangruiyu.flutterjsonbeanfactory.utils.commitContent
import com.github.zhangruiyu.flutterjsonbeanfactory.workers.FileGenerator
import com.github.zhangruiyu.flutterjsonbeanfactory.utils.showNotify
import java.io.File
import java.lang.RuntimeException

class FlutterBeanFactoryAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        generateAllFile(e.getData(PlatformDataKeys.PROJECT)!!)

    }

    companion object {
        /**
         * 生成辅助类
         */
        fun generateAllFile(project: Project) {
            val pubSpecConfig = YamlHelper.getPubSpecConfig(project)
            //判断是否是flutter项目
            if (pubSpecConfig != null && YamlHelper.shouldActivateFor(project)) {
                try {
                    //如果没有可以生成的文件,那么就不会生成
                    val allClass = FileHelpers.getAllEntityFiles(project)
                    if (allClass.isEmpty()) {
                        throw RuntimeException("No classes that inherit JsonConvert were found,the project root directory must contain the lib directory, because this plugin will only scan the lib directory of the root directory")
                    }
                    ApplicationManager.getApplication().invokeLater {
                        runWriteAction {
                            FileGenerator(project).generate()
                        }
                    }
                    FileHelpers.getGeneratedFileRun(project,pubSpecConfig.generatedPath) {
                        //1.上次生成的老旧老文件
                        val oldHelperChildren =
                            it.children.filterIsInstance<VirtualFileImpl>().toMutableList()
                        //2.删除多余helper文件
                        oldHelperChildren.forEach { oldPath ->
                            //新生成的文件名
                            val newFileNameList =
                                allClass.map { newPath -> "${File(newPath.second).nameWithoutExtension}.g.dart" }
                            //如果现在生成的不包含在这里,那么就删除
                            if (newFileNameList.contains(oldPath.name).not()) {
                                oldPath.delete(oldPath)
                            }
                        }
                        //3.重新生成所有helper类
                        FileHelpers.generateAllDartEntityHelper(project,pubSpecConfig.generatedPath, allClass)
                        //4.重新生成jsonConvert类
                        val content = StringBuilder()
                        content.append("// ignore_for_file: non_constant_identifier_names\n// ignore_for_file: camel_case_types\n// ignore_for_file: prefer_single_quotes\n\n")
                        content.append("// This file is automatically generated. DO NOT EDIT, all your changes would be lost.\n")
                        content.append("import 'package:flutter/material.dart' show debugPrint;\n")
                        //导包
                        allClass.forEach { itemNeedFile ->
                            content.append(itemNeedFile.second)
                            content.append("\n")
                        }

                        content.append("\n")

                        ////
                        content.append("JsonConvert jsonConvert = JsonConvert();")
                        content.append("\n")
                        content.append("typedef JsonConvertFunction<T> = T Function(Map<String, dynamic> json);")
                        content.append("\n")
                        content.append("typedef EnumConvertFunction<T> = T Function(String value);")
                        content.append("\n")
                        content.append("typedef ConvertExceptionHandler = void Function(Object error, StackTrace stackTrace);")
                        content.append("\n")
                        content.append("extension MapSafeExt<K, V> on Map<K, V> {\n" +
                                "  T? getOrNull<T>(K? key) {\n" +
                                "    if (!containsKey(key) || key == null) {\n" +
                                "      return null;\n" +
                                "    } else {\n" +
                                "      return this[key] as T?;\n" +
                                "    }\n" +
                                "  }\n" +
                                "}")
                        content.append("\n")
                        content.append("class JsonConvert {")
                        content.append("\n")
                        content.append("\tstatic ConvertExceptionHandler? onError;")
                        content.append("\n")
                        ///这里本来写成get方法的,虽然解决了hotreload无法更新的问题,但是会导致效率低下,对jsonarray有很多值的情况下,遍历会导致重复get效率低下
                        content.append("\tJsonConvertClassCollection convertFuncMap = JsonConvertClassCollection();")
                        content.append("\t/// When you are in the development, to generate a new model class, hot-reload doesn't find new generation model class, you can build on MaterialApp method called jsonConvert. ReassembleConvertFuncMap (); This method only works in a development environment\n")
                        content.append("\t/// https://flutter.cn/docs/development/tools/hot-reload\n")
                        content.append("\t/// class MyApp extends StatelessWidget {\n")
                        content.append("\t///    const MyApp({Key? key})\n")
                        content.append("\t///        : super(key: key);\n")
                        content.append("\t///\n")
                        content.append("\t///    @override\n")
                        content.append("\t///    Widget build(BuildContext context) {\n")
                        content.append("\t///      jsonConvert.reassembleConvertFuncMap();\n")
                        content.append("\t///      return MaterialApp();\n")
                        content.append("\t///    }\n")
                        content.append("\t/// }\n")
                        content.append("\tvoid reassembleConvertFuncMap(){\n")
                        content.append("\tbool isReleaseMode = const bool.fromEnvironment('dart.vm.product');\n")
                        content.append("\tif(!isReleaseMode) {\n")
                        content.append("\tconvertFuncMap = JsonConvertClassCollection();\n")
                        content.append("\t}\n")
                        content.append("\t}\n")
                        content.append(
                            "  T? convert<T>(dynamic value, {EnumConvertFunction? enumConvert}) {\n" +
                                    "    if (value == null) {\n" +
                                    "      return null;\n" +
                                    "    }\n" +
                                    "    if (value is T) {\n" +
                                    "      return value;\n" +
                                    "    }\n" +
                                    "    try {\n" +
                                    "      return _asT<T>(value, enumConvert: enumConvert);\n" +
                                    "    } catch (e, stackTrace) {\n" +
                                    "      debugPrint('asT<${"\$T"}> ${"\$e"} ${"\$stackTrace"}');\n" +
                                    "      if (onError != null) {" +
                                    "         onError!(e, stackTrace);" +
                                    "      }"+
                                    "      return null;\n" +
                                    "    }\n" +
                                    "  }"
                        )
                        content.append("\n\n")
                        content.append(
                            "  List<T?>? convertList<T>(List<dynamic>? value, {EnumConvertFunction? enumConvert}) {\n" +
                                    "    if (value == null) {\n" +
                                    "      return null;\n" +
                                    "    }\n" +
                                    "    try {\n" +
                                    "      return value.map((dynamic e) => _asT<T>(e,enumConvert: enumConvert)).toList();\n" +
                                    "    } catch (e, stackTrace) {\n" +
                                    "      debugPrint('asT<${"\$T"}> ${"\$e"} ${"\$stackTrace"}');\n" +
                                    "      if (onError != null) {" +
                                    "         onError!(e, stackTrace);" +
                                    "      }"+
                                    "      return <T>[];\n" +
                                    "    }\n" +
                                    "  }"
                        )
                        content.append("\n\n")
                        content.append(
                            "List<T>? convertListNotNull<T>(dynamic value, {EnumConvertFunction? enumConvert}) {\n" +
                                    "    if (value == null) {\n" +
                                    "      return null;\n" +
                                    "    }\n" +
                                    "    try {\n" +
                                    "      return (value as List<dynamic>).map((dynamic e) => _asT<T>(e,enumConvert: enumConvert)!).toList();\n" +
                                    "    } catch (e, stackTrace) {\n" +
                                    "      debugPrint('asT<${"\$T"}> ${"\$e"} ${"\$stackTrace"}');\n" +
                                    "      if (onError != null) {" +
                                    "         onError!(e, stackTrace);" +
                                    "      }"+
                                    "      return <T>[];\n" +
                                    "    }\n" +
                                    "  }"
                        )
                        content.append("\n\n")
                        content.append(
                            "  T? _asT<T extends Object?>(dynamic value,\n" +
                                    "      {EnumConvertFunction? enumConvert}) {\n" +
                                    "    final String type = T.toString();\n" +
                                    "    final String valueS = value.toString();\n" +
                                    "    if (enumConvert != null) {\n" +
                                    "      return enumConvert(valueS) as T;\n" +
                                    "    } else if (type == \"String\") {\n" +
                                    "      return valueS as T;\n" +
                                    "    } else if (type == \"int\") {\n" +
                                    "      final int? intValue = int.tryParse(valueS);\n" +
                                    "      if (intValue == null) {\n" +
                                    "        return double.tryParse(valueS)?.toInt() as T?;\n" +
                                    "      } else {\n" +
                                    "        return intValue as T;\n" +
                                    "      }\n" +
                                    "    } else if (type == \"double\") {\n" +
                                    "      return double.parse(valueS) as T;\n" +
                                    "    } else if (type == \"DateTime\") {\n" +
                                    "      return DateTime.parse(valueS) as T;\n" +
                                    "    } else if (type == \"bool\") {\n" +
                                    "      if (valueS == '0' || valueS == '1') {\n" +
                                    "        return (valueS == '1') as T;\n" +
                                    "      }\n" +
                                    "      return (valueS == 'true') as T;\n" +
                                    "    } else if (type == \"Map\" || type.startsWith(\"Map<\")) {\n" +
                                    "      return value as T;\n" +
                                    "    } else {\n" +
                                    "      if (convertFuncMap.containsKey(type)) {\n" +
                                    "        if (value == null) {\n" +
                                    "          return null;\n" +
                                    "        }\n" +
                                    "        var covertFunc = convertFuncMap[type]!;\n" +
                                    "        if(covertFunc is Map<String, dynamic>) {\n" +
                                    "          return covertFunc(value as Map<String, dynamic>) as T;\n" +
                                    "        }else{\n" +
                                    "          return covertFunc(Map<String, dynamic>.from(value)) as T;\n" +
                                    "        }\n" +
                                    "      } else {\n" +
                                    "        throw UnimplementedError('${"\$type"} unimplemented,you can try running the app again');\n" +
                                    "      }\n" +
                                    "    }\n" +
                                    "  }"
                        )
                        //_getListFromType
                        content.append("\n\n")
                        content.append(
                            "\t//list is returned by type\n" +
                                    "\tstatic M? _getListChildType<M>(List<Map<String, dynamic>> data) {\n"
                        )
                        allClass.forEach { itemClass ->
                            itemClass.first.classes.forEach { itemFile ->
                                content.append("\t\tif(<${itemFile.className}>[] is M){\n")
                                content.append("\t\t\treturn data.map<${itemFile.className}>((Map<String, dynamic> e) => ${itemFile.className}.fromJson(e)).toList() as M;\n")
                                content.append("\t\t}\n")
                            }
                        }
                        content.append(
                            "\n\t\tdebugPrint(\"\$M not found\");\n\t"
                        )
                        content.append(
                            "\n\t\treturn null;\n"
                        )
                        content.append(
                            "\t}"
                        )
                        content.append("\n\n")
                        //fromJsonAsT
                        content.append(
                            "\tstatic M? fromJsonAsT<M>(dynamic json) {\n" +
                                    "\t\tif (json is M) {\n" +
                                    "\t\t\treturn json;\n" +
                                    "\t\t}\n" +
                                    "\t\tif (json is List) {\n" +
                                    "\t\t\treturn _getListChildType<M>(json.map((dynamic e) => e as Map<String, dynamic>).toList());\n" +
                                    "\t\t} else {\n" +
                                    "\t\t\treturn jsonConvert.convert<M>(json);\n" +
                                    "\t\t}\n" +
                                    "\t}"
                        )

                        content.append("\n")
                        content.append("}\n\n")
                        content.append("\tclass JsonConvertClassCollection {")
                        content.append("\tMap<String, JsonConvertFunction> convertFuncMap = {")
                        content.append("\n")
                        allClass.forEach { itemClass ->
                            itemClass.first.classes.forEach { itemFile ->
                                content.append("\t\t(${itemFile.className}).toString(): ${itemFile.className}.fromJson,\n")
                            }
                        }
                        content.append("\t};\n")
                        content.append("bool containsKey(String type) {\n")
                        content.append("return convertFuncMap.containsKey(type);\n")
                        content.append("}\n")
                        content.append("JsonConvertFunction? operator [](String key) {\n")
                        content.append("return convertFuncMap[key];\n")
                        content.append("}\n")
                        content.append("\t}")
                        val generated = FileHelpers.getJsonConvertBaseFile(project,pubSpecConfig.generatedPath)
                        //获取json_convert_content目录,并写入
                        generated.findOrCreateChildData(this, "json_convert_content.dart")
                            .commitContent(project, content.toString())

                        project.showNotify("convert factory is generated")
                    }
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                    e.message?.let { project.showNotify(it) }
                }


            } else {
                project.showNotify("This project is not the flutter project")
            }


        }
    }
}
