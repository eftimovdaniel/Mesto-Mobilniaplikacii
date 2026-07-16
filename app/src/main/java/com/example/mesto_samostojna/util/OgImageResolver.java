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
 * Izvlekuva "vistinska" slika na biznisot od website HTML.
 *
 * ZOSO: favicon e mal; og:image e podobar (profil/cover od sajt).
 * Prioritet: og:image → twitter:image → apple-touch-icon → link rel=icon.
 * Kesira vo SharedPreferences (TTL 1 den hit / 30 min miss) — da ne se
 * downloada HTML za sekoj red vo listata.
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

    /** null = nemame info uste. "" = znaeme deka nema (negative cache). */
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

    /**
     * Asinhrono go simnuva HTML-ot na sajtot i bara najdobra slika.
     *
     * ZOSO async (OkHttp enqueue): mreza ne smee na UI thread. Rezultatot se
     * vraka preku Callback2, sekogas na main thread (vidi postMain), za da moze
     * bezbedno da se setira na ImageView.
     * KAKO: gradi Request so lazen "brauzerski" User-Agent (nekoi sajtovi
     * blokiraat nepoznati klienti), go cita HTML-ot, parsira, i kesira rezultat.
     */
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

    /**
     * Cita samo del od HTML-ot (do MAX_BYTES = 256 KB).
     *
     * ZOSO delumno citanje: og:image/meta tagovite se vo <head> (pocetokot na
     * stranata) — nema potreba da simnuvame cela golema stranica. Cim najdeme
     * meta-image tag (po prvite ~32 KB), prekinuvame i vrakame — pobrzo i
     * pomalku podatoci.
     */
    @WorkerThread
    private static String readPartial(ResponseBody body) throws IOException {
        byte[] buf = new byte[MAX_BYTES];
        int total = 0;
        int n;
        var src = body.byteStream();
        while (total < MAX_BYTES && (n = src.read(buf, total, MAX_BYTES - total)) != -1) {
            total += n;
            // Rano izleguvanje: cim ima dovolno pročitano i najdovme tag, stop.
            if (total > 32 * 1024) {
                String s = new String(buf, 0, total, java.nio.charset.StandardCharsets.UTF_8);
                if (META_IMAGE.matcher(s).find()) {
                    return s;
                }
            }
        }
        return new String(buf, 0, total, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Bara slika vo HTML-ot po prioritet: og:image/twitter:image → apple-touch
     * -icon → &lt;link rel="icon"&gt;. Prviot pogoden se vraka kako apsoluten URL.
     * ZOSO ovoj redosled: og:image e najkvaliteten (cover/profil), ikonite se
     * rezerva koga sajtot nema social meta tagovi.
     */
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

    /** Default placeholder favicon-i sto da gi ignorirame. */
    private static boolean isGenericFavicon(String url) {
        String low = url.toLowerCase(java.util.Locale.US);
        return low.endsWith("/vite.svg")
                || low.endsWith("/favicon.ico")
                || low.endsWith("/favicon-16x16.png")
                || low.endsWith("/favicon-32x32.png")
                || low.contains("/wp-includes/images/")
                || low.contains("/wp-content/themes/twenty");
    }

    /**
     * Pretvora bilo kakov URL od HTML (relativen, "//", "/path", cel) vo
     * apsoluten URL sto Glide moze da go simne.
     *
     * ZOSO: content/href atributite cesto se relativni (npr. "/img/logo.png") —
     * bez base domen nemaat smisla. Isto taka gi otfrla data: URI i genericki
     * placeholder favicon-i (nema vrednost da gi prikazuvame).
     */
    @Nullable
    private static String absolutize(@Nullable String raw, String baseUrl) {
        if (raw == null || raw.isEmpty()) return null;
        String s = raw.replace("&amp;", "&").trim(); // dekodiraj HTML-escaped &
        if (s.startsWith("data:")) return null;      // inline base64 — preskoci
        if (isGenericFavicon(s)) return null;        // default ikona — bez vrednost
        if (s.startsWith("//")) return "https:" + s; // protocol-relative → https
        if (s.startsWith("/")) {
            // Apsolutna pateka na istiot host: scheme://host + path
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
        // Relativen URL → join so base.
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

    /**
     * Zapisuva rezultat vo kes zaedno so vremenska oznaka (:ts).
     * Prazen string "" = negativen kes (znaeme deka nema slika) — sprecuva
     * povtoreno simnuvanje na sekoe renderiranje na redot.
     */
    private static void writeCache(Context app, String key, String value) {
        SharedPreferences sp = app.getSharedPreferences(PREFS, 0);
        sp.edit()
                .putString(key, value)
                .putLong(key + ":ts", System.currentTimeMillis())
                .apply();
    }

    /**
     * Go vraka callback-ot na glavniot (UI) thread.
     * ZOSO: OkHttp callback-ot rabota na pozadinska niska, a menuvanje na
     * ImageView smee samo od UI thread — pa prekuHandler(mainLooper) go "frlame"
     * rezultatot nazad na main thread.
     */
    private static void postMain(Callback2 cb, @Nullable String url) {
        new android.os.Handler(android.os.Looper.getMainLooper())
                .post(() -> cb.onResolved(url));
    }

    /** Osiguruva deka website ima scheme i validen host; inaku null. */
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

    /**
     * Klucot za SharedPreferences kes. Prefiksot "v2:" e verzija na kes —
     * ako se smeni logikata na parsiranje, dovolno e da se smeni prefiksot
     * za starite (potencijalno pogresni) vnesovi da se ignoriraat.
     */
    private static String keyFor(String website) {
        return "v2:" + website.trim().toLowerCase(java.util.Locale.US);
    }
}
