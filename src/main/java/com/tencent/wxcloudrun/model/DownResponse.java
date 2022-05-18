package com.tencent.wxcloudrun.model;

import lombok.Data;

/**
 * @Name: WebResponse
 * @Desc:
 * @Author Liff
 * @Date 2022/5/19
 */
@Data
public class DownResponse {

    private String status;
    private String msg;
    private Result result;

}
