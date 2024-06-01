package com.tencent.wxcloudrun.dto;

import lombok.Data;

@Data
public class MsgRequest {

  private String toUserName;
  private String fromUserName;
  private Long createTime;
  private String msgType;
  private String content;
  private String msgId;

}
