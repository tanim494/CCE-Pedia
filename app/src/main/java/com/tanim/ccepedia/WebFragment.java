package com.tanim.ccepedia;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class WebFragment extends Fragment {

    private static final String ARG_URL = "url";
    private String url;
    private WebView webView;
    private ProgressBar progressBar;
    private FloatingActionButton fabShare, fabShareCommunity;

    private FirebaseFirestore db;
    private String appUpdateLink;

    public static WebFragment newInstance(String url) {
        WebFragment fragment = new WebFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            url = getArguments().getString(ARG_URL);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_web_view, container, false);

        webView = view.findViewById(R.id.webViewContainer);
        progressBar = view.findViewById(R.id.progressBar);
        fabShare = view.findViewById(R.id.fabShare);
        fabShare.setOnClickListener(v -> shareCurrentPage());

        // Share the currently loaded page into the Community Chat as a link attachment card.
        fabShareCommunity = view.findViewById(R.id.fabShareCommunity);
        fabShareCommunity.setOnClickListener(v -> {
            String currentUrl = (webView != null && webView.getUrl() != null) ? webView.getUrl() : url;
            String title = (webView != null && webView.getTitle() != null && !webView.getTitle().isEmpty())
                    ? webView.getTitle() : currentUrl;
            ShareToCommunityDialog.show(requireContext(), CommunityShare.TYPE_LINK, currentUrl, title);
        });

        setupWebView();
        fetchAppConfig();

        if (url != null && !url.isEmpty()) {
            String finalUrl = url;

            if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                finalUrl = "https://" + finalUrl;
            }

            if (finalUrl.contains("drive.google.com")) {
                if (!finalUrl.contains("?")) {
                    finalUrl = finalUrl + "?usp=sharing";
                } else if (!finalUrl.contains("usp=")) {
                    finalUrl = finalUrl + "&usp=sharing";
                }
            }

            webView.loadUrl(finalUrl);
        }

        return view;
    }

    private void fetchAppConfig() {
        db.collection("appConfig").document("main")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        appUpdateLink = documentSnapshot.getString("updateLink");
                    }
                });
    }

    private void shareCurrentPage() {
        if (webView == null || webView.getUrl() == null) {
            Toast.makeText(requireContext(), "Page not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUrl = webView.getUrl();

        String shareMessage = "Check out this link from IIUC Pedia:\n" +
                currentUrl;

        if (appUpdateLink != null && !appUpdateLink.isEmpty()) {
            shareMessage += "\n\nDownload IIUC Pedia from - " + appUpdateLink;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared from IIUC Pedia");

        startActivity(Intent.createChooser(shareIntent, "Share link via..."));
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        webSettings.setSaveFormData(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("fb:") || url.startsWith("tg:") || url.startsWith("mailto:") || url.startsWith("tel:") || url.startsWith("whatsapp:")) {//Tanim
                    try {
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);//Tanim
                    } catch (android.content.ActivityNotFoundException e) {
                        Toast.makeText(requireContext(), "No application found to handle this link.", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }

                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                webView.setVisibility(View.VISIBLE);
                super.onPageFinished(view, url);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

                DownloadManager dm = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);

                if (dm != null) {
                    Uri uri = Uri.parse(url);
                    DownloadManager.Request request = new DownloadManager.Request(uri);

                    request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url));
                    request.addRequestHeader("User-Agent", userAgent);//Tanim

                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                    String suggestedFilename = URLUtil.guessFileName(url, contentDisposition, mimetype);


                    String filenameWithoutExt;
                    String extension = "";
                    int lastDot = suggestedFilename.lastIndexOf('.');

                    if (lastDot > 0) {
                        filenameWithoutExt = suggestedFilename.substring(0, lastDot);
                        extension = suggestedFilename.substring(lastDot);
                    } else {
                        filenameWithoutExt = suggestedFilename;
                    }

                    String prefixedFilename = "IIUC_Pedia_" + filenameWithoutExt;

                    suggestedFilename = prefixedFilename + extension;

                    String lowerFilename = suggestedFilename.toLowerCase();
                    boolean isImage = lowerFilename.endsWith(".jpg") ||
                            lowerFilename.endsWith(".jpeg") ||
                            lowerFilename.endsWith(".png");

                    if (!isImage) {
                        String filenameFromHeader = URLUtil.guessFileName(url, contentDisposition, mimetype);
                        if (filenameFromHeader != null && !filenameFromHeader.isEmpty()) {
                        }

                        if (!suggestedFilename.toLowerCase().endsWith(".pdf")) {

                            int lastDotIndex = suggestedFilename.lastIndexOf('.');
                            if (lastDotIndex != -1) {
                                suggestedFilename = suggestedFilename.substring(0, lastDotIndex) + ".pdf";//Tanim
                            } else {
                                suggestedFilename = suggestedFilename + ".pdf";
                            }
                        }
                    }

                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, suggestedFilename);

                    dm.enqueue(request);
                    Toast.makeText(requireContext(), "Starting download: " + suggestedFilename, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Download manager not available.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public boolean canGoBack() {
        return webView != null && webView.canGoBack();
    }

    public void goBack() {
        if (webView != null) {
            webView.goBack();
        }
    }
}