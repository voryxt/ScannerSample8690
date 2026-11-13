package com.honeywell.scanner.gen7sdk.ScannerSample;


import com.honeywell.scanner.sdk.common.seychellesetting.*;
import com.honeywell.scanner.sdk.common.seychellesetting.TextFontSize;

public class DeviceSetting {
    public  String m_text = "";
    public  DeviceTextSetting.TextLineType m_TextLineType = DeviceTextSetting.TextLineType.UpLine;
    public  int m_TextFontSize = TextFontSize.Medium;
    public  FontSizeSetting.TextLineType m_FontSizeLineType = FontSizeSetting.TextLineType.UpLine;
    public  DeviceLanguageSetting.LanguageOptions m_LanguageOption = DeviceLanguageSetting.LanguageOptions.loEnglish;
    public  DeviceTextColorSetting.TextColors m_TextColor = DeviceTextColorSetting.TextColors.Blue;
    public  DeviceTextColorSetting.TextColorType m_TextColorType = DeviceTextColorSetting.TextColorType.BgColor;

    public  boolean m_EnableButtonPress = false;
    public  boolean m_bENA13_Enable = false;
    public  boolean m_bQR_Enable = false;
}
