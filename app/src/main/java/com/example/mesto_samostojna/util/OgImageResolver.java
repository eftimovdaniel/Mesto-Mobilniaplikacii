package com.example.mesto_samostojna.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Извлекува „вистинска" слика на бизнисот од website-от, со приоритет:
 *   1. <meta property="og:image" content="..."> (и og:image:secure_url)
 *   2. <meta name="twitter:image" content="...">
 *   3. <link rel="apple-touch-icon" href="..."> (обично 180×180+, висок квалитет)
 *   4. <link rel="icon" href="..."> / <link rel="shortcut icon" ...>
 * Кешира резултати во SharedPreferences (TTL 1 ден ако најден, 30 мин ако не е).
 */
public final class OgImageResolver {

    private static final String TAG = "OgImageResolver";
    private static final String PREFS = "og_image_cache";
    private static final long TTL_HIT_MILLIS = TimeUnit.DAYS.toMillis(1);
    private static final long TTL_MISS_MILLIS = TimeUnit.MINUTES.toMillis(30);
    private static final int MAX_BYTES = 256 * 1024;

    private static final OkHttpClient CLIENT =
            new OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(8, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build();

    private static final Pattern META_IMAGE =
            Pattern.compile(
                    "<meta[^>]+(?:property|name)\\s*=\\s*[\"'](?:og:image(?::secure_url)?|twitter:image(?::src)?)[\"'][^>]*>",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern LINK_APPLE_ICON =
            Pattern.compile(
                    "<link[^>]+rel\\s*=\\s*[\"']apple-touch-icon(?:-precomposed)?[\"'][^>]*>",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern LINK_ICON =
            Pattern.compile(
                    "<link[^>]+rel\\s*=\\s*[\"'](?:shortcut\\s+icon|icon)[\"'][^>]*>",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern CONTENT_ATTR =
            Pattern.compile(
                    "content\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private static final Pattern HREF_ATTR =
            Pattern.compile(
                    "href\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    public interface Callback2 {
        @MainThread
        void onResolved(@Nullable String imageUrl);
    }

    private OgImageResolver() {}

    /** null = немаме информација уште. "" = знаеме дека нема (negative cache). */
    @Nullable
    public static String cachedFor(Context ctx, String website) {
        if (TextUtils.isEmpty(website)) {
            return null;
        }
        SharedPreferences sp = ctx.getApplicationContext().getSharedPreferences(PREFS, 0);
        String key = keyFor(website);
        long ts = sp.getLong(key + ":ts", 0L);
        if (ts == 0) return null;
        String cached = sp.getString(key, null);
        long age = System.currentTimeMillis() - ts;
        long ttl = TextUtils.isEmpty(cached) ? TTL_MISS_MILLIS : TTL_HIT_MILLIS;
        if (age > ttl) return null;
        return cached;
    }

    public static void resolve(Context ctx, String website, Callback2 cb) {
        if (TextUtils.isEmpty(website)) {
            cb.onResolved(null);
            return;
        }
        final String normalizedUrl = normalize(website);
        if (normalizedUrl == null) {
            cb.onResolved(null);
            return;
        }
        final Context app = ctx.getApplicationContext();
        final String key = keyFor(website);

        Request req =
                new Request.Builder()
                        .url(normalizedUrl)
                        .header(
                                "User-Agent",
                                "Mozilla/5.0 (Linux; Android 12) Mesto/1.0")
                        .header("Accept", "text/html,application/xhtml+xml")
                        .build();

        CLIENT.newCall(req)
                .enqueue(
                        new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Log.w(TAG, "fetch failed " + normalizedUrl, e);
                                writeCache(app, key, "");
                                postMain(cb, null);
                            }

                            @Override
                            public void onResponse(Call call, Response response)
                                    throws IOException {
                                String img = null;
                                try (ResponseBody body = response.body()) {
                                    if (response.isSuccessful() && body != null) {
                                        img = parseImage(readPartial(body), normalizedUrl);
                                    }
                                }
                                writeCache(app, key, img == null ? "" : img);
                                Log.d(TAG, "resolved " + normalizedUrl + " -> " + img);
                                postMain(cb, img);
                            }
                        });
    }

    @WorkerThread
    private static String readPartial(ResponseBody body) throws IOException {
        byte[] buf = new byte[MAX_BYTES];
        int total = 0;
        int n;
        var src = body.byteStream();
        while (total < MAX_BYTES && (n = src.read(buf, total, MAX_BYTES - total)) != -1) {
            total += n;
            if (total > 32 * 1024) {
                String s = new String(buf, 0, total, java.nio.charset.StandardCharsets.UTF_8);
                if (META_IMAGE.matcher(s).find()) {
                    return s;
                }
            }
        }
        return new String(buf, 0, total, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Nullable
    private static String parseImage(String html, String baseUrl) {
        // 1. og:image / twitter:image
        Matcher m = META_IMAGE.matcher(html);
        while (m.find()) {
            String tag = m.group();
            Matcher c = CONTENT_ATTR.matcher(tag);
            if (c.find()) {
                String url = absolutize(c.group(1), baseUrl);
                if (url != null) return url;
            }
        }
        // 2. apple-touch-icon
        Matcher a = LINK_APPLE_ICON.matcher(html);
        while (a.find()) {
            String tag = a.group();
            Matcher h = HREF_ATTR.matcher(tag);
            if (h.find()) {
                String url = absolutize(h.group(1), baseUrl);
                if (url != null) return url;
            }
        }
        // 3. <link rel="icon">
        Matcher i = LINK_ICON.matcher(html);
        while (i.find()) {
            String tag = i.group();
            Matcher h = HREF_ATTR.matcher(tag);
            if (h.find()) {
                String url = absolutize(h.group(1), baseUrl);
                if (url != null) return url;
            }
        }
        return null;
    }

    /** Дефолтни placeholder favicon-и што да ги игнорираме. */
    private static boolean isGenericFavicon(String url) {
        String low = url.toLowerCase(java.util.Locale.US);
        return low.endsWith("/vite.svg")
                || low.endsWith("/favicon.ico")
                || low.endsWith("/favicon-16x16.png")
                || low.endsWith("/favicon-32x32.png")
                || low.contains("/wp-includes/images/")
                || low.contains("/wp-content/themes/twenty");
    }

    @Nullable
    private static String absolutize(@Nullable String raw, String baseUrl) {
        if (raw == null || raw.isEmpty()) return null;
        String s = raw.replace("&amp;", "&").trim();
        if (s.startsWith("data:")) return null;
        if (isGenericFavicon(s)) return null;
        if (s.startsWith("//")) return "https:" + s;
        if (s.startsWith("/")) {
            try {
                Uri base = Uri.parse(baseUrl);
                return base.getScheme() + "://" + base.getAuthority() + s;
            } catch (Exception ignored) {
                return null;
            }
        }
        if (s.startsWith("http://") || s.startsWith("https://")) {
            return s;
        }
        // Релативен URL → join со base.
        try {
            Uri base = Uri.parse(baseUrl);
            String basePath = base.getPath() != null ? base.getPath() : "/";
            int slash = basePath.lastIndexOf('/');
            String dir = slash >= 0 ? basePath.substring(0, slash + 1) : "/";
            return base.getScheme() + "://" + base.getAuthority() + dir + s;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void writeCache(Context app, String key, String value) {
        SharedPreferences sp = app.getSharedPreferences(PREFS, 0);
        sp.edit()
                .putString(key, value)
                .putLong(key + ":ts", System.currentTimeMillis())
                .apply();
    }

    private static void postMain(Callback2 cb, @Nullable String url) {
        new android.os.Handler(android.os.Looper.getMainLooper())
                .post(() -> cb.onResolved(url));
    }

    @Nullable
    private static String normalize(String website) {
        try {
            String s = website.trim();
            if (!s.startsWith("http://") && !s.startsWith("https://")) {
                s = "https://" + s;
            }
            Uri u = Uri.parse(s);
            if (u.getHost() == null) return null;
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    private static String keyFor(String website) {
        return "v2:" + website.trim().toLowerCase(java.util.Locale.US);
    }
}
