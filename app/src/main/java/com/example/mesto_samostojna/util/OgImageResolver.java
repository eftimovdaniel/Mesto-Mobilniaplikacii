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
 * Извлекува „вистинска" слика на бизнисот од неговиот website преку OpenGraph (`og:image`)
 * или Twitter `twitter:image` мета-таг. За Instagram профил → ja враќа profile picture.
 * Кешира резултати во SharedPreferences (TTL 1 ден).
 */
public final class OgImageResolver {

    private static final String TAG = "OgImageResolver";
    private static final String PREFS = "og_image_cache";
    private static final long TTL_MILLIS = TimeUnit.DAYS.toMillis(1);
    private static final int MAX_BYTES = 256 * 1024; // повеќето head таго се во првите ~50KB

    private static final OkHttpClient CLIENT =
            new OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(8, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build();

    // <meta property="og:image" content="..."> или <meta name="twitter:image" content="...">
    private static final Pattern META_IMAGE =
            Pattern.compile(
                    "<meta[^>]+(?:property|name)\\s*=\\s*[\"'](?:og:image(?::secure_url)?|twitter:image)[\"'][^>]*>",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT_ATTR =
            Pattern.compile(
                    "content\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    public interface Callback2 {
        /** Се повикува на main thread. URL може да биде null ако не е најден. */
        @MainThread
        void onResolved(@Nullable String imageUrl);
    }

    private OgImageResolver() {}

    /** Брза проверка во keш (sync). null = немаме информација уште. "" = знаеме дека нема. */
    @Nullable
    public static String cachedFor(Context ctx, String website) {
        if (TextUtils.isEmpty(website)) {
            return null;
        }
        SharedPreferences sp = ctx.getApplicationContext().getSharedPreferences(PREFS, 0);
        String key = keyFor(website);
        long ts = sp.getLong(key + ":ts", 0L);
        if (ts == 0 || System.currentTimeMillis() - ts > TTL_MILLIS) {
            return null;
        }
        return sp.getString(key, null);
    }

    /** Резолвира async и го кешира резултатот; cb се повикува на main thread. */
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
                                Log.w(TAG, "OG fetch failed " + normalizedUrl, e);
                                writeCache(app, key, "");
                                postMain(cb, null);
                            }

                            @Override
                            public void onResponse(Call call, Response response)
                                    throws IOException {
                                String img = null;
                                try (ResponseBody body = response.body()) {
                                    if (response.isSuccessful() && body != null) {
                                        img = parseOgImage(readPartial(body), normalizedUrl);
                                    }
                                }
                                writeCache(app, key, img == null ? "" : img);
                                Log.d(TAG, "OG resolved " + normalizedUrl + " -> " + img);
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
            // Често og:image е во првите 30-50KB; ако веќе го најдеме, излез.
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
    private static String parseOgImage(String html, String baseUrl) {
        Matcher m = META_IMAGE.matcher(html);
        while (m.find()) {
            String tag = m.group();
            Matcher c = CONTENT_ATTR.matcher(tag);
            if (c.find()) {
                String raw = c.group(1);
                if (raw == null || raw.isEmpty()) continue;
                String unescaped = raw.replace("&amp;", "&");
                if (unescaped.startsWith("//")) {
                    return "https:" + unescaped;
                }
                if (unescaped.startsWith("/")) {
                    try {
                        Uri base = Uri.parse(baseUrl);
                        return base.getScheme() + "://" + base.getAuthority() + unescaped;
                    } catch (Exception ignored) {
                        return null;
                    }
                }
                return unescaped;
            }
        }
        return null;
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
        return "v1:" + website.trim().toLowerCase(java.util.Locale.US);
    }
}
