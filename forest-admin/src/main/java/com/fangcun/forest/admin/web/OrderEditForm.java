package com.fangcun.forest.admin.web;

import jakarta.validation.constraints.*;
import com.fangcun.forest.admin.entity.SalesOrder;

public class OrderEditForm {
    @NotBlank(message = "请填写收货人")
    @Size(max = 50)
    private String receiverName;
    @NotBlank(message = "请填写电话")
    @Pattern(regexp = "[0-9+() -]{6,20}", message = "联系电话格式不正确")
    private String receiverPhone;
    @NotBlank(message = "请填写地址")
    @Size(min = 5, max = 255, message = "地址须为 5—255 个字符")
    private String receiverAddress;
    @NotNull
    @Min(0)
    private Integer version = 0;

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String value) { receiverName = value; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String value) { receiverPhone = value; }
    public String getReceiverAddress() { return receiverAddress; }
    public void setReceiverAddress(String value) { receiverAddress = value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { version = value; }
    public static OrderEditForm from(SalesOrder order) {
        OrderEditForm form = new OrderEditForm(); form.setReceiverName(order.getReceiverName());
        form.setReceiverPhone(order.getReceiverPhone()); form.setReceiverAddress(order.getReceiverAddress());
        form.setVersion(order.getVersion()); return form;
    }
}
