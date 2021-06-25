package com.ruiyu.beanfactory

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileImpl
import com.ruiyu.file.FileHelpers
import com.ruiyu.helper.YamlHelper
import com.ruiyu.helper.commitContent
import com.ruiyu.utils.toLowerCaseFirstOne
import com.ruiyu.workers.FileGenerator
import com.ruiyu.utils.showNotify
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
            if (YamlHelper.shouldActivateFor(project)) {
                try {
                    //如果没有可以生成的文件,那么就不会生成
                    val allClass = FileHelpers.getAllEntityFiles(project)
                    if (allClass.isEmpty()) {
                        throw RuntimeException("No classes that inherit JsonConvert were found")
                    }
                    ApplicationManager.getApplication().invokeLater {
                        runWriteAction {
                            FileGenerator(project).generate()
                        }
                    }
                    FileHelpers.getGeneratedFileRun(project) {
                        //上次生成的老旧老文件
                        val oldHelperChildren = it.children.filterIsInstance<VirtualFileImpl>().toMutableList()
                        //重新生成所有helper类
                        FileHelpers.generateAllDartEntityHelper(project, allClass)
                        val content = StringBuilder()
                        content.append("// ignore_for_file: non_constant_identifier_names\n// ignore_for_file: camel_case_types\n// ignore_for_file: prefer_single_quotes\n\n")
                        content.append("// This file is automatically generated. DO NOT EDIT, all your changes would be lost.\n")
                        //导包
                        allClass.forEach { itemNeedFile ->
                            //需要生成包名
                            val helpPackageName = "${File(itemNeedFile.second).nameWithoutExtension}_helper.dart"
                            content.append(
                                "import 'package:${pubSpecConfig?.name}/${
                                    itemNeedFile.second.substringAfter(
                                        "${pubSpecConfig?.name}/"
                                    )
                                }"
                            )
                            content.append("\n")
                            content.append("import 'package:${pubSpecConfig?.name}/generated/json/${helpPackageName}';")
                            content.append("\n")
                            oldHelperChildren.removeIf { oldItemFile ->
                                //删除包含的,剩下的就是多余的,比如之前生成过,现在被删除
                                oldItemFile.path.contains(helpPackageName)
                            }
                        }
                        //删除多余helper文件
                        oldHelperChildren.forEach { needDelFile ->
                            needDelFile.delete(needDelFile)
                        }
                        content.append("\n")

                        ////
                        content.append(
                            """class JsonConvert<T> {
	T fromJson(Map<String, dynamic> json) {
		return _getFromJson<T>(runtimeType, this, json);
	}"""
                        )

///
                        //tojson
                        content.append("\n\n");
                        content.append(
                            """  Map<String, dynamic> toJson() {
		return _getToJson<T>(runtimeType, this);
  }"""
                        )
                        content.append("\n\n");
                        content.append(
                            "  static _getFromJson<T>(Type type, data, json) {\n" +
                                    "    switch (type) {"
                        )
                        allClass.forEach { itemClass ->
                            itemClass.first.classes.forEach { itemFile ->
                                content.append("\n\t\t\tcase ${itemFile.className}:\n")
                                content.append("\t\t\t\treturn ${itemFile.className.toLowerCaseFirstOne()}FromJson(data as ${itemFile.className}, json) as T;")
                            }
                        }
                        content.append(
                            "    }\n" +
                                    "    return data as T;\n" +
                                    "  }"
                        )
                        content.append("\n\n");
                        content.append(
                            "  static _getToJson<T>(Type type, data) {\n" +
                                    "\t\tswitch (type) {"
                        )
                        allClass.forEach {
                            it.first.classes.forEach { itemFile ->
                                content.append("\n\t\t\tcase ${itemFile.className}:\n")
                                content.append("\t\t\t\treturn ${itemFile.className.toLowerCaseFirstOne()}ToJson(data as ${itemFile.className});")
                            }
                        }
                        content.append(
                            "\n\t\t\t}\n" +
                                    "\t\t\treturn data as T;\n" +
                                    "\t\t}"
                        )
                        content.append("\n");
                        //_fromJsonSingle
                        content.append(
                            "  //Go back to a single instance by type\n" +
                                    "\tstatic _fromJsonSingle<M>( json) {\n"
                        )
                        content.append("\t\tString type = M.toString();\n")
                        allClass.forEach { itemClass ->
                            val isFirstIf = allClass.indexOf(itemClass) == 0
                            itemClass.first.classes.forEach { itemFile ->
                                content.append("\t\tif(type == (${itemFile.className}).toString()){\n")
                                content.append("\t\t\treturn ${itemFile.className}().fromJson(json);\n")
                                content.append("\t\t}\n")
                            }
                        }
                        content.append(
                            "\n\t\treturn null;\n" +
                                    "\t}"
                        )

                        //_getListFromType
                        content.append("\n\n");
                        content.append(
                            "  //list is returned by type\n" +
                                    "\tstatic M _getListChildType<M>(List data) {\n"
                        )
                        allClass.forEach { itemClass ->
                            itemClass.first.classes.forEach { itemFile ->
                                content.append("\t\tif(<${itemFile.className}>[] is M){\n")
                                content.append("\t\t\treturn data.map<${itemFile.className}>((e) => ${itemFile.className}().fromJson(e)).toList() as M;\n")
                                content.append("\t\t}\n")
                            }
                        }
                        content.append(
                            "\n\t\tthrow Exception(\"not fond\");\n" +
                                    "\t}"
                        )
                        content.append("\n\n")
                        //fromJsonAsT
                        content.append(
                            "  static M fromJsonAsT<M>(json) {\n" +
                                    "\t\tif (json is List) {\n" +
                                    "\t\t\treturn _getListChildType<M>(json);\n" +
                                    "\t\t} else {\n" +
                                    "\t\t\treturn _fromJsonSingle<M>(json) as M;\n" +
                                    "\t\t}\n" +
                                    "\t}"
                        )

                        content.append("\n")
                        content.append("}")

                        FileHelpers.getJsonConvertContentFile(project) { itemVirtualFile ->
                            itemVirtualFile.commitContent(project, content.toString())
                        }


                        project.showNotify("convert factory is generated")
                    }
                } catch (e: RuntimeException) {
                    e.message?.let { project.showNotify(it) }
                }


            } else {
                project.showNotify("This project is not the flutter project or the flutterJson in pubspec.yaml with the enable set to false")
            }


        }
    }
}
