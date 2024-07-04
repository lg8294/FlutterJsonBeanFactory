package com.lg.jsontodart

import com.intellij.openapi.application.ApplicationManager
import com.lg.setting.Settings
import com.lg.utils.toLowerCaseFirstOne
import com.lg.utils.toUpperCaseFirstOne
import com.lg.utils.upperCharToUnderLine
import com.lg.utils.upperTable
import java.util.*

class CollectInfo {
    //用户输入的类名
    var userInputClassName = ""
    var userInputJson = ""

    //用户设置的后缀
    fun modelSuffix(): String {
        return ApplicationManager.getApplication()
            .getService(Settings::class.java).state.modelSuffix.lowercase(Locale.getDefault())
    }

    //用户输入的类名转为文件名
    fun transformInputClassNameToFileName(): String {
        return if (!userInputClassName.contains("_")) {
            (userInputClassName + modelSuffix().toUpperCaseFirstOne()).upperCharToUnderLine()
        } else {
            (userInputClassName + "_" + modelSuffix().toLowerCaseFirstOne())
        }

    }


    //用户输入的名字转为首个class的名字(文件中的类名)
    fun firstClassName(): String {
        return if (userInputClassName.contains("_")) {
            (upperTable(userInputClassName)).toUpperCaseFirstOne()
        } else {
            (userInputClassName).toUpperCaseFirstOne()
        }
    }

    //用户输入的名字转为首个class的名字(文件中的类名)
    fun firstClassEntityName(): String {
        return if (userInputClassName.contains("_")) {
            (upperTable(userInputClassName).toUpperCaseFirstOne() + modelSuffix().toUpperCaseFirstOne())
        } else {
            (userInputClassName.toUpperCaseFirstOne() + modelSuffix().toUpperCaseFirstOne())
        }
    }
}