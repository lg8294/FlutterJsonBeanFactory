package com.lg.utils

import com.google.gson.JsonArray
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.lg.PLUGIN_NOTIFICATION_GROUP_NAME
import java.awt.Component
import java.awt.Container
import javax.swing.Box
import javax.swing.BoxLayout


/**
 *
 * Created by Seal.Wu on 2017/9/25.
 */

fun Container.addComponentIntoVerticalBoxAlignmentLeft(component: Component) {
    if (layout is BoxLayout) {

        val hBox = Box.createHorizontalBox()
        hBox.add(component)
        hBox.add(Box.createHorizontalGlue())
        add(hBox)
    }

}


/**
 * array only has one element
 */
private fun JsonArray.onlyHasOneElement(): Boolean {
    return size() == 1
}

/**
 * array only has object element
 */
private fun JsonArray.allObjectElement(): Boolean {
    forEach {
        if (it.isJsonObject.not()) {
            return false
        }
    }
    return true
}

fun Project.showNotify(notifyMessage: String) {
    try {
        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup(PLUGIN_NOTIFICATION_GROUP_NAME)
        ApplicationManager.getApplication().invokeLater {
            val notification = notificationGroup.createNotification(notifyMessage, NotificationType.INFORMATION)
            Notifications.Bus.notify(notification, this)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Project.showErrorMessage(notifyMessage: String) {
    Messages.showInfoMessage(this, notifyMessage, "Info")
}

