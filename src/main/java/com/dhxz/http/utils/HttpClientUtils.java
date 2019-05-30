package com.dhxz.http.utils;

import static com.dhxz.http.Urls.*;

import com.dhxz.http.ModeEnum;
import com.dhxz.http.Urls;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.boot.web.servlet.server.Session.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author 10066610
 * @desaciption todo
 */
@Slf4j
@Data
public class HttpClientUtils implements Serializable {
    
    private static final String DEFAULT_CHARACTER = "UTF-8";
    private static final String HTTPS = "https";
    private RequestConfig requestConfig;
    private StandardHttpRequestRetryHandler standardHttpRequestRetryHandler;
    private CloseableHttpClient client;
    private HttpClientContext context;
    private JsonNode initConfig;
    private String pixivUserId;
    HashMap<String, String> cookie;
    Pattern findUserId = Pattern.compile("pixiv.user.id = \"(\\d+)\";");
    public HttpClientUtils() {
        this.standardHttpRequestRetryHandler = new StandardHttpRequestRetryHandler(3, true);
        this.requestConfig = RequestConfig.custom()
                .setSocketTimeout(5000)
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(2000)
                .setRedirectsEnabled(false)
                .setMaxRedirects(0)
                .build();
        this.client = HttpClientBuilder.create()
                .setRetryHandler(standardHttpRequestRetryHandler)
                .build();
        
    }
    
    public HttpClientUtils(RequestConfig.Builder builder) {
        standardHttpRequestRetryHandler = new StandardHttpRequestRetryHandler(3, true);
        this.client = HttpClientBuilder.create()
                .setRetryHandler(standardHttpRequestRetryHandler)
                .build();
        this.requestConfig = builder.setSocketTimeout(5000)
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(2000)
                .setRedirectsEnabled(false)
                .setMaxRedirects(0)
                .build();
    }
    
    public String simpleGet(String url,Map<String,String> headers){
        return parseResponse(send(url,null,headers,null,null,null,HttpMethod.GET),DEFAULT_CHARACTER);
    }
    
    public String simpleGet(String url) {
        return parseResponse(send(url, null, null, null, null, null, HttpMethod.GET),
                DEFAULT_CHARACTER);
    }
    
    public void initConfig() throws IOException {
        String val = Jsoup.parse(simpleGet(LOGIN_URL))
                .select("#init-config").val();
        ObjectMapper mapper = new ObjectMapper();
        this.initConfig = mapper.readTree(val);
    }
    
    public void login(String username, String password) throws IOException {
        context = HttpClientContext.create();
        
        initConfig();
        
        Map<String, String> form = Maps.newHashMap();
        
        form.put("pixiv_id", username);
        form.put("password", password);
        form.put("captcha", "");
        form.put("g_recaptcha_response", "");
        form.put("return_to", "");
        form.put("lang", "en");
        form.put("post_key", initConfig.get("pixivAccount.postKey").textValue());
        form.put("source", "accounts");
        form.put("ref", "");
        HttpResponse response = loginPost(LOGIN_API, form, context);
        cookie = new HashMap<>();
        if (response.getStatusLine().getStatusCode() == HttpStatus.OK.value()){
            //login success
            // parse cookie
            Header[] headers = response.getHeaders(HttpHeaders.SET_COOKIE);
            for (Header header : headers) {
                if (header.getValue().contains("PHPSESSID")){
                    cookie.put(HttpHeaders.SET_COOKIE,header.getValue());
                }
            }
        }
        String index = simpleGet(INDEX, cookie);
        Matcher matcher = findUserId.matcher(index);
    
       if (matcher.find()){
           String result = matcher.group();
           pixivUserId = result.replaceAll("\\D","");
       }
    }
    
    public String simplePost(String url, Map<String, String> form) {
        return parseResponse(send(url, null, null, form, null, null, HttpMethod.POST),
                DEFAULT_CHARACTER);
    }
    
