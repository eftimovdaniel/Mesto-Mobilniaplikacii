package com.example.mesto_samostojna.util;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Гради листа URL-ови за логото на компанијата (по приоритет: clearbit → google s2 → DuckDuckGo).
 * Glide ги пробува по ред преку .error() chain.
 */
public final class LogoUrls {

    private static final String CLEARBIT = "https://logo.clearbit.com/%s?size=256";
    private static final String GOOGLE_S2 = "https://www.google.com/s2/favicons?domain=%s&sz=128";
    private static final String DUCKDUCKGO = "https://icons.duckduckgo.com/ip3/%s.ico";

    private LogoUrls() {}

    @Nullable
    public static String domainFromWebsite(@Nullable String website) {
        if (TextUtils.isEmpty(website)) {
            return null;
        }
        String input = website.trim();
        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            input = "https://" + input;
        }
        try {
            Uri uri = Uri.parse(input);
            String host = uri.getHost();
            if (host == null) {
                return null;
            }
            host = host.toLowerCase(Locale.US);
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            String[] socials = {
                "instagram.com",
                "facebook.com",
                "tiktok.com",
                "twitter.com",
                "x.com",
                "youtube.com"
            };
            for (String s : socials) {
                if (host.equals(s) || host.endsWith("." + s)) {
                    return s;
                }
            }
            return host;
        } catch (Exception e) {
            return null;
        }
    }

    public static List<String> candidatesFor(@Nullable String website) {
        List<String> out = new ArrayList<>();
        String domain = domainFromWebsite(website);
        if (domain == null) {
            return out;
        }
        out.add(String.format(Locale.US, CLEARBIT, domain));
        out.add(String.format(Locale.US, GOOGLE_S2, domain));
        out.add(String.format(Locale.US, DUCKDUCKGO, domain));
        return out;
    }
}
