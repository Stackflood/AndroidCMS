package com.example.manish.androidcms.networking;

/**
 * Created by Manish on 4/13/2015.
 */
public class OAuthAuthenticatorFactoryDefault implements OAuthAuthenticatorFactoryAbstract {
    public OAuthAuthenticator make() {
        return new OAuthAuthenticator();
    }
}

