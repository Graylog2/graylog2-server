package org.graylog2.shared.bindings;

import com.ning.http.client.AsyncHttpClient;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AsyncHttpClientProvider implements Provider<AsyncHttpClient> {
    private static AsyncHttpClient asyncHttpClient = null;

    @Inject
    public AsyncHttpClientProvider() {
        if (asyncHttpClient == null)
            asyncHttpClient = new AsyncHttpClient();
    }

    @Override
    public AsyncHttpClient get() {
        return asyncHttpClient;
    }
}
