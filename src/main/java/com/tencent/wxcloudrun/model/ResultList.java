package com.tencent.wxcloudrun.model;

import lombok.Data;

import java.util.List;

/**
 * @Name: ResultList
 * @Desc:
 * @Author Liff
 * @Date 2022/5/19
 */
@Data
public class ResultList {
    private List<FolderRes> items;
    private Integer count;
}
