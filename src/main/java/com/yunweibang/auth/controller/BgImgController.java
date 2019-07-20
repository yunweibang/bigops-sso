package com.yunweibang.auth.controller;

import com.yunweibang.auth.model.ImageData;
import com.yunweibang.auth.service.UserService;
import com.yunweibang.auth.utils.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.*;

@Controller
@RequestMapping("/image")
public class BgImgController {

    private static Logger logger = LoggerFactory.getLogger(UserService.class);

    @RequestMapping(value = "/bg.jpg", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<byte[]> viewLoginImage() throws Exception {
        String sql = "select content_type,data from bg_img where type='login' limit 1 ";
        ImageData imageData = getBlob(sql);
        return ResponseEntity.ok().header("Content-Type", imageData.getContentType()).body(imageData.getData());
    }

    public ImageData getBlob(String SQL) throws Exception {

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getCon();
            stmt = conn.prepareStatement(SQL);
            rs = stmt.executeQuery();
            if (rs.next()) {
                String contentType = rs.getString("content_type");
                Blob bb = rs.getBlob("data");
                byte[] data = bb.getBytes(1, (int) bb.length());
                ImageData img = new ImageData();
                img.setContentType(contentType);
                img.setData(data);
                return img;
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        } finally {
            conn.close();
            stmt.close();
        }
        return null;
    }

    public Connection getCon() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Connection con = DriverManager.getConnection(JdbcUtils.getDataSourceUrl(), JdbcUtils.getUserName(), JdbcUtils.getPassWord());
        return con;
    }

}
