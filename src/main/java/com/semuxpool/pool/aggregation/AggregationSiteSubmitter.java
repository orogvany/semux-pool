package com.semuxpool.pool.aggregation;

import com.semuxpool.client.SemuxClient;
import com.semuxpool.client.api.Delegate;
import com.semuxpool.client.api.SemuxException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * todo - work in progress.
 * need to figure out ssl certs issue
 */
public class AggregationSiteSubmitter
{
    private static final Logger logger = LoggerFactory.getLogger(AggregationSiteSubmitter.class);

    public void registerValidator(String validatorAddress, float donationPercent, SemuxClient client)
    {
        /**
         * The software is free/open without restriction, but semuxpool.com advertising for pool
         * It costs money to run, and my web dev guy likes to be paid for his work :D
         *
         * Validation also occurs server side..
         */
        // don't register with semuxpool.com if not supporting site operation.
        // advertising is not free.
        if (donationPercent >= 0.05f)
        {
            HttpPost post = new HttpPost("https://semuxpool.com/api/delegates");
            try
            {
                Delegate delegate = client.getDelegate(validatorAddress);
                if (delegate == null)
                {
                    logger.error("Unable to find delegate with address " + validatorAddress);
                    return;
                }
                String delegateName = delegate.getName();

                CloseableHttpClient httpClient = getClient();
                StringEntity entity = new StringEntity("[{\"name\":\"" + delegateName + "\"}]");
                post.setEntity(entity);
                post.setHeader("Accept", "application/json");
                post.setHeader("Content-type", "application/json");

                CloseableHttpResponse response = httpClient.execute(post);
                if (response.getStatusLine().getStatusCode() != 200)
                {
                    logger.error("Unable to register delegate with semuxpool.com");
                }
                httpClient.close();
            }
            catch (IOException | SemuxException | NoSuchAlgorithmException | KeyManagementException e)
            {
                logger.error(e.getMessage(), e);
            }
        }
        else
        {
            logger.warn("Not registering with semuxpool.com, donation amount too low.");
        }
    }

    private CloseableHttpClient getClient() throws KeyManagementException, NoSuchAlgorithmException
    {
        SSLContext sslContext = SSLContext.getInstance("SSL");

        // set up a TrustManager that trusts everything temporarily
        sslContext.init(
            null, new TrustManager[] {
                new X509TrustManager()
                {
                    public X509Certificate[] getAcceptedIssuers()
                    {
                        System.out.println("getAcceptedIssuers =============");
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs,
                        String authType)
                    {
                        System.out.println("checkClientTrusted =============");
                    }

                    public void checkServerTrusted(X509Certificate[] certs,
                        String authType)
                    {
                        System.out.println("checkServerTrusted =============");
                    }
                } }, new SecureRandom());

        SSLSocketFactory sf = new SSLSocketFactory(sslContext);
        Scheme httpsScheme = new Scheme("https", 443, sf);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(httpsScheme);

        // apache HttpClient version >4.2 should use BasicClientConnectionManager
        ClientConnectionManager cm = new SingleClientConnManager(schemeRegistry);
        return HttpClients.custom().setSslcontext(sslContext).build();
    }
}
