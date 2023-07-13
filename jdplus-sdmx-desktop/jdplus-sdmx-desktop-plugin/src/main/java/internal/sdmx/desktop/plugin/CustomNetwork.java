package internal.sdmx.desktop.plugin;

import lombok.NonNull;
import nbbrd.io.curl.CurlHttpURLConnection;
import nbbrd.net.proxy.SystemProxySelector;
import nl.altindag.ssl.SSLFactory;
import sdmxdl.web.spi.Network;
import sdmxdl.web.spi.URLConnectionFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.net.ProxySelector;

@lombok.Value
@lombok.Builder(toBuilder = true)
public final class CustomNetwork implements Network {

    boolean curlBackend;
    boolean autoProxy;
    boolean defaultTrustMaterial;
    boolean systemTrustMaterial;

    @lombok.NonNull
    @lombok.Getter(lazy = true)
    private final ProxySelector lazyProxySelector = initProxySelector();

    @lombok.NonNull
    @lombok.Getter(lazy = true)
    private final sdmxdl.web.spi.SSLFactory lazySSLFactory = initSSLFactory();

    private ProxySelector initProxySelector() {
        return autoProxy ? SystemProxySelector.ofServiceLoader() : ProxySelector.getDefault();
    }

    private sdmxdl.web.spi.SSLFactory initSSLFactory() {
        SSLFactory.Builder result = SSLFactory.builder();
        if (defaultTrustMaterial) {
            result.withDefaultTrustMaterial();
        }
        if (systemTrustMaterial) {
            result.withSystemTrustMaterial();
        }
        return new SSLFactoryAdapter(result.build());
    }

    @Override
    public ProxySelector getProxySelector() {
        return getLazyProxySelector();
    }

    @Override
    public sdmxdl.web.spi.@NonNull SSLFactory getSSLFactory() {
        return getLazySSLFactory();
    }

    @Override
    public URLConnectionFactory getURLConnectionFactory() {
        return curlBackend ? CurlHttpURLConnection::of : URLConnectionFactory.getDefault();
    }

    @lombok.AllArgsConstructor
    private static final class SSLFactoryAdapter implements sdmxdl.web.spi.SSLFactory {

        private final @NonNull SSLFactory delegate;

        @Override
        public @NonNull SSLSocketFactory getSSLSocketFactory() {
            return delegate.getSslSocketFactory();
        }

        @Override
        public @NonNull HostnameVerifier getHostnameVerifier() {
            return delegate.getHostnameVerifier();
        }
    }
}
