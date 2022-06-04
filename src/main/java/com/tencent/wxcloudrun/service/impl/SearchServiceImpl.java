package com.tencent.wxcloudrun.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.tencent.wxcloudrun.model.DownResponse;
import com.tencent.wxcloudrun.model.FolderRes;
import com.tencent.wxcloudrun.model.WebResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * search service
 */
@Slf4j
public class SearchServiceImpl {

    static int time_out = 4000;

    static String def_mv = "电影仓：https://www.aliyundrive.com/s/u9BAWCEZqHi/folder/625154d1bd111a6b52bc4dc6bdb4aa8351efcffd";
    static String def_ds = "电视剧仓：https://www.aliyundrive.com/s/rJQ38Ab9Qd7/folder/6210a2fbea04dde2ea53432696ceb6ca4fbe28ec";

    static String defRes = "暂未找到包含关键字的资源，看下资源库，或者稍后重试：" + System.lineSeparator() + def_mv + System.lineSeparator() + def_ds;

    static String base_url = "https://api.upyunso.com/";

    static String start_url = "https://api.upyunso.com/search?page=1&s_type=2&keyword=";

    static int res_limit = 10;

    static String lineSp = System.lineSeparator();

    static ExecutorService executorService = Executors.newCachedThreadPool();

    static Cache<String, String> resCache = CacheBuilder.newBuilder()
            //cache的初始容量
            .initialCapacity(20)
            //cache最大缓存数
            .maximumSize(200)
            //设置写缓存后n秒钟过期
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    public static String searchByKeyword(String keyword) throws InterruptedException {
        // 获取value的值，如果key不存在，调用collable方法获取value值加载到key中再返回
        String res = resCache.getIfPresent(keyword);
        return StringUtils.isEmpty(res) ? getResFromWeb(keyword) : res;

    }

    public static String invokeApi(int invokeType, String keyword) {
        Document document;
        try {
            if (invokeType == 1) {
                String kw = URLEncoder.encode(keyword, "utf-8");
                document = Jsoup.parse(new URL(start_url + kw), time_out);
            } else {
                document = Jsoup.parse(new URL(base_url + keyword), time_out);
            }

        } catch (Exception e) {
            log.error("search error", e);
            return defRes;
        }
        final Base64.Decoder decoder = Base64.getDecoder();
        return new String(decoder.decode(document.text()), StandardCharsets.UTF_8);
    }

    public static String getResFromWeb(String keyword) {

        String decodeStr = invokeApi(1, keyword);
        WebResponse response = new Gson().fromJson(decodeStr, WebResponse.class);
        List<FolderRes> elements = response.getResult().getItems();
        if (elements == null || elements.size() <= 0) {
            return defRes;
        }
        // 多线程异步查找
        executorService.submit(() -> {
            try {
                updateCache(keyword, elements);
            } catch (InterruptedException e) {
                log.error("update resource cache error", e);
            }
        });

        // 先返回第一个
        StringBuilder resStr = new StringBuilder("包含[ " + keyword + " ]的第一个资源：" + lineSp);
        // return top resource
        String downUrl = "";
        for (FolderRes element : elements) {
            if (element.getPage_url().contains("download")) {
                downUrl = element.getPage_url();
                break;
            }
        }
        if (downUrl.length() <= 0) {
            return defRes;
        }
        String resUrl = getResUrl(downUrl);
        if (StringUtil.isBlank(resUrl)) {
            return defRes;
        }
        resStr.append(resUrl).append(lineSp).append(lineSp).append("请再次发送消息获取更多资源");
        return resStr.toString();
    }

    private static void updateCache(String keyword, List<FolderRes> elements) throws InterruptedException {
        log.debug("start {}", System.currentTimeMillis());
        Set<String> urlSet = new CopyOnWriteArraySet<>();
        Set<String> nameSet = new CopyOnWriteArraySet<>();
        StringBuilder resStr = new StringBuilder("包含[ " + keyword + " ]的资源：" + lineSp);
        CountDownLatch latch = new CountDownLatch(res_limit);
        for (FolderRes element : elements) {
            if (urlSet.size() < res_limit) {
                executorService.submit(() -> {
                    String downUrl = element.getPage_url();
                    if (!downUrl.contains("download")) {
                        return;
                    }
                    String resUrl = getResUrl(element.getPage_url());
                    if (resUrl.length() > 0 && !nameSet.contains(element.getPath()) && !urlSet.contains(resUrl) && urlSet.size() < res_limit) {
                        urlSet.add(resUrl);
                        nameSet.add(element.getPath());
                        resStr.append(element.getPath()).append(":").append(resUrl).append(lineSp);
                        latch.countDown();
                    }
                });
                Thread.sleep(300);
            } else {
                break;
            }
        }
        log.debug("threads started {}", System.currentTimeMillis());
        boolean done = latch.await(1000, TimeUnit.MILLISECONDS);
        log.debug("threads end {}", done);
        String res = urlSet.size() > 0 ? resStr.toString() : defRes;
        log.debug("res is {}", res);
        resCache.put(keyword, res);
    }

    private static String getParam(String url) {
        return url.replace(".html", "");
    }

    public static String getResUrl(String resId) {
        String response = invokeApi(2, getParam(resId));
        final DownResponse downResponse = new Gson().fromJson(response, DownResponse.class);
        return downResponse.getResult().getRes_url();
    }

    public static void main(String[] args) throws InterruptedException {
        Document document = null;
        String keyword = "我的祖国";
        try {
            document = Jsoup.parse(new URL(start_url + keyword), time_out);
        } catch (Exception e) {
            log.error("search error", e);
        }
        assert document != null;
        Elements resList = document.select("div.main-info > h1 > a");
    }

}
