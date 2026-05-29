package com.example.mesto_samostojna.util;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.mesto_samostojna.R;

import java.util.List;

/** Helper за Glide со chain на error-fallback URL-ови. */
public final class GlideLogoLoader {

    private GlideLogoLoader() {}

    public static void load(ImageView target, String website) {
        List<String> urls = LogoUrls.candidatesFor(website);
        RequestOptions options =
                new RequestOptions()
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_company_placeholder)
                        .error(R.drawable.ic_company_placeholder);

        if (urls.isEmpty()) {
            Glide.with(target).load(R.drawable.ic_company_placeholder).into(target);
            return;
        }

        RequestBuilder<?> request =
                Glide.with(target).load(urls.get(0)).apply(options);
        for (int i = 1; i < urls.size(); i++) {
            request = request.error(Glide.with(target).load(urls.get(i)).apply(options));
        }
        request.into(target);
    }
}
