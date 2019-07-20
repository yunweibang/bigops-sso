/*
 * Copyright 2018 www.yunweibang.com Inc. All rights reserved.
 */
package com.yunweibang.auth.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * @author lpp
 */
public class WeiXinUtils {
    private static final String WEIXIN_SEND_MSG_API = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s";
    private static final String WEIXIN_GET_TOKEN_API = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param accessToken
     * @param msg
     * @return
     * @throws IOException
     */
    public static boolean sendTextMsg(String corpId, String secret, String to, int agentId, String msg)
            throws Exception {
        String accessToken = getAccessToken(corpId, secret);

        URL url = new URL(String.format(WEIXIN_SEND_MSG_API, accessToken));

        HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) url.openConnection();

        httpsUrlConnection.setRequestMethod("POST");
        httpsUrlConnection.setRequestProperty("Content-Type", "application/json");
        httpsUrlConnection.setDoInput(true);
        httpsUrlConnection.setDoOutput(true);

        OutputStreamWriter writer = new OutputStreamWriter(httpsUrlConnection.getOutputStream());

        String sendMsg = "{ \"touser\": \"%s\", \"toparty\": \"\", \"msgtype\": \"text\", \"agentid\": \"%s\", \"text\": { \"content\": \"%s\" }, \"safe\":\"0\" }";
        sendMsg = String.format(sendMsg, to, agentId, msg);
        writer.write(sendMsg);
        writer.flush();

        InputStream in = httpsUrlConnection.getInputStream();
        String response = IOUtils.toString(in, Charset.forName("UTF-8"));
        @SuppressWarnings("unchecked")
        Map<String, Object> respObj = objectMapper.readValue(response, Map.class);

        IOUtils.closeQuietly(in);
        httpsUrlConnection.disconnect();
        return (Integer) respObj.get("errcode") == 0;
    }

    private static String getAccessToken(String corpId, String secret) throws Exception {
        URL url = new URL(String.format(WEIXIN_GET_TOKEN_API, corpId, secret));

        TrustManager[] tm = {new HttpsTrustsManager()};
        SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
        sslContext.init(null, tm, new java.security.SecureRandom());

        // 从上述SSLContext对象中得到SSLSocketFactory对象
        SSLSocketFactory ssf = sslContext.getSocketFactory();

        HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) url.openConnection();
        httpsUrlConnection.setSSLSocketFactory(ssf);

        httpsUrlConnection.setDoInput(true);
        httpsUrlConnection.setRequestMethod("GET");

        InputStream in = httpsUrlConnection.getInputStream();

        String response = IOUtils.toString(in, Charset.forName("UTF-8"));
        @SuppressWarnings("unchecked")
        Map<String, Object> respObj = objectMapper.readValue(response, Map.class);

        IOUtils.closeQuietly(in);
        httpsUrlConnection.disconnect();
        return (String) respObj.get("access_token");
    }

    private static class HttpsTrustsManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

    }
}
