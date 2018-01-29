package com.semuxpool.pool.aggregation;

import com.semuxpool.client.SemuxClient;
import com.semuxpool.client.api.Delegate;
import com.semuxpool.client.api.SemuxException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
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
        // don't bother registering with semuxpool.com if not donating.
        if (donationPercent < 0.05)
        {

            HttpPost post = new HttpPost("https://semuxpool.com/api/delegates");
            try
            {
                Delegate delegate = client.getDelegate(validatorAddress);
                if(delegate == null)
                {
                    logger.error("Unable to find delegate with address " + validatorAddress);
                    return;
                }
                String delegateName = delegate.getName();

                CloseableHttpClient httpClient = HttpClients.createDefault();
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
            catch (IOException | SemuxException e)
            {
                logger.error(e.getMessage(), e);
            }
        }
        else
        {
            logger.warn("Not registering with semuxpool.com, donation amount too low.");
        }
    }
}
