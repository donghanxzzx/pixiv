package com.dhxz.http;

import static com.dhxz.http.Urls.LOGIN_URL;

import com.dhxz.http.utils.HttpClientUtils;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;

/**
 * @author 10066610
 * @desaciption todo
 */
public class ClientTests {
    
    private final String base_url = "https://www.pixiv.net";
    private HttpClientUtils client;
    @Before
    public void setUp(){
        this.client = new HttpClientUtils(
                RequestConfig.custom().setProxy(new HttpHost("127.0.0.1", 1080)));
    }
    
    @Test
    public void get() throws IOException {
        String result = client.simpleGet(LOGIN_URL);
        String val = Jsoup.parse(result)
                .select("#init-config").val();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(val);
        System.out.println(jsonNode.get("pixivAccount.postKey"));
    }
    
    @Test
    public void login() throws IOException {
        
        client.login("donghanxzzx@163.com","donghan19930926");
    
        HttpResponse rank = client.rank(ModeEnum.DAILY);
        String response = client.parseResponse(rank, "UTF-8");
        Document document = Jsoup.parse(response);
//        System.out.println(document);
        System.out.println(response);
    
    }
    
    @Test
    public void number(){
    
    }
    
}
