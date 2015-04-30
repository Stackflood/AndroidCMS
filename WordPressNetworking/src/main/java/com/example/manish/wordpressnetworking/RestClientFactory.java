package com.example.manish.wordpressnetworking;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.example.manish.wordpresscomrest.RestClient;
import com.example.manish.wordpresscomrest.RestRequest;
import com.example.manish.wordpresscomrest.Oauth;
import com.example.manish.wordpresscomrest.RestRequest.ErrorListener;

public class RestClientFactory {
    private static RestClientFactoryAbstract sFactory;

    public static RestClient instantiate(RequestQueue queue) {
        return instantiate(queue, RestClient.REST_CLIENT_VERSIONS.V1);
    }

    public static RestClient instantiate(RequestQueue queue, RestClient.REST_CLIENT_VERSIONS version) {
        if (sFactory == null) {
            sFactory = new RestClientFactoryDefault();
        }
        return sFactory.make(queue, version);
    }
}
