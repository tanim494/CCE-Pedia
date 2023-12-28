package com.tanim.ccepedia;

import android.graphics.Color;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_web_content, container, false);

        WebView webView = view.findViewById(R.id.webContent);
        webView.setBackgroundColor(Color.parseColor("#738F73"));
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // Enable JavaScript if required
        webView.loadUrl(openLink);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Inject JavaScript to hide elements with a specific class
                //webView.evaluateJavascript("document.body.style.color='#FFFFFF';", null);
                //webView.evaluateJavascript("document.querySelectorAll('h1').forEach(function(h1) { h1.style.color='#FFFFFF'; });", null);
                webView.evaluateJavascript("document.querySelector('.mainTableTopMiddle').style.display='none';", null);
                webView.evaluateJavascript("document.querySelector('.flex-shrink-0').style.backgroundColor='#738F73';", null);                webView.evaluateJavascript("document.querySelector('.ownershipPanel').style.display='none';", null);
                webView.evaluateJavascript("document.querySelector('.articleFooterContainer').style.display='none';", null);
                webView.evaluateJavascript("document.querySelector('.footer').style.display='none';", null);
            }
        });

        return view;
    }
    public static void setLink(String link) {
        WebContent.openLink = link;
    }
}
