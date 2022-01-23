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
   * @return
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


  /**
   * 获取当前计数
   * @return API response json
   */
  @GetMapping(value = "/api/count")
  ApiResponse get() {
    logger.info("/api/count get request");
    Optional<Counter> counter = counterService.getCounter(1);
    Integer count = 0;
    if (counter.isPresent()) {
      count = counter.get().getCount();
    }

    return ApiResponse.ok(count);
  }


  /**
   * 更新计数，自增或者清零
   * @param request {@link CounterRequest}
   * @return API response json
   */
  @PostMapping(value = "/api/count")
  ApiResponse create(@RequestBody CounterRequest request) {
    logger.info("/api/count post request, action: {}", request.getAction());

    Optional<Counter> curCounter = counterService.getCounter(1);
    if (request.getAction().equals("inc")) {
      Integer count = 1;
      if (curCounter.isPresent()) {
        count += curCounter.get().getCount();
      }
      Counter counter = new Counter();
      counter.setId(1);
      counter.setCount(count);
      counterService.upsertCount(counter);
      return ApiResponse.ok(count);
    } else if (request.getAction().equals("clear")) {
      if (!curCounter.isPresent()) {
        return ApiResponse.ok(0);
      }
      counterService.clearCount(1);
      return ApiResponse.ok(0);
    } else {
      return ApiResponse.error("参数action错误");
    }
  }

}