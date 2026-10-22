package com.example.restaurantapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.restaurantapp.R;

public class MapaFragment extends Fragment {

    private WebView webView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapa, container, false);

        webView = view.findViewById(R.id.webViewMapa);

        // Configuramos el WebView para que se comporte como un navegador
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // Evita que el mapa se abra en el navegador externo de Android
        webView.setWebViewClient(new WebViewClient());

        // Coordenadas de Plaza Mundo Apopa
        double lat = 13.790389172491867;
        double lng = -89.17581349548963;

        // Usamos Leaflet.js (a través de HTML) para dibujar el mapa con el pin y el nombre
        String htmlMapa = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
                "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
                "    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
                "    <style>\n" +
                "        body { margin: 0; padding: 0; }\n" +
                "        #map { height: 100vh; width: 100vw; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"map\"></div>\n" +
                "    <script>\n" +
                "        var map = L.map('map').setView([" + lat + ", " + lng + "], 16);\n" +
                "        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
                "            attribution: '© OpenStreetMap'\n" +
                "        }).addTo(map);\n" +
                "        var marker = L.marker([" + lat + ", " + lng + "]).addTo(map);\n" +
                "        marker.bindPopup('<b>Restaurant App</b><br>Plaza Mundo Apopa').openPopup();\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";

        // Cargamos el HTML
        webView.loadDataWithBaseURL(null, htmlMapa, "text/html", "UTF-8", null);

        return view;
    }
}