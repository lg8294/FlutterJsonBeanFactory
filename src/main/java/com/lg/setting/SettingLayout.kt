package com.lg.setting

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.lg.utils.addComponentIntoVerticalBoxAlignmentLeft
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel


class SettingLayout(settingState: Settings) {
    private val panel: JPanel = JPanel(BorderLayout())
    private val beanNameTextField: JBTextField

    init {

        val beanNameLayout = createLinearLayoutVertical()
        val beanName = JBLabel()
        beanName.border = JBUI.Borders.empty(5, 0)
        beanName.text = "model suffix"
        beanNameLayout.addComponentIntoVerticalBoxAlignmentLeft(beanName)
        beanNameTextField = JBTextField(settingState.modelSuffix)
        beanNameTextField.preferredSize = JBDimension(400, 40)
        beanNameLayout.addComponentIntoVerticalBoxAlignmentLeft(beanNameTextField)


        panel.add(beanNameLayout, BorderLayout.NORTH)

        val label1 = JBLabel()
        label1.border = JBUI.Borders.empty(5, 0)
        label1.text = "Configure scan suffix files(Please separate them with commas)"
        beanNameLayout.addComponentIntoVerticalBoxAlignmentLeft(
            label1
        )
        panel.add(createLinearLayoutVertical(), BorderLayout.AFTER_LAST_LINE)
    }

    fun getRootComponent(): JComponent {
        return this.panel
    }


    fun getModelSuffix(): String {
        return beanNameTextField.text
    }


}

fun createLinearLayoutVertical(): JPanel {
    val container = JPanel()
    val boxLayout = BoxLayout(container, BoxLayout.PAGE_AXIS)
    container.layout = boxLayout
    return container
}