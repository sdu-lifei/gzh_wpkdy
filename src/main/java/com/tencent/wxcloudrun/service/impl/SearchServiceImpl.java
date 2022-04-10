package com.tencent.wxcloudrun.service.impl;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * search service
 */
public class SearchServiceImpl {

    static int time_out = 4000;

    static String def_mv = "电影仓：https://www.aliyundrive.com/s/u9BAWCEZqHi/folder/625154d1bd111a6b52bc4dc6bdb4aa8351efcffd";
    static String def_ds = "电视剧仓：https://www.aliyundrive.com/s/rJQ38Ab9Qd7/folder/6210a2fbea04dde2ea53432696ceb6ca4fbe28ec";

    static String base_url = "https://www.upyunso.com/";

    static String start_url = "https://www.upyunso.com/search.html?page=1&search_folder_or_file=0&" +
            "is_search_folder_content=0&is_search_path_title=0&category=video&file_extension=all&search_model=1&keyword=";


    public static String searchByKeyword(String keyword) throws IOException {

        String defRes = def_mv + System.lineSeparator() + def_ds;

        Document document = Jsoup.parse(new URL(start_url + keyword), time_out);
        Element resList = document.getElementById("res_list");
        if (resList == null) {
            return defRes;
        }
        Elements elements = resList.getElementsByAttributeValueContaining("href", "download.html");

        // return top resource
        Set<String> urlSet = new HashSet<>();
        StringBuilder resStr = new StringBuilder("包含[ " + keyword + " ]的资源：\n");
        for (int i = 0; i < elements.size() && urlSet.size() < 5; i++) {
            Element element = elements.get(i);
            String resUrl = getResUrl(element);
            if (resUrl.length() > 0 && !urlSet.contains(resUrl)) {
                urlSet.add(resUrl);
                resStr.append(element.text()).append(": ").append(resUrl).append("\n");
            }
        }

        return urlSet.size() > 0 ? resStr.toString() : defRes;
    }

    public static String getResUrl(Element element) throws IOException {
        String resId = element.attributes().get("href");
        Connection.Response response = Jsoup.connect(base_url + resId).followRedirects(false).method(Connection.Method.GET).execute();
        return response.headers("location").get(0);
    }

    public static void main(String[] args) throws IOException {
        System.out.println(searchByKeyword("超越333"));
    }

}
