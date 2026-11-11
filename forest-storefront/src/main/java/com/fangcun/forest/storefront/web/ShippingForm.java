package com.fangcun.forest.storefront.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ShippingForm {
    @NotBlank(message = "请填写收货人")
    @Size(max = 50, message = "收货人最多 50 个字符")
    private String receiverName;
    @NotBlank(message = "请填写联系电话")
    @Pattern(regexp = "[0-9+() -]{6,20}", message = "联系电话须为 6—20 位数字、空格或电话符号")
    private String receiverPhone;
    @NotBlank(message = "请填写收货地址")
    @Size(min = 5, max = 255, message = "收货地址须为 5—255 个字符")
    private String receiverAddress;
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String value) { receiverName = value; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String value) { receiverPhone = value; }
    public String getReceiverAddress() { return receiverAddress; }
    public void setReceiverAddress(String value) { receiverAddress = value; }
}
