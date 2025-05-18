package com.tanim.ccepedia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class Faculty extends Fragment {

    private ListView facultyListView;
    private ArrayList<FacultyModel> facultyList = new ArrayList<>();
    private FacultyAdapter facultyAdapter;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_faculty, container, false);

        facultyListView = rootView.findViewById(R.id.facultyListView);
        facultyAdapter = new FacultyAdapter(requireContext(), facultyList);
        facultyListView.setAdapter(facultyAdapter);

        fetchFacultyFromWeb();

        facultyListView.setOnItemClickListener((parent, view, position, id) -> {
            FacultyModel selected = facultyList.get(position);
            dialPhoneNumber(selected.getPhone());
            Toast.makeText(requireContext(), "Calling " + selected.getName() + " Sir", Toast.LENGTH_SHORT).show();
        });

        return rootView;
    }

    private void fetchFacultyFromWeb() {
        new Thread(() -> {
            try {
                String url = "https://www.iiuc.ac.bd/cce/faculty";
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0")
                        .timeout(10000)
                        .get();

                Elements headings = doc.select("div.heading_s1 h2");
                ArrayList<FacultyModel> tempList = new ArrayList<>();

                for (Element heading : headings) {
                    String title = heading.text().trim();

                    // Skip unwanted sections
                    if (title.equalsIgnoreCase("Administrative and General Staff") ||
                            title.equalsIgnoreCase("Chairman & Co-ordinator")) {
                        continue;
                    }

                    Element current = heading.parent().parent().nextElementSibling();
                    while (current != null && current.select("h2").isEmpty()) {
                        Elements tables = current.select("table.table-striped");

                        for (Element table : tables) {
                            Element img = table.selectFirst("img");
                            String imageUrl = (img != null) ? img.attr("src") : "";
                            if (!imageUrl.startsWith("http") && !imageUrl.isEmpty()) {
                                imageUrl = "https://www.iiuc.ac.bd" + imageUrl;
                            }

                            Element facultyInfo = table.selectFirst("td:nth-child(2)");

                            String name = getTextSafe(facultyInfo, "strong");
                            String designation = getSiblingTextSafe(facultyInfo, "i.fa-check-circle");
                            String mobile = extractValue(facultyInfo.html(), "Mobile:");

                            FacultyModel faculty = new FacultyModel(name, designation, mobile, imageUrl);
                            tempList.add(faculty);
                        }
                        current = current.nextElementSibling();
                    }
                }

                // Update UI on main thread
                mainHandler.post(() -> {
                    facultyList.clear();
                    facultyList.addAll(tempList);
                    facultyAdapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(requireContext(),
                        "Failed to load faculty data: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void dialPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }

    private String getTextSafe(Element parent, String selector) {
        Element el = parent.selectFirst(selector);
        return el != null ? el.text().trim() : "";
    }

    private String getSiblingTextSafe(Element parent, String selector) {
        Element el = parent.selectFirst(selector);
        return (el != null && el.nextSibling() != null) ? el.nextSibling().toString().trim() : "";
    }

    private String extractValue(String html, String keyword) {
        int index = html.indexOf(keyword);
        if (index == -1) return "";
        int start = index + keyword.length();
        int end = html.indexOf("<", start);
        return html.substring(start, end).replaceAll("&nbsp;", "").trim();
    }
}
