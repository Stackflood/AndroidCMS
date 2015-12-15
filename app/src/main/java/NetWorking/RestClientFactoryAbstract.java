package NetWorking;

import com.android.volley.RequestQueue;

import Rest.RestClient;

public interface RestClientFactoryAbstract {
    public RestClient make(RequestQueue queue);
    public RestClient make(RequestQueue queue, RestClient.REST_CLIENT_VERSIONS version);
}
