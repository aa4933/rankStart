package org.seo.rank.api;


import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.seo.rank.Ranker;
import org.seo.rank.impl.BaiduRanker;
import org.seo.rank.impl.ThreeRanker;
import org.seo.rank.model.Rank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wuilly
 */
@WebServlet(name = "GetRank360", urlPatterns = {"/GetRank360"})
public class GetRank360 extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetArticle.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Ranker RANKER = new ThreeRanker();

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.ñ˜
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {


        long start = System.currentTimeMillis();
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String url = request.getParameter("url");
        String keyword = request.getParameter("keyword");
        String word = new String(keyword.getBytes("iso8859-1"), "UTF-8");

        LOGGER.info("url:" + url);
        LOGGER.info("keyword:" + word);
        Rank rank = new Rank();
        rank.setUrl(url);
        rank.setKeyword(word);
        RANKER.rank(rank);

        //输出json
        try (PrintWriter out = response.getWriter()) {
            String json = MAPPER.writeValueAsString(rank);
            out.println(json);
        }
        //计时
        long cost = System.currentTimeMillis() - start;
        LOGGER.info("GetRank360 耗时 " + cost + " 毫秒");
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>


}