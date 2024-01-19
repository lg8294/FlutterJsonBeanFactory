package com.lg.workers


import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.lg.App


/**
 * User: zhangruiyu
 * Date: 2019/12/22
 * Time: 15:30
 */
class Initializer : StartupActivity, DocumentListener {

    override fun runActivity(project: Project) {
        App.project = project
    }
}