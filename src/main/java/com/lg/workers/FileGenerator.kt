package com.lg.workers

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.lg.file.FileHelpers
import com.lg.helper.YamlHelper

/**
 * User: zhangruiyu
 * Date: 2019/12/22
 * Time: 15:34
 */
class FileGenerator(private val project: Project) {

    private var psiManager: PsiManager? = null
    private var documentManager: PsiDocumentManager? = null

    init {
        try {
            psiManager = PsiManager.getInstance(project)
        } catch (ignored: Throwable) {
        }
        try {
            documentManager = PsiDocumentManager.getInstance(project)
        } catch (ignored: Throwable) {
        }
    }

    fun generate() {
        if (!YamlHelper.shouldActivateFor(project)) {
            return
        }
        psiManager?.let { psiManager ->
            documentManager?.let { documentManager ->
                val builder = StringBuilder()
                builder.append("// ignore_for_file: non_constant_identifier_names\n// ignore_for_file: camel_case_types\n// ignore_for_file: prefer_single_quotes\n\n")
                builder.append("// This file is automatically generated. DO NOT EDIT, all your changes would be lost.\n")
//                builder.append(JsonConvertContent)
//                builder.append("\n")
                builder.append(getJSONFieldContent(project))
                val jsonConvertContent = builder.toString()
                FileHelpers.getJsonConvertJsonFiledFile(project) { file ->
                    psiManager.findFile(file)?.let { dartFile ->
                        documentManager.getDocument(dartFile)?.let { document ->
                            if (document.text != jsonConvertContent) {
                                document.setText(jsonConvertContent)
                                documentManager.commitDocument(document)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {

        fun getJSONFieldContent(project: Project): String {
            val isNullSafe = YamlHelper.getPubSpecConfig(project)?.isNullSafe ?: true
            return if (isNullSafe) """
class JSONField {
  /// Specify the parse field name
  final String? name;

  /// Whether to participate in toJson
  final bool? serialize;

  /// Whether to participate in fromMap
  final bool? deserialize;

  const JSONField({this.name, this.serialize, this.deserialize});
}
""" else """
class JSONField {
  /// Specify the parse field name
  final String name;

  /// Whether to participate in toJson
  final bool serialize;
  
  /// Whether to participate in fromMap
  final bool deserialize;

  const JSONField({this.name, this.serialize, this.deserialize});
}
"""
        }

    }
}