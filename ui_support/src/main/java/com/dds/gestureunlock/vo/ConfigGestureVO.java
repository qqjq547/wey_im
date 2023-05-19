package com.dds.gestureunlock.vo;

import com.dds.gestureunlock.util.ResourceUtil;

import java.io.Serializable;

public class ConfigGestureVO implements Serializable {

    private int minimumCodeLength;
    private int maximumAllowTrialTimes;
    private int currentAllowTrialTimes;
    private int totalErrorCount;
    private long errorRemainInterval;
    private long successRemainInterval;
    private String backgroundColor;
    private String normalThemeColor;
    private String normalTextThemeColor;
    private String selectedThemeColor;
    private String errorThemeColor;
    private String creationBeginPrompt;
    private String codeLengthErrorPrompt;
    private String codeCheckPrompt;
    private String checkErrorPrompt;
    private String creationSucceedPrompt;
    private String verificationBeginPrompt;
    private String verificationErrorPrompt;
    private String verificationErrorMaxPrompt;
    private String verificationErrorDisableAccountWarn;
    private String verificationSucceedPrompt;
    private String restartCreationButtonTitle;
    private String backgroundImage;
    private String iconImage;
    //是否显示轨迹
    private boolean isShowTrack;

    private boolean disableAccountIsOpen;

    private String unSelectColor1;
    private String unSelectColor2;
    private String lineColor;

    public ConfigGestureVO() {
        restoreDefaultConfig();
    }

    /**
     * 获取默认配置参数
     *
     * @return
     */
    public static ConfigGestureVO defaultConfig() {
        return new ConfigGestureVO().restoreDefaultConfig();
    }

    /**
     * 重置默认参数
     */
    private ConfigGestureVO restoreDefaultConfig() {
        this.minimumCodeLength = 4;
        this.maximumAllowTrialTimes = 5;
        this.currentAllowTrialTimes = 5;
        this.errorRemainInterval = 1000;
        this.successRemainInterval = 200;
        this.backgroundColor = "#FFFFFF";
        this.normalThemeColor = "#d4d6d9";
        this.normalTextThemeColor = "#000000";
        this.selectedThemeColor = "#178aff";
        this.errorThemeColor = "#f50d2e";
        this.creationBeginPrompt = ResourceUtil.getString("plugin_uexGestureUnlock_creationBeginPrompt");
        this.codeLengthErrorPrompt = ResourceUtil.getString("plugin_uexGestureUnlock_codeLengthErrorPrompt");
        this.codeCheckPrompt = ResourceUtil.getString("plugin_uexGestureUnlock_codeCheckPrompt");
        this.checkErrorPrompt = ResourceUtil.getString("plugin_uexGestureUnlock_checkErrorPrompt");
        this.creationSucceedPrompt = ResourceUtil.getString("plugin_uexGestureUnlock_creationSucceedPrompt");
        this.verificationBeginPrompt = ResourceUtil.getString("plugin_uexGestureUnlock_verificationBeginPrompt");
        this.verificationErrorPrompt = ResourceUtil.getString("plugin_uexGestureUnlock_verificationErrorPrompt");
        this.verificationErrorMaxPrompt = ResourceUtil.getString("plugin_uexGestureUnlock_verificationErrorMaxPrompt");
        this.verificationErrorDisableAccountWarn = ResourceUtil.getString("verification_error_disable_account_warn");
        this.verificationSucceedPrompt = ResourceUtil.getString("plugin_uexGestureUnlock_verificationSucceedPrompt");
        this.restartCreationButtonTitle = ResourceUtil.getString("plugin_uexGestureUnlock_restartCreationButtonTitle");
        this.backgroundImage = null;
        this.iconImage = null;
        this.isShowTrack = true;
        this.disableAccountIsOpen = false;
        this.unSelectColor1 = "#F8FAFD";
        this.unSelectColor2 = "#D5E1F6";
        this.lineColor = "#8DA7D3";
        return this;
    }

    public boolean isShowTrack() {
        return isShowTrack;
    }

    public void setShowTrack(boolean showTrack) {
        isShowTrack = showTrack;
    }

    public int getMinimumCodeLength() {
        return minimumCodeLength;
    }

    public void setMinimumCodeLength(int minimumCodeLength) {
        this.minimumCodeLength = minimumCodeLength;
    }

    public int getMaximumAllowTrialTimes() {
        return maximumAllowTrialTimes;
    }

    public void setMaximumAllowTrialTimes(int maximumAllowTrialTimes) {
        this.maximumAllowTrialTimes = maximumAllowTrialTimes;
    }

    public int getCurrentAllowTrialTimes() {
        return currentAllowTrialTimes;
    }

    public void setCurrentAllowTrialTimes(int currentAllowTrialTimes) {
        this.currentAllowTrialTimes = currentAllowTrialTimes;
    }

    public int getTotalErrorCount() {
        return totalErrorCount;
    }

    public void setTotalErrorCount(int totalErrorCount) {
        this.totalErrorCount = totalErrorCount;
    }

    public long getErrorRemainInterval() {
        return errorRemainInterval;
    }

