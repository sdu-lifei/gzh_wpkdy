package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.CounterRequest;
import com.tencent.wxcloudrun.model.Counter;
import com.tencent.wxcloudrun.service.CounterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 自动回复消息控制器
 */
@RestController
public class aotuReplyController {

  final CounterService counterService;
  final Logger logger;

  public aotuReplyController(@Autowired CounterService counterService) {
    this.counterService = counterService;
    this.logger = LoggerFactory.getLogger(aotuReplyController.class);
  }

  /**
   * 微信成为开发者 接口
   * @param signature : 签名
   * @param timestamp : 时间戳
   * @param nonce     : 随机数
   * @param echostr   : 随机字符串
   * @return initial msg
   */
  @GetMapping("/wx/authorize")
  public String authorize(@RequestParam("signature") String signature,
                          @RequestParam("timestamp") Long timestamp,
                          @RequestParam("nonce") String nonce,
                          @RequestParam("echostr") String echostr) {
    logger.info("【signature：{}】", signature);
    logger.info("【timestamp：{}】", timestamp);
    logger.info("【nonce：{}】", nonce);
    logger.info("【echostr：{}】", echostr);
    return echostr;
  }

}