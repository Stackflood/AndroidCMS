package com.example.manish.wordpressnetworking;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.example.manish.wordpresscomrest.RestClient;
import com.example.manish.wordpresscomrest.RestRequest;
import com.example.manish.wordpresscomrest.Oauth;
import com.example.manish.wordpresscomrest.RestRequest.ErrorListener;

public class RestClientFactoryDefault implements RestClientFactoryAbstract {
    public RestClient make(RequestQueue queue) {
        return new RestClient(queue);
    }

    public RestClient make(RequestQueue queue, RestClient.REST_CLIENT_VERSIONS version) {
        return new RestClient(queue, version);
    }
}
