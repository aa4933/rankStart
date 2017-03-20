package org.seo.rank.impl;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seo.rank.Ranker;
import org.seo.rank.model.Rank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Created by wuilly on 2017/3/1.
 */
public class ThreeRanker implements Ranker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreeRanker.class);
    private static final String ACCEPT = "text/html, */*; q=0.01";
    private static final String ENCODING = "gzip, deflate";
    private static final String LANGUAGE = "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3";
    private static final String CONNECTION = "keep-alive";
    private static final String HOST = "www.so.com";
    private static final String REFERER = "http://www.so.com";
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
        //校验是否出传入
        if(StringUtils.isBlank(rank.getKeyword()) || StringUtils.isBlank(rank.getUrl())){
            return ;
        }
        String keyword=rank.getKeyword();
        //分页对比网址
        for (int i = 1; i < PAGE; i++) {
            String path = "https://www.so.com/s?q="+keyword+"&pn=" + i;

            LOGGER.debug(path);


            System.out.println(i);
            int r = searchThreeRank(path, rank);

            if (r > 0){
                rank.setRank(r+(i-1)*10);
                //找到排名
                return;
            }
        }
    }

    /**
     * 检查360排名
     * @param url 检查360的URL
     * @param rank 网页排名
     * @return
     */
    private int searchThreeRank(String url, Rank rank) {
        String targetUrl = rank.getUrl();
        try {
            trustEveryone();
            Document document = Jsoup.connect(url) .header("Accept", ACCEPT)
                    .header("Accept-Encoding", ENCODING)
                    .header("Accept-Language", LANGUAGE)
                    .header("Connection", CONNECTION)
                    .header("Host", HOST)
                    .header("Referer", REFERER)
                    .header("User-Agent", USER_AGENT)
                    .get();

            String titleCssQuery = "html body div div ul li h3 a";
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



                String realUrl = element.attr("href");
                String useUrl=rank.getUrl().replace("http://","").replace("/","");



                System.out.println(useUrl);

                LOGGER.debug("url:"+url);
                LOGGER.debug("realUrl:"+realUrl);


                if(realUrl.contains(useUrl)){
                    return i;
                }
            }

        } catch (Exception ex) {
            LOGGER.error("搜索出错",ex);
        }
        return -1;
    }

    /**
     * ssl证书忽略
     */
    public static void trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[] { new X509TrustManager() {

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            } }, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }
}
