package com.example.mesto_samostojna.util;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gradi lista URL-ovi za logo (Google S2 → DuckDuckGo).
 *
 * ZOSO dve uslugi: ako edna padne, drugata moze da go najde favicon-ot.
 * KAKO: Glide gi probuva po red preku .error() chain.
 */
public final class LogoUrls {

    private static final String GOOGLE_S2 = "https://www.google.com/s2/favicons?domain=%s&sz=128";
    private static final String DUCKDUCKGO = "https://icons.duckduckgo.com/ip3/%s.ico";

    // Privaten konstruktor — ovaa klasa e samo zbir staticki metodi (utility),
    // ne treba da se instancira.
    private LogoUrls() {}

    /**
     * Od cel URL izvlekuva "cist" domen pogoden za favicon servis.
     *
     * ZOSO: Google/DuckDuckGo baraat samo domen (pr. "primer.mk"), ne cel URL.
     * KAKO:
     *  - dodava https:// ako fali (Uri.parse bara scheme za da najde host);
     *  - trga "www." prefiks (favicon-ot e ist za www i bez www);
     *  - ako domenot e socijalna mreza (IG/FB...), vraka go glavniot domen
     *    (npr. "instagram.com/nekoj" → "instagram.com") — profil-URL nema
     *    sopstven favicon, pa zemame go faviconot na samata mreza.
     *
     * @return cist host, ili null ako vlezot ne e validen URL.
     */
    @Nullable
    public static String domainFromWebsite(@Nullable String website) {
        if (TextUtils.isEmpty(website)) {
            return null;
        }
        String input = website.trim();
        // Bez scheme, Uri.parse ne umee da go izdvoi host-ot — pa go dodavame.
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
            // Socijalni mrezi: profil-linkovite nemaat svoj favicon —
            // vrakame go domenot na mrezata za da dobieme nejzinata ikona.
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

    /**
     * Vraka podredena lista favicon URL-ovi za daden website.
     *
     * Redosledot e prioritet: prvo Google S2 (poobicno naiden), potoa
     * DuckDuckGo kako rezerva. Glide gi probuva po red preku .error() chain —
     * ako prviot padne, avtomatski go probuva vtoriot.
     *
     * @return prazna lista ako domenot ne moze da se izvlece.
     */
    public static List<String> candidatesFor(@Nullable String website) {
        List<String> out = new ArrayList<>();
        String domain = domainFromWebsite(website);
        if (domain == null) {
            return out;
        }
        out.add(String.format(Locale.US, GOOGLE_S2, domain));
        out.add(String.format(Locale.US, DUCKDUCKGO, domain));
        return out;
    }
}
