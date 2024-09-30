package com.refinitiv.ema.rmtes.example;

import com.refinitiv.ema.access.Msg;
import com.refinitiv.ema.access.AckMsg;
import com.refinitiv.ema.access.GenericMsg;
import com.refinitiv.ema.access.RefreshMsg;
import com.refinitiv.ema.access.ReqMsg;
import com.refinitiv.ema.access.StatusMsg;
import com.refinitiv.ema.access.UpdateMsg;
import com.refinitiv.ema.access.EmaFactory;
import com.refinitiv.ema.access.OmmConsumer;
import com.refinitiv.ema.access.OmmConsumerClient;
import com.refinitiv.ema.access.OmmConsumerConfig;
import com.refinitiv.ema.access.OmmConsumerEvent;
import com.refinitiv.ema.access.OmmException;

class AppClientConsumer implements OmmConsumerClient {
    public void onRefreshMsg(RefreshMsg refreshMsg, OmmConsumerEvent event) {
        System.out.println(refreshMsg);
    }

    public void onUpdateMsg(UpdateMsg updateMsg, OmmConsumerEvent event) {
        System.out.println(updateMsg);
    }

    public void onStatusMsg(StatusMsg statusMsg, OmmConsumerEvent event) {

        System.out.println(statusMsg);
    }

    public void onGenericMsg(GenericMsg genericMsg, OmmConsumerEvent consumerEvent) {
    }

    public void onAckMsg(AckMsg ackMsg, OmmConsumerEvent consumerEvent) {
    }

    public void onAllMsg(Msg msg, OmmConsumerEvent consumerEvent) {
    }
}

public class RMTESConsumer {
    public static void main(String[] args) {
        OmmConsumer consumer = null;
        try {
            AppClientConsumer appClient = new AppClientConsumer();

            OmmConsumerConfig config = EmaFactory.createOmmConsumerConfig();

            consumer = EmaFactory.createOmmConsumer(config.host("localhost:14002").username("user"));

            ReqMsg reqMsg = EmaFactory.createReqMsg();

            consumer.registerClient(reqMsg.serviceName("DIRECT_FEED").name("/LSEG.L"), appClient);

            Thread.sleep(6000000);
        } catch (InterruptedException | OmmException excp) {
            System.out.println(excp.getMessage());
        } finally {
            if (consumer != null) consumer.uninitialize();
        }
    }
}
