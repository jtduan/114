package code.jtduan.ticket.util;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by djt on 12/24/16.
 */

public class OKHttpUtil {
    private static Logger logger = LoggerFactory.getLogger(OKHttpUtil.class);

    public static OkHttpClient client = HttpsUtil.getUnsafeOkHttpClient().newBuilder().cookieJar(new MyCookieJar()).build();

    public static void clearCookie() {
        client = HttpsUtil.getUnsafeOkHttpClient().newBuilder().cookieJar(new MyCookieJar()).build();
    }


    public static String sendPost(String url, String params) {
        return sendPost(url, params, 2);
    }

    public static String sendPost(String url, String params, int num) {
        logger.debug(url + ":" + params);
        RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=utf-8"), params);
        Request request = new Request.Builder()
                .url(url).post(body)
                .build();

        return send(request, num);
    }

    public static String sendGet(String url, String params) {
        if (!params.isEmpty()) {
            url = url + "?" + params;
        }
        Request request = new Request.Builder()
                .url(url).get()
                .build();
        return send(request, 2);
    }

    private static String send(Request request, int num) {
        try {
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            if (num > 1) {
                return send(request, num - 1);
            } else {
                logger.debug(e.getMessage());
                return "";
            }
        }
    }

    public static byte[] getAndResponse(String url, int times) {
        Request request = new Request.Builder()
                .url(url).get()
                .build();
        try{
            return client.newCall(request).execute().body().bytes();
        } catch (IOException e) {
            if (times > 1) {
                return getAndResponse(url, times - 1);
            }else{
                logger.debug(e.getMessage());
                return new byte[0];
            }
        }
    }

    static class MyCookieJar implements CookieJar {

        private List<Cookie> cookies;

        MyCookieJar() {
            cookies = new ArrayList<>();
        }

        @Override
        public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
            this.cookies.addAll(list);
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl httpUrl) {
            if (cookies == null) return Collections.emptyList();
            return cookies;
        }

    }
}
