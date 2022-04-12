package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.dto.WxXmlData;
import com.tencent.wxcloudrun.service.impl.SearchServiceImpl;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * 自动回复消息控制器
 */
@Slf4j
@RestController
public class aotuReplyController {

    /**
     * 微信成为开发者 接口
     *
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
        log.info("【echostr：{}】", echostr);
        return echostr;
    }

    @PostMapping(value = "/wx/authorize", consumes = {MediaType.TEXT_XML_VALUE}, produces = {MediaType.TEXT_XML_VALUE})
    public String authorize(HttpServletRequest request) {
        WxXmlData msg = new WxXmlData();
        try {
            msg = resolveXmlData(request.getInputStream());
        } catch (IOException e) {
            log.error("parse msg error", e);
        }
        return autoResponse(msg);
    }

    public WxXmlData resolveXmlData(InputStream in) throws IOException {
        StringWriter sw = new StringWriter();
        IOUtils.copy(in, sw, StandardCharsets.UTF_8);
        String xmlData = sw.toString();
        WxXmlData wxXmlData = null;
        try {
            XStream xstream = new XStream();
            xstream.addPermission(AnyTypePermission.ANY);
            //这个必须要加 不然无法转换成WxXmlData对象
            xstream.setClassLoader(WxXmlData.class.getClassLoader());
            xstream.processAnnotations(WxXmlData.class);
            xstream.alias("xml", WxXmlData.class);
            wxXmlData = (WxXmlData) xstream.fromXML(xmlData);
        } catch (Exception e) {
            log.error("【error】{0}", e);
        }
        return wxXmlData;
    }

    public String autoResponse(WxXmlData wxData) {
        WxXmlData resultXmlData = new WxXmlData();
        resultXmlData.setToUserName(wxData.getFromUserName());  //收到的消息是谁发来的再发给谁
        resultXmlData.setFromUserName(wxData.getToUserName());  //
        resultXmlData.setMsgType("text");
        resultXmlData.setCreateTime(System.currentTimeMillis());
        String content = "Sorry, service error please try again later!";
        try {
            log.info("search by {}", wxData);
            content = SearchServiceImpl.searchByKeyword(wxData.getContent());
        } catch (Exception e) {
            log.error("other error", e);
        }

        resultXmlData.setContent(content);
        XStream xstream = new XStream();
        xstream.processAnnotations(WxXmlData.class);
        xstream.setClassLoader(WxXmlData.class.getClassLoader());
        return xstream.toXML(resultXmlData);
    }
}