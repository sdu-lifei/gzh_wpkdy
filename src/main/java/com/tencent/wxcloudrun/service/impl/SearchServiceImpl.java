package com.tencent.wxcloudrun.service.impl;

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

    static int time_out = 30000;

    static String base_url = "https://www.upyunso.com/";

    static String start_url = "https://www.upyunso.com/search.html?page=1&search_folder_or_file=0&" +
            "is_search_folder_content=0&is_search_path_title=0&category=video&file_extension=all&search_model=1&keyword=";


    public static String searchByKeyword(String keyword) throws IOException {

        Document document = Jsoup.parse(new URL(start_url + keyword), time_out);
        Element resList = document.getElementById("res_list");
        if (resList == null) {
            return "Sorry, can not find the resource by " + keyword;
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

        return urlSet.size() > 0 ? resStr.toString() : "Sorry, can not find the resource by " + keyword;
    }

    public static String getResUrl(Element element) throws IOException {
        String resId = element.attributes().get("href");
        Document resDoc = Jsoup.parse(new URL(base_url + resId), time_out);
        if (resDoc == null || resDoc.getElementsByAttributeValueContaining("href", "aliyundrive.com").size() == 0) {
            return "";
        }
//        System.out.println(resDoc.getElementsByAttributeValueContaining("href", "aliyundrive.com"));
        return resDoc.getElementsByAttributeValueContaining("href", "aliyundrive.com").get(0).attributes().get("href");
    }

    public static void main(String[] args) throws IOException {
        System.out.println(searchByKeyword("超越2022"));
    }

}
