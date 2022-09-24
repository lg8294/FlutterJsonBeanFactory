package com.lg.setting

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.Configurable
import com.lg.PLUGIN_NAME
import javax.swing.JComponent

@State(name = settingName, storages = [Storage(settingStorageName)])
class SettingComponent : Configurable {
    var settingLayout: SettingLayout? = null
    override fun isModified(): Boolean {
        if (settingLayout == null) {
            return false
        }
        return getSettings() != Settings(
                settingLayout!!.getModelSuffix(),null,false)
    }

    override fun getDisplayName(): String {
        return PLUGIN_NAME
    }

    override fun apply() {
        settingLayout?.run {
            getSettings().apply {
                modelSuffix = getModelSuffix()
            }
        }
    }


    override fun createComponent(): JComponent? {
        settingLayout = SettingLayout(getSettings())
        return settingLayout!!.getRootComponent()
    }

    private fun getSettings(): Settings {
        return ServiceManager.getService(Settings::class.java).state
    }

}
