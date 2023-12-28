package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.fragment.app.Fragment;

public class WebContent extends Fragment {
    private static String openLink;
    private WebView webView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_web_content, container, false);

        webView = view.findViewById(R.id.webContent);
        WebSettings webSettings = webView.getSettings();
        webView.canGoBack();
        webSettings.setJavaScriptEnabled(true); // Enable JavaScript if required
        webView.setWebViewClient(new WebViewClient());

        webView.loadUrl(openLink);

        return view;
    }
    public static void setLink(String link) {
        WebContent.openLink = link;
    }
}
