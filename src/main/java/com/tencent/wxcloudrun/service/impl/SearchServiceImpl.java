package com.tencent.wxcloudrun.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
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

    static String base_url = "https://www.upyunso.com/";

    static String start_url = "https://www.upyunso.com/search.html?page=1&search_folder_or_file=1&is_search_folder_content=0&is_search_path_title=1&category=all&file_extension=all&search_model=1&keyword=";

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

    public static String getResFromWeb(String keyword) {
        Document document;
        try {
            document = Jsoup.parse(new URL(start_url + keyword), time_out);
        } catch (Exception e) {
            log.error("search error", e);
            return defRes;
        }
        Elements elements = document.select("div.main-info > h1 > a");
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
        Optional<Element> element = elements.stream().filter(a -> a.attr("href").contains("download.html")).findFirst();
        resStr.append(getResUrl(element.get())).append(lineSp).append(lineSp).append("如需更多资源请重试");
        return resStr.toString();
    }

    private static void updateCache(String keyword, Elements elements) throws InterruptedException {
        log.debug("start {}", System.currentTimeMillis());
        Set<String> urlSet = new CopyOnWriteArraySet<>();
        Set<String> nameSet = new CopyOnWriteArraySet<>();
        StringBuilder resStr = new StringBuilder("包含[ " + keyword + " ]的资源：" + lineSp);
        CountDownLatch latch = new CountDownLatch(res_limit);
        for (Element element : elements) {
            if (!element.attr("href").contains("download.html")) {
                continue;
            }
            if (urlSet.size() < res_limit) {
                executorService.submit(() -> {
                    String resUrl = getResUrl(element);
                    if (resUrl.length() > 0 && !nameSet.contains(element.text()) && !urlSet.contains(resUrl) && urlSet.size() < res_limit) {
                        urlSet.add(resUrl);
                        nameSet.add(element.text());
                        resStr.append(resUrl).append(lineSp);
                        latch.countDown();
                    }
                });
                Thread.sleep(400);
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

    public static String getResUrl(Element element) {
        String resId = element.attr("href");
        Connection.Response response;
        try {
            response = Jsoup.connect(base_url + resId).followRedirects(false).method(Connection.Method.GET).execute();
        } catch (IOException e) {
            log.error("get ali yun url error", e);
            return "";
        }
        return response.headers("location").size() == 0 ? "" : element.text() + ": " + response.headers("location").get(0);
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
