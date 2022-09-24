package com.lg.setting

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.lg.PLUGIN_NAME

const val settingName = "${PLUGIN_NAME}Settings"
const val settingStorageName = "${PLUGIN_NAME}Settings.xml"

@State(name = settingName, storages = [(Storage(settingStorageName))])
data class Settings(
    var modelSuffix: String,
    var isOpenNullSafety: Boolean?,
    var isOpenNullAble: Boolean?
) : PersistentStateComponent<Settings> {

    constructor() : this(
        "entity", null, null
    )

    override fun getState(): Settings {
        return this
    }

    override fun loadState(state: Settings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}