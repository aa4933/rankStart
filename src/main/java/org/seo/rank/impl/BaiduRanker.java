/**
 * 
 * APDPlat - Application Product Development Platform
 * Copyright (c) 2013, 杨尚川, yang-shangchuan@qq.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package org.seo.rank.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seo.rank.Ranker;
import org.seo.rank.tools.DynamicIp;
import org.seo.rank.list.UrlTools;
import org.seo.rank.model.Rank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 判断网页是否被搜索引擎收录以及收录之后的排名情况
 * @author 杨尚川
 */
public class BaiduRanker implements Ranker{
    private static final Logger LOGGER = LoggerFactory.getLogger(BaiduRanker.class);
    private static final String ACCEPT = "text/html, */*; q=0.01";
    private static final String ENCODING = "gzip, deflate";
    private static final String LANGUAGE = "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3";
    private static final String CONNECTION = "keep-alive";
    private static final String HOST = "www.baidu.com";
    private static final String REFERER = "http://www.baidu.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:31.0) Gecko/20100101 Firefox/31.0";
    
    //获取多少页
    private static final int PAGE = 15;
    private static final int PAGESIZE = 10;

    @Override
    public void rank(List<Rank> ranks) {
        for(Rank rank : ranks){
            rank(rank);
        }
    }
    @Override
    public void rank(Rank rank){
        doRank(rank);
    }
    /**
     * 查询网页在百度的排名
     * @param rank 排名数据结构
     */
    public void doRank(Rank rank){
        if(StringUtils.isBlank(rank.getKeyword()) || StringUtils.isBlank(rank.getUrl())){
            return ;
        }
        //检查是否被百度收录
        searchBaiduIndex(rank);

        if(!rank.isIndex()){
            return;
        }
        //检查百度排名
        String query = null;
        try {
            query = URLEncoder.encode(rank.getKeyword(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("url构造失败", e);
            return ;
        }

        //过滤空
        if(StringUtils.isBlank(query)){
            return ;
        }

        //分页对比网址
        for (int i = 0; i < PAGE; i++) {
            String path = "http://www.baidu.com/s?tn=monline_5_dg&ie=utf-8&wd=" + query+"&oq="+query+"&usm=3&f=8&bs="+query+"&rsv_bp=1&rsv_sug3=1&rsv_sug4=141&rsv_sug1=1&rsv_sug=1&pn=" + i * PAGESIZE;

            LOGGER.debug(path);

            int r = searchBaiduRank(path, rank);

            if (r > 0){
                rank.setRank(r+i*10);
                //找到排名
                return;
            }
        }
    }
    /**
     * 检查百度是否收录
     * @param rank 
     */
    private void searchBaiduIndex(Rank rank) {
        String url = rank.getUrl();
        url = "http://www.baidu.com/s?wd=" + url;
        LOGGER.debug(url);

        try {
            Document document = Jsoup.connect(url)
                    .header("Accept", ACCEPT)
                    .header("Accept-Encoding", ENCODING)
                    .header("Accept-Language", LANGUAGE)
                    .header("Connection", CONNECTION)
                    .header("User-Agent", USER_AGENT)
                    .header("Host", HOST)
                    .get();

            String notFoundCssQuery = "html body div div div div div p";
            Elements elements = document.select(notFoundCssQuery);
            for(Element element : elements){
                String text = element.text();
                if(text.contains("抱歉，没有找到与") && text.contains("相关的网页。")){
                    //未被百度收录
                    LOGGER.debug("未被百度收录");
                    rank.setIndex(false);
                    return;
                }
            }
            //被收录
                    rank.setIndex(true);
                    return;

        } catch (IOException ex) {
            LOGGER.error("搜索出错",ex);
        }
        LOGGER.debug("未被百度收录");
    }
    /**
     * 检查百度排名
     * @param url 检查百度的URL
     * @param rank 网页排名
     * @return 
     */
    private int searchBaiduRank(String url, Rank rank) {
        String targetUrl = rank.getUrl();
        try {
            Document document = Jsoup.connect(url)
                    .header("Accept", ACCEPT)
                    .header("Accept-Encoding", ENCODING)
                    .header("Accept-Language", LANGUAGE)
                    .header("Connection", CONNECTION)
                    .header("Host", HOST)
                    .header("Referer", REFERER)
                    .header("User-Agent", USER_AGENT)
                    .get();
            String titleCssQuery = "html body div div div div div h3.t a";
            Elements elements = document.select(titleCssQuery);
            //排名
            int i=0;
            for(Element element : elements){
                String title = element.text();

                //过滤空的
                if(StringUtils.isBlank(title)){
                    continue;
                }

                i++;
                LOGGER.debug(i+":"+title);


                //此段代码有争议，理论和实际不符合，执行会造成全部跳过
               /*if(title.contains(rank.getKeyword())){
                    LOGGER.debug("搜索结果标题不包括关键词，忽略");
                    continue;
                }*/


                String href = element.attr("href");


                href = UrlTools.normalizeUrl(url, href);
                String realUrl = urlConvert(href);

                if (realUrl.contains("www.baidu.com")){
                    i--;
                    LOGGER.debug("忽略广告");
                    continue;
                }
                System.out.println(realUrl);
                LOGGER.debug("url:"+url);
                LOGGER.debug("realUrl:"+realUrl);
                LOGGER.debug("targetUrl:"+targetUrl);


                if(targetUrl.equals(realUrl)){

                    return i;
                }
            }
        } catch (Exception ex) {
            LOGGER.error("搜索出错",ex);
        }
        return -1;
    }
    /**
     * 将百度的链接转换为网页的链接
     * @param url 百度链接
     * @return 网页链接
     */
    private static String urlConvert(String url){
        try{
            if(!url.startsWith("http://www.baidu.com/link?url=")){
                //不需要转换URL
                return url;
            }
            LOGGER.debug("转换前的URL："+url);
            Connection.Response response = getResponse(url);
            //这里要处理爬虫限制
            if(response==null || response.body().contains("请您点击按钮解除封锁")
                    || response.body().contains("请输入以下验证码")){
                //使用新的IP地址
                DynamicIp.toNewIp();
                response = getResponse(url);
            }
            String realUrl = response.header("Location");
            LOGGER.debug("转换后的URL："+realUrl);
            //检查网页是否被重定向
            //这个检查会导致速度有点慢
            //这个检测基本没有必要，除非是那种极其特殊的网站，ITEYE曾经就是，后来在我的建议下改进了
            /*
            LOGGER.debug("检查是否有重定向："+realUrl);
            Connection.Response response = getResponse(realUrl);
            //这里要处理爬虫限制
            if(response==null || response.body().contains("请您点击按钮解除封锁")
                              || response.body().contains("请输入以下验证码")){
                //使用新的IP地址
                DynamicIp.toNewIp();
                response = getResponse(realUrl);
            }
            String realUrl2 = response.header("Location");
            if(!StringUtils.isBlank(realUrl2)){
                LOGGER.debug("检查到重定向到："+realUrl2);
                return realUrl2;
            }
            */
            return realUrl;
        }catch(Exception e){
            LOGGER.error("URL转换异常", e);
        }
        return url;
    }
    private static Connection.Response getResponse(String url) {
        try{
            Connection.Response response = Jsoup.connect(url)
                    .header("Accept", ACCEPT)
                    .header("Accept-Encoding", ENCODING)
                    .header("Accept-Language", LANGUAGE)
                    .header("Connection", CONNECTION)
                    .header("Host", HOST)
                    .header("Referer", REFERER)
                    .header("User-Agent", USER_AGENT)
                    .ignoreContentType(true)
                    .timeout(30000)
                    .followRedirects(false)
                    .execute();
            return response;
        } catch (Exception e){
            LOGGER.debug("获取页面失败：", e);
        }
        return null;
    }
}
