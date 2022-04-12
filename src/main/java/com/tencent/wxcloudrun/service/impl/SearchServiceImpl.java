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
import java.util.Set;
import java.util.concurrent.*;

/**
 * search service
 */
@Slf4j
public class SearchServiceImpl {

    static int time_out = 3500;

    static String def_mv = "电影仓：https://www.aliyundrive.com/s/u9BAWCEZqHi/folder/625154d1bd111a6b52bc4dc6bdb4aa8351efcffd";
    static String def_ds = "电视剧仓：https://www.aliyundrive.com/s/rJQ38Ab9Qd7/folder/6210a2fbea04dde2ea53432696ceb6ca4fbe28ec";

    static String base_url = "https://www.upyunso.com/";

    static String start_url = "https://www.upyunso.com/search.html?page=1&search_folder_or_file=0&" +
            "is_search_folder_content=0&is_search_path_title=0&category=video&file_extension=all&search_model=1&keyword=";

    static ExecutorService executorService = Executors.newCachedThreadPool();

    static Cache<String, String> resCache = CacheBuilder.newBuilder()
            //cache的初始容量
            .initialCapacity(20)
            //cache最大缓存数
            .maximumSize(200)
            //设置写缓存后n秒钟过期
            .expireAfterWrite(12, TimeUnit.HOURS)
            .build();

    public static String searchByKeyword(String keyword) throws InterruptedException {
        // 获取value的值，如果key不存在，调用collable方法获取value值加载到key中再返回
        String res = resCache.getIfPresent(keyword);
        return StringUtils.isEmpty(res) ? getResFromWeb(keyword) : res;

    }

    public static String getResFromWeb(String keyword) throws InterruptedException {
        String defRes = "暂未找到包含关键字的资源，看看有没有喜欢的：" + System.lineSeparator() + def_mv + System.lineSeparator() + def_ds;
        Document document;
        try {
            document = Jsoup.parse(new URL(start_url + keyword), time_out);
        } catch (Exception e) {
            log.error("search error", e);
            return defRes;
        }
        Element resList = document.getElementById("res_list");
        if (resList == null) {
            return defRes;
        }
        Elements elements = resList.getElementsByAttributeValueContaining("href", "download.html");

        // return top resource
        Set<String> urlSet = new CopyOnWriteArraySet<>();
        StringBuilder resStr = new StringBuilder("包含[ " + keyword + " ]的资源：\n");
        CountDownLatch latch = new CountDownLatch(5);
        for (Element element : elements) {
            executorService.submit(() -> {
                if (urlSet.size() < 5) {
                    String resUrl = "";
                    try {
                        resUrl = getResUrl(element);
                    } catch (IOException e) {
                        log.error("get ali yun url error", e);
                    }
                    if (resUrl.length() > 0 && !urlSet.contains(resUrl) && urlSet.size() < 5) {
                        urlSet.add(resUrl);
                        resStr.append(element.text()).append(": ").append(resUrl).append("\n");
                        latch.countDown();
                    }
                }
            });
            Thread.sleep(300);
        }
        latch.await(500, TimeUnit.MILLISECONDS);
//        executorService.shutdown();
        String res = urlSet.size() > 0 ? resStr.toString() : defRes;
        resCache.put(keyword, res);
        return res;
    }

    public static String getResUrl(Element element) throws IOException {
        String resId = element.attributes().get("href");
        Connection.Response response = Jsoup.connect(base_url + resId).followRedirects(false).method(Connection.Method.GET).execute();
        return response.headers("location").size() == 0 ? "" : response.headers("location").get(0);
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println(searchByKeyword("山河令"));
    }

}
