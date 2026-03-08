package com.haircutbooking.Haircut_Booking.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

public class VNPayUtil {
    public static String getTxnRef(String url) throws URISyntaxException {
        URI uri = new URI(url);
        List<NameValuePair> params = URLEncodedUtils.parse(uri, "UTF-8");
        Map<String, String> paramMap = params.stream()
                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        return paramMap.get("vnp_TxnRef");
    }
}