package com.lei2j.sms.bomb.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The new HTTP utils
 * @author leijinjun
 * @date 2019/9/17
 * @version V1.0
 **/
public class HttpUtils {

    private static final CloseableHttpClient CLIENT;

    private static final BasicResponseHandler BASIC_RESPONSE_HANDLER = new BasicResponseHandler();

    static {
        try {
            SSLContextBuilder sslContextBuilder = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build(),
                    new String[]{"TLSv1.2", "TLSv1.1"}, null, NoopHostnameVerifier.INSTANCE);
            CLIENT = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 文件上传
     */
    public static class MultiPart {

        private String name;

        private String fileName;

        private String contentType;

        private InputStream inputStream;

        private Map<String, String> extendField;

        private MultiPart() {
        }

        private MultiPart(String name,
                          String fileName,
                          String contentType,
                          InputStream inputStream,
                          Map<String, String> extendField) {
            this.name = name;
            this.fileName = fileName;
            this.contentType = contentType;
            this.inputStream = inputStream;
            this.extendField = extendField;
        }

        public String getName() {
            return name;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public Map<String, String> getExtendField() {
            return extendField;
        }

        public static Builder create() {
            return new Builder();
        }

        public static class Builder {

            private String name;

            private String fileName;

            private String contentType;

            private InputStream inputStream;

            private final Map<String, String> extendField = new HashMap<>(0);

            public Builder setName(String name) {
                this.name = name;
                return this;
            }

            public Builder setFileName(String fileName) {
                this.fileName = fileName;
                return this;
            }

            public Builder setContentType(String contentType) {
                this.contentType = contentType;
                return this;
            }

            public Builder setInputStream(InputStream in) {
                this.inputStream = in;
                return this;
            }

            public Builder addExtendField(String name, String text) {
                this.extendField.put(name, text);
                return this;
            }

            public MultiPart build() {
                return new MultiPart(name, fileName, contentType, inputStream, extendField);
            }
        }
    }

    /**
     * @param url url
     * @param requestTimeout 从连接管理器获取连接的超时时间
     * @param connectTimeout 建立连接的超时时间
     * @param socketTimeout 读取响应两个数据包最大间隔时间
     * @param headers 请求头 if {@code null}, do nothing
     * @return String
     * @throws IOException i/o error
     */
    public static String get(String url,
                             int requestTimeout,
                             int connectTimeout,
                             int socketTimeout,
                             Map<String, String> headers) throws IOException {
        Objects.requireNonNull(url, "url is null");
        HttpGet httpGet = (HttpGet) createHttpRequest(url, HttpGet.METHOD_NAME, headers,
                requestTimeout, connectTimeout, socketTimeout);
        try (CloseableHttpResponse response = CLIENT.execute(httpGet)) {
            return BASIC_RESPONSE_HANDLER.handleResponse(response);
        }
    }

    public static String get(String url, int socketTimeout, Map<String, String> headers) throws IOException {
        return get(url, 1000 * 15, 1000 * 15, socketTimeout, headers);
    }

    public static String get(String url, int socketTimeout) throws IOException {
        return get(url, socketTimeout, null);
    }

    public static String get(String url, Map<String, String> headers) throws IOException {
        return get(url, 1000 * 60, headers);
    }

    public static String get(String url) throws IOException {
        return get(url, (Map<String, String>) null);
    }

    public static <T> T get(String url, Class<T> cls, Map<String,String> headers) throws IOException {
        String response = get(url, headers);
        return JSONObject.parseObject(response, cls);
    }

    public static <T> T get(String url, Class<T> cls) throws IOException {
        return get(url, cls, null);
    }

    public static String get(String url, Map<String, String> paramMap, Map<String, String> headerMap) throws IOException {
        String requestURL = url;
        if (paramMap != null && !paramMap.isEmpty()) {
            StringJoiner params = new StringJoiner("&");
            for (Map.Entry<String, String> entry : paramMap.entrySet()) {
                params.add(entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
            }
            requestURL = requestURL + "?" + params.toString();
        }
        return get(requestURL, headerMap);
    }

    public static InputStream getStreaming(String url, Map<String, String> headers) throws IOException {
        Objects.requireNonNull(url, "url is null");
        HttpGet httpGet = (HttpGet) createHttpRequest(url, HttpGet.METHOD_NAME, headers, 0, 0, 0);
        CloseableHttpResponse execute = CLIENT.execute(httpGet);
        HttpEntity entity = execute.getEntity();
        assert entity.isStreaming();
        return entity.getContent();
    }

    public static Map<String, List<HeaderElement[]>> getHeaders(String url) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response = CLIENT.execute(httpGet);
        return Arrays.stream(response.getAllHeaders()).collect(Collectors.groupingBy(Header::getName, Collectors.mapping(Header::getElements, Collectors.toList())));
    }

    public static List<HeaderElement[]> getHeader(String url, String name) throws IOException {
        if (name == null) {
            return null;
        }
        Map<String, List<HeaderElement[]>> headerMap = getHeaders(url);
        return headerMap.get(name);
    }

    public static String post(String url,
                              String json,
                              int requestTimeout,
                              int connectTimeout,
                              int socketTimeout,
                              Map<String, String> headers) throws IOException {
        HttpPost httpPost = (HttpPost) createHttpRequest(url, HttpPost.METHOD_NAME, headers,
                requestTimeout, connectTimeout, socketTimeout);
        httpPost.addHeader("Content-Type", "application/json");
        if (json != null && !json.isEmpty()) {
            httpPost.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
        }
        try (CloseableHttpResponse execute = CLIENT.execute(httpPost)) {
            return BASIC_RESPONSE_HANDLER.handleResponse(execute);
        }
    }

    public static String post(String url, String json, int socketTimeout, Map<String, String> headers) throws IOException {
        return post(url, json, 1000 * 15, 1000 * 15, socketTimeout, headers);
    }

    public static String post(String url, String json, int socketTimeout) throws IOException {
        return post(url, json, socketTimeout, null);
    }

    public static String post(String url, String json, Map<String, String> headers) throws IOException {
        return post(url, json, 1000 * 60, headers);
    }

    public static String post(String url, String json) throws IOException {
        return post(url, json, (Map<String, String>) null);
    }

    public static <T> T post(String url, String json, Class<T> cls, Map<String, String> headers) throws IOException {
        return JSONObject.parseObject(post(url, json, headers), cls);
    }

    public static <T> T post(String url, String json, Class<T> cls) throws IOException {
        return JSONObject.parseObject(post(url, json), cls);
    }

    public static String postWithFormData(String url,
                                   MultiPart multipartFile,
                                   int requestTimeout,
                                   int connectTimeout,
                                   int socketTimeout) throws IOException {
        HttpPost httpPost = (HttpPost) createHttpRequest(url, HttpPost.METHOD_NAME, null,
                requestTimeout, connectTimeout, socketTimeout);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .addBinaryBody(multipartFile.getName(), multipartFile.getInputStream(), ContentType.create(multipartFile.getContentType()), multipartFile.getFileName())
                .setContentType(ContentType.MULTIPART_FORM_DATA)
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        Map<String, String> extendField = multipartFile.getExtendField();
        if (extendField != null) {
            for (Map.Entry<String, String> entry : extendField.entrySet()) {
                builder.addTextBody(entry.getKey(), entry.getValue());
            }
        }
        httpPost.setEntity(builder.build());
        try (CloseableHttpResponse execute = CLIENT.execute(httpPost)) {
            return BASIC_RESPONSE_HANDLER.handleResponse(execute);
        }
    }

    public static String postWithFormData(String url, MultiPart multipart, int socketTimeout) throws IOException {
        return postWithFormData(url, multipart, 1000 * 15, 1000 * 15, socketTimeout);
    }

    public static String postWithFormData(String url, MultiPart multipart) throws IOException {
        return postWithFormData(url, multipart, 1000 * 60);
    }

    public static <T> T postWithFormData(String url, MultiPart multipart, Class<T> cls) throws IOException {
        return JSONObject.parseObject(postWithFormData(url, multipart), cls);
    }

    public static String postForUpload(String url,
                                InputStream in,
                                int requestTimeout,
                                int connectTimeout,
                                int socketTimeout) throws IOException {
        HttpPost httpPost = (HttpPost) createHttpRequest(url, HttpPost.METHOD_NAME, null,
                requestTimeout, connectTimeout, socketTimeout);
        httpPost.addHeader("Content-Type", "application/octet-stream");
        httpPost.setEntity(new InputStreamEntity(in));
        CloseableHttpResponse execute = CLIENT.execute(httpPost);
        return BASIC_RESPONSE_HANDLER.handleResponse(execute);
    }

    public static String postForUpload(String url, InputStream in) throws IOException {
        return postForUpload(url, in, 1000 * 15, 1000 * 15, 1000 * 60);
    }


    public static String postWithXml(String url,String xml) throws IOException {
        HttpPost httpPost = (HttpPost) createHttpRequest(url, HttpPost.METHOD_NAME, null,
                1000 * 15, 1000 * 15, 1000 * 60);
        httpPost.addHeader(new BasicHeader("Content-Type", "text/xml"));
        httpPost.setEntity(new StringEntity(xml, StandardCharsets.UTF_8));
        CloseableHttpResponse execute = CLIENT.execute(httpPost);
        return BASIC_RESPONSE_HANDLER.handleResponse(execute);
    }

    public static String postFormUrlencoded(String url, Map<String, String> params) throws IOException {
        return postFormUrlencoded(url, params, null);
    }

    public static String postFormUrlencoded(String url, Map<String, String> params,Map<String,String> headers) throws IOException {
        HttpPost httpPost = (HttpPost) createHttpRequest(url, HttpPost.METHOD_NAME, headers,
                1000 * 15, 1000 * 15, 1000 * 60);
        httpPost.addHeader(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
        if (params != null) {
            List<BasicNameValuePair> pairList =
                    params.entrySet().stream().map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue())).collect(Collectors.toList());
            httpPost.setEntity(new UrlEncodedFormEntity(pairList, StandardCharsets.UTF_8));
        }
        CloseableHttpResponse execute = CLIENT.execute(httpPost);
        return BASIC_RESPONSE_HANDLER.handleResponse(execute);
    }

    private static HttpRequestBase createHttpRequest(String url,String method,Map<String,String> headerMap,
                                              int connectionRequestTimeout, int connectionTimeout, int socketTimeout) {
        HttpRequestBase requestBase;
        switch (method) {
            case HttpPost.METHOD_NAME:
                requestBase = new HttpPost(url);
                break;
            case HttpGet.METHOD_NAME:
                requestBase = new HttpGet(url);
                break;
            case HttpPut.METHOD_NAME:
                requestBase = new HttpPut(url);
                break;
            case HttpDelete.METHOD_NAME:
                requestBase = new HttpDelete(url);
                break;
            default:
                throw new UnsupportedOperationException(method);
        }
        if (headerMap != null) {
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                requestBase.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
            }
        }
        requestBase.setConfig(RequestConfig.custom()
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .setConnectTimeout(connectionTimeout)
                .setSocketTimeout(socketTimeout)
                .build());
        return requestBase;
    }
}