    public void setErrorRemainInterval(long errorRemainInterval) {
        this.errorRemainInterval = errorRemainInterval;
    }

    public long getSuccessRemainInterval() {
        return successRemainInterval;
    }

    public void setSuccessRemainInterval(long successRemainInterval) {
        this.successRemainInterval = successRemainInterval;
    }

    public int getBackgroundColor() {
        return ResourceUtil.parseColor(backgroundColor);
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getNormalThemeColor() {
        return ResourceUtil.parseColor(normalThemeColor);
    }

    public void setNormalThemeColor(String normalThemeColor) {
        this.normalThemeColor = normalThemeColor;
    }

    public int getNormalTextThemeColor() {
        return ResourceUtil.parseColor(normalTextThemeColor);
    }

    public void setNormalTextThemeColor(String normalTextThemeColor) {
        this.normalTextThemeColor = normalTextThemeColor;
    }

    public int getSelectedThemeColor() {
        return ResourceUtil.parseColor(selectedThemeColor);
    }

    public void setSelectedThemeColor(String selectedThemeColor) {
        this.selectedThemeColor = selectedThemeColor;
    }

    public int getErrorThemeColor() {
        return ResourceUtil.parseColor(errorThemeColor);
    }

    public void setErrorThemeColor(String errorThemeColor) {
        this.errorThemeColor = errorThemeColor;
    }

    public String getCreationBeginPrompt() {
        return creationBeginPrompt;
    }

    public void setCreationBeginPrompt(String creationBeginPrompt) {
        this.creationBeginPrompt = creationBeginPrompt;
    }

    public String getCodeLengthErrorPrompt() {
        return codeLengthErrorPrompt;
    }

    public void setCodeLengthErrorPrompt(String codeLengthErrorPrompt) {
        this.codeLengthErrorPrompt = codeLengthErrorPrompt;
    }

    public String getCodeCheckPrompt() {
        return codeCheckPrompt;
    }

    public void setCodeCheckPrompt(String codeCheckPrompt) {
        this.codeCheckPrompt = codeCheckPrompt;
    }

    public String getCheckErrorPrompt() {
        return checkErrorPrompt;
    }

    public void setCheckErrorPrompt(String checkErrorPrompt) {
        this.checkErrorPrompt = checkErrorPrompt;
    }

    public String getCreationSucceedPrompt() {
        return creationSucceedPrompt;
    }

    public void setCreationSucceedPrompt(String creationSucceedPrompt) {
        this.creationSucceedPrompt = creationSucceedPrompt;
    }

    public String getVerificationBeginPrompt() {
        return verificationBeginPrompt;
    }

    public void setVerificationBeginPrompt(String verificationBeginPrompt) {
        this.verificationBeginPrompt = verificationBeginPrompt;
    }

    public String getVerificationErrorPrompt() {
        return verificationErrorPrompt;
    }

    public void setVerificationErrorPrompt(String verificationErrorPrompt) {
        this.verificationErrorPrompt = verificationErrorPrompt;
    }

    public String getVerificationErrorMaxPrompt() {
        return verificationErrorMaxPrompt;
    }

    public void setVerificationErrorMaxPrompt(String verificationErrorMaxPrompt) {
        this.verificationErrorMaxPrompt = verificationErrorMaxPrompt;
    }

    public String getVerificationErrorDisableAccountWarn() {
        return verificationErrorDisableAccountWarn;
    }

    public void setVerificationErrorDisableAccountWarn(String verificationErrorDisableAccountWarn) {
        this.verificationErrorDisableAccountWarn = verificationErrorDisableAccountWarn;
    }

    public String getVerificationSucceedPrompt() {
        return verificationSucceedPrompt;
    }

    public void setVerificationSucceedPrompt(String verificationSucceedPrompt) {
        this.verificationSucceedPrompt = verificationSucceedPrompt;
    }

    public String getRestartCreationButtonTitle() {
        return restartCreationButtonTitle;
    }

    public void setRestartCreationButtonTitle(String restartCreationButtonTitle) {
        this.restartCreationButtonTitle = restartCreationButtonTitle;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public String getIconImage() {
        return iconImage;
    }

    public void setIconImage(String iconImage) {
        this.iconImage = iconImage;
    }

    public int getUnSelectColor1() {
        return ResourceUtil.parseColor(unSelectColor1);
    }

    public void setUnSelectColor1(String unSelectColor1) {
        this.unSelectColor1 = unSelectColor1;
    }

    public int getUnSelectColor2() {
        return ResourceUtil.parseColor(unSelectColor2);
    }

    public void setUnSelectColor2(String unSelectColor2) {
        this.unSelectColor2 = unSelectColor2;
    }

    public void setDisableAccountIsOpen(boolean disableAccountIsOpen) {
        this.disableAccountIsOpen = disableAccountIsOpen;
    }

    public boolean isDisableAccountIsOpen() {
        return disableAccountIsOpen;
    }

    public int getLineColor() {
        return ResourceUtil.parseColor(lineColor);
    }

    public void setLineColor(String lineColor) {
        this.lineColor = lineColor;
    }
}
