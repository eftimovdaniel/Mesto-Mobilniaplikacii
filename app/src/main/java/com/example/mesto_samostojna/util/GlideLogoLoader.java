package com.example.mesto_samostojna.util;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.mesto_samostojna.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper za logo na kompanija (Glide).
 *
 * ZOSO fallback chain: ne site kompanii imaat image_url — seuste sakame ikona.
 * KAKO RABOTI (prioritet):
 *   1) imageUrl od korisnikot (ako e vnesen)
 *   2) kesiran og:image od website (profil slika IG/FB itd.)
 *   3) async fetch og:image → apply na istiot ImageView
 *   4) Google S2 favicon → DuckDuckGo → lokalen placeholder
 */
public final class GlideLogoLoader {

    private static final String TAG = "GlideLogoLoader";
    private static final int TAG_WEBSITE = R.id.row_icon; // postoecki id kako tag-key

    private GlideLogoLoader() {}

    /** Kompatibilnost: samo website (bez imageUrl). */
    public static void load(ImageView target, @Nullable String website) {
        load(target, null, website);
    }

    /**
     * Prv prioritet: imageUrl. Ako e prazen → og:image od website → favicon → placeholder.
     */
    public static void load(
            ImageView target, @Nullable String imageUrl, @Nullable String website) {
        target.setTag(TAG_WEBSITE, website);

        if (!TextUtils.isEmpty(imageUrl)) {
            applyChain(target, imageUrl, LogoUrls.candidatesFor(website));
            return;
        }

        String cachedOg = OgImageResolver.cachedFor(target.getContext(), website);
        if (!TextUtils.isEmpty(cachedOg)) {
            applyChain(target, cachedOg, LogoUrls.candidatesFor(website));
            return;
        }

        applyChain(target, null, LogoUrls.candidatesFor(website));

        if (TextUtils.isEmpty(website) || cachedOg != null /* kesirano "" = negativno */) {
            return;
        }

        OgImageResolver.resolve(
                target.getContext(),
                website,
                resolved -> {
                    Object current = target.getTag(TAG_WEBSITE);
                    if (current == null || !current.equals(website)) return;
                    if (TextUtils.isEmpty(resolved)) return;
                    applyChain(target, resolved, LogoUrls.candidatesFor(website));
                });
    }

    private static void applyChain(
            ImageView target, @Nullable String primary, List<String> fallbacks) {
        List<String> chain = new ArrayList<>();
        if (!TextUtils.isEmpty(primary)) chain.add(primary);
        if (fallbacks != null) chain.addAll(fallbacks);

        RequestOptions options =
                new RequestOptions()
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_company_placeholder)
                        .error(R.drawable.ic_company_placeholder);

        if (chain.isEmpty()) {
            Glide.with(target).load(R.drawable.ic_company_placeholder).into(target);
            return;
        }

        RequestBuilder<Drawable> request =
                Glide.with(target).load(chain.get(0)).apply(options);
        for (int i = 1; i < chain.size(); i++) {
            request = request.error(Glide.with(target).load(chain.get(i)).apply(options));
        }
        request.listener(
                        new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(
                                    @Nullable GlideException e,
                                    Object model,
                                    Target<Drawable> t,
                                    boolean isFirstResource) {
                                Log.w(TAG, "Glide load failed for model=" + model, e);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(
                                    Drawable resource,
                                    Object model,
                                    Target<Drawable> t,
                                    DataSource dataSource,
                                    boolean isFirstResource) {
                                Log.d(TAG, "Glide loaded " + model + " from " + dataSource);
                                return false;
                            }
                        })
                .into(target);
    }
}
