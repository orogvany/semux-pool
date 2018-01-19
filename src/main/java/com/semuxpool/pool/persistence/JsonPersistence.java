package com.semuxpool.pool.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semuxpool.client.api.SemuxException;
import com.semuxpool.pool.api.Payment;
import com.semuxpool.pool.api.Payout;
import com.semuxpool.pool.state.PoolState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persist payouts to readable json.
 * An actual datastore would be nicer longterm, but this is easy to debug.
 */
public class JsonPersistence implements Persistence
{
    private static final Logger logger = LoggerFactory.getLogger(JsonPersistence.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PAYOUT_DIRECTORY = "payouts";

    public String persistPayout(Payout payout)
    {
        if (payout.getPayouts().isEmpty() && payout.getPaidPayouts().isEmpty())
        {
            return null;
        }
        String fileName = PAYOUT_DIRECTORY + File.separator + "Payout-" + System.currentTimeMillis() + "-" + payout.getStartBlock() + "-" + payout.getEndBlock() + ".json";
        //persist the block
        try
        {
            //don't double persist
            if (payout.getId() != null)
            {
                logger.error("Already persisted block! Use Update method");
                System.exit(-1);
            }
            payout.setId(fileName);
            MAPPER.writeValue(new File(fileName), payout);
            logger.info("Wrote " + payout.getStartBlock() + " - " + payout.getEndBlock());


            return fileName;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }


    public PoolState loadPoolState() throws IOException, SemuxException
    {
        PoolState poolState = new PoolState();

        long block = 0l;

        List<Payout> payouts = getAllPayouts();
        for (Payout payout : payouts)
        {
            poolState.addPayout(payout);
            block = payout.getEndBlock();
        }

        //start with block after last tallied.
        poolState.setCurrentBlock(block + 1);

        return poolState;
    }

    @Override
    public void update(Payout payout)
    {
        if (payout.getId() == null)
        {
            logger.error("Unpersisted object, use persist");
            System.exit(-1);
        }
        try
        {
            MAPPER.writeValue(new File(payout.getId()), payout);
        }
        catch (IOException e)
        {
            logger.error("Unable to persist file", e);
        }
    }

    public List<Payout> getAllPayouts() throws IOException
    {
        //load all the payouts
        ObjectMapper mapper = new ObjectMapper();

        File directory = new File(PAYOUT_DIRECTORY);
        if (!directory.isDirectory())
        {
            directory.mkdir();
        }
        List<String> payoutFileNames = new ArrayList<String>();
        for (File file : directory.listFiles())
        {
            payoutFileNames.add(file.getAbsolutePath());
        }
        Collections.sort(payoutFileNames);

        List<Payout> payouts = new ArrayList<>();
        for (String fileName : payoutFileNames)
        {
            logger.debug("Checking file " + fileName);
            //look for last one not paid out
            File file = new File(fileName);
            Payout payout = mapper.readValue(file, Payout.class);
            payouts.add(payout);
        }
        return payouts;
    }
}
