package com.ark.service.apidocs.utils;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http远程调用包
 *
 */
public class HttpClientUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientUtils.class);

    /**
     * 发送HttpPost请求
     *
     * @param strURL 服务地址
     * @param params json字符串,例如: "{ \"id\":\"12345\" }" ;其中属性名必须带双引号<br/>
     * @return 成功:返回json字符串<br/>
     */
    public static String post(String strURL, String params) {
        try {
            // 创建连接
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            // 设置请求方式
            connection.setRequestMethod("POST");
            // 设置接收数据的格式
            connection.setRequestProperty("Accept", "application/json");
            // 设置发送数据的格式
            connection.setRequestProperty("Content-Type", "application/json");
            connection.connect();
            // utf-8编码
            OutputStreamWriter out = new OutputStreamWriter(
                    connection.getOutputStream(), "UTF-8");
            out.append(params);
            out.flush();
            out.close();
            // 读取响应
            int length = connection.getContentLength();
            InputStream is = connection.getInputStream();
            if (length != -1) {
                byte[] data = new byte[length];
                byte[] temp = new byte[512];
                int readLen = 0;
                int destPos = 0;
                while ((readLen = is.read(temp)) > 0) {
                    System.arraycopy(temp, 0, data, destPos, readLen);
                    destPos += readLen;
                }
                // utf-8编码
                String result = new String(data, "UTF-8");
                LOG.info("url:" + strURL + ", params:" + params + ", result:" + result);
                return result;
            }
        } catch (IOException e) {
            LOG.warn("http接口上报失败。url:" + strURL + ", params:" + params + "，errorMsg:" + e.getMessage());
        }
        // 自定义错误信息
        return "error";
    }

}