    public String parseResponse(HttpResponse httpResponse, String resultCharacter) {
        try {
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.OK.value()) {
                BufferedHttpEntity bhe = new BufferedHttpEntity(httpResponse.getEntity());
                if (StringUtils.isEmpty(resultCharacter)) {
                    resultCharacter = DEFAULT_CHARACTER;
                }
                return EntityUtils.toString(bhe, resultCharacter);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public HttpResponse loginPost(String url, Map<String, String> formParameters,
            HttpClientContext context) {
        HttpPost post = new HttpPost(url);
        post.setConfig(requestConfig);
        HttpResponse response = null;
        try {
            
            List<BasicNameValuePair> pairList = formParameters.keySet()
                    .stream()
                    .map(key -> new BasicNameValuePair(key, formParameters.get(key)))
                    .collect(Collectors.toList());
            post.setEntity(new UrlEncodedFormEntity(pairList, DEFAULT_CHARACTER));
            response = client.execute(post);
            log.info("request url:\t" + url + ";\tresponse status:\t" + response.getStatusLine());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return response;
    }
    
    public HttpResponse send(final String url, String content, Map<String, String> headers,
            Map<String, String> formParameters, String contentCharacter, String resultCharacter,
            HttpMethod method) {
        if (StringUtils.isEmpty(contentCharacter)) {
            contentCharacter = DEFAULT_CHARACTER;
        }
        
        // auto close
        try {
            
            if (url.toLowerCase().startsWith(HTTPS)) {
                initSSL(client, getPort(url));
            }
            
            HttpResponse httpResponse = null;
            switch (method) {
                case GET:
                    HttpGet get = new HttpGet(url);
                    get.setConfig(requestConfig);
                    addHeader(get, headers);
                    httpResponse = client.execute(get);
                    break;
                case POST:
                    HttpPost post = new HttpPost(url);
                    post.setConfig(requestConfig);
                    addHeader(post, headers);
                    if (CollectionUtils.isEmpty(formParameters)) {
                        post.setEntity(new StringEntity(content, contentCharacter));
                    } else {
                        List<BasicNameValuePair> pairList = formParameters.keySet()
                                .stream()
                                .map(key -> new BasicNameValuePair(key, formParameters.get(key)))
                                .collect(Collectors.toList());
                        post.setEntity(new UrlEncodedFormEntity(pairList, DEFAULT_CHARACTER));
                    }
                    httpResponse = client.execute(post);
                    break;
                case DELETE:
                    HttpDelete delete = new HttpDelete(url
                    );
                    delete.setConfig(requestConfig);
                    addHeader(delete, headers);
                    httpResponse = client.execute(delete);
                    break;
                case PUT:
                    HttpPut put = new HttpPut(url);
                    put.setConfig(requestConfig);
                    addHeader(put, headers);
                    put.setEntity(new StringEntity(content, contentCharacter));
                    httpResponse = client.execute(put);
                    break;
                case PATCH:
                    HttpPatch patch = new HttpPatch(url);
                    patch.setConfig(requestConfig);
                    addHeader(patch, headers);
                    patch.setEntity(new StringEntity(content, contentCharacter));
                    httpResponse = client.execute(patch);
                    break;
                default:
                    throw new UnsupportedOperationException("不支持的请求方式");
            }
            
            return httpResponse;
        } catch (ClientProtocolException e) {
            log.error("protocol error", e);
        } catch (IOException e) {
            log.error("Network error", e);
        }
        return null;
    }
    
    private static void addHeader(HttpRequestBase request, Map<String, String> headers) {
        if (!CollectionUtils.isEmpty(headers)) {
            headers.keySet()
                    .forEach(key -> request.addHeader(key, headers.get(key)));
        }
    }
    
    
    private static void initSSL(CloseableHttpClient httpClient, int port) {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            final X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                        throws CertificateException {
                    
                }
                
                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                        throws CertificateException {
                    
                }
                
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            SSLConnectionSocketFactory ssf = new SSLConnectionSocketFactory(
                    sslContext);
            
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", ssf).build();
            BasicHttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(
                    registry);
            HttpClients.custom().setConnectionManager(ccm).build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
    }
    
    private static int getPort(String url) {
        int startIndex = url.indexOf("://") + "://".length();
        String host = url.substring(startIndex);
        if (host.indexOf("/") != -1) {
            host = host.substring(0, host.indexOf("/"));
        }
        
        int port = 443;
        if (host.contains(":")) {
            int i = host.indexOf(":");
            port = Integer.parseInt(host.substring(i + 1));
        }
        
        return port;
    }
    
    public HttpResponse rank(ModeEnum daily) {
        
        return send(RANK + "?" + daily.getValue(),null,cookie,null,null,null,HttpMethod.GET);
    }
}
