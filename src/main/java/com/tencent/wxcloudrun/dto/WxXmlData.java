package com.tencent.wxcloudrun.dto;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

import java.io.Serializable;

/**
 * @Name: WxXmlData
 * @Desc:
 * @Author Liff
 * @Date 2022/1/23
 */
@Data
@XStreamAlias("xml")
public class WxXmlData implements Serializable {

    @XStreamAlias("ToUserName")
    private String toUserName;
    @XStreamAlias("FromUserName")
    private String fromUserName;
    @XStreamAlias("CreateTime")
    private Long createTime;
    @XStreamAlias("MsgType")
    private String msgType;
    @XStreamAlias("Content")
    private String content;
    @XStreamAlias("MsgId")
    private String msgId;
    //
    @XStreamAlias("Title")
    private String title;
    @XStreamAlias("Description")
    private String description;
    @XStreamAlias("Url")
    private String url;
    /**
     * 订阅或者取消订阅的事件
     */
    @XStreamAlias("Event")
    private String event;
    @XStreamAlias("EventKey")
    private String eventkey;
}