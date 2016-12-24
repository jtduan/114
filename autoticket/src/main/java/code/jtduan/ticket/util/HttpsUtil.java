package code.jtduan.ticket.util;

import okhttp3.OkHttpClient;
import okio.Buffer;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * @author jtduan
 * @date 2016/12/20
 */
public class HttpsUtil {
    /**
     * 方式一
     * @return
     */
    public static OkHttpClient getUnsafeOkHttpClient() {

        try {
            final X509TrustManager trustAllCerts = new X509TrustManager() {
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    X509Certificate[] x509Certificates = new X509Certificate[0];
                    return x509Certificates;
                }
            };
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllCerts}, null);
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient okHttpClient = new OkHttpClient()
                    .newBuilder()
                    .sslSocketFactory(sslSocketFactory, trustAllCerts)
//                    .proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1",1080)))
                    .connectTimeout(4, TimeUnit.SECONDS)
                    .readTimeout(2,TimeUnit.SECONDS)
                    .writeTimeout(2,TimeUnit.SECONDS)
                    .hostnameVerifier((a,b)->true)
                    .build();
            return okHttpClient;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    /**
     * 方式二
     * @return
     */
    @Deprecated
    public static OkHttpClient getValidOkHttpClient(){
        X509TrustManager trustManager;
        SSLSocketFactory sslSocketFactory;
        try {
            trustManager = trustManagerForCertificates(trustedCertificatesInputStream());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { trustManager }, null);
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        return new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(1,TimeUnit.SECONDS)
                .writeTimeout(1,TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .build();
    }

    private static X509TrustManager trustManagerForCertificates(InputStream in)
            throws GeneralSecurityException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(in);
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("expected non-empty set of trusted certificates");
        }

        // Put the certificates a key store.
        char[] password = "password".toCharArray(); // Any password will work.
        KeyStore keyStore = newEmptyKeyStore(password);
        int index = 0;
        for (Certificate certificate : certificates) {
            String certificateAlias = Integer.toString(index++);
            keyStore.setCertificateEntry(certificateAlias, certificate);
        }

        // Use it to build an X509 trust manager.
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }

    private static KeyStore newEmptyKeyStore(char[] password) throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream in = null; // By convention, 'null' creates an empty key store.
            keyStore.load(in, password);
            return keyStore;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static InputStream trustedCertificatesInputStream() {
        String CER_12306 ="-----BEGIN CERTIFICATE-----\n" +
                "MIICsTCCAhqgAwIBAgIIODtw6bZEH1kwDQYJKoZIhvcNAQEFBQAwRzELMAkGA1UE\n" +
                "BhMCQ04xKTAnBgNVBAoTIFNpbm9yYWlsIENlcnRpZmljYXRpb24gQXV0aG9yaXR5\n" +
                "MQ0wCwYDVQQDEwRTUkNBMB4XDTE0MDUyNjAxNDQzNloXDTE5MDUyNTAxNDQzNlow\n" +
                "azELMAkGA1UEBhMCQ04xKTAnBgNVBAoTIFNpbm9yYWlsIENlcnRpZmljYXRpb24g\n" +
                "QXV0aG9yaXR5MRkwFwYDVQQLHhCUwY3vW6JiN2cNUqFOLV/DMRYwFAYDVQQDEw1r\n" +
                "eWZ3LjEyMzA2LmNuMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC8Cxlz+V/4\n" +
                "KkUk8YTxVxzii7xp2gZPWuuVBiwQ6iwL98it75WNGiYCUasDXy3O8wY+PtZFvgEK\n" +
                "kpHqQ1U6uemiHStthUS1xTBsU/TuXF6AHc+oduP6zCGKcUnHRAksRb8BGSgzBA/X\n" +
                "3B9CUKnYa9YA2EBIYccrzIh6aRAjDHbvYQIDAQABo4GBMH8wHwYDVR0jBBgwFoAU\n" +
                "eV62d7fiUoND7cdRiExjhSwAQ1gwEQYJYIZIAYb4QgEBBAQDAgbAMAsGA1UdDwQE\n" +
                "AwIC/DAdBgNVHQ4EFgQUj/0m74jhq993ItPCldNHYLJ884MwHQYDVR0lBBYwFAYI\n" +
                "KwYBBQUHAwIGCCsGAQUFBwMBMA0GCSqGSIb3DQEBBQUAA4GBAEXeoTkvUVSeQzAx\n" +
                "FIvqfC5jvBuApczonn+Zici+50Jcu17JjqZ0zEjn4HsNHm56n8iEbmOcf13fBil0\n" +
                "aj4AQz9hGbjmvQSufaB6//LM1jVe/OSVAKB4C9NUdY5PNs7HDzdLfkQjjDehCADa\n" +
                "1DH+TP3879N5zFoWDgejQ5iFsAh0\n" +
                "-----END CERTIFICATE-----";
        return new Buffer()
                .writeUtf8(CER_12306)
                .inputStream();
    }

}
