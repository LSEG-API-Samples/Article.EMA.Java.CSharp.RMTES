///*|----------------------------------------------------------------------------------------------------
// *|            This source code is provided under the Apache 2.0 license
// *|  and is provided AS IS with no warranty or guarantee of fit for purpose.
// *|                See the project's LICENSE.md for details.
// *|           Copyright (C) 2024 LSEG. All rights reserved.
///*|----------------------------------------------------------------------------------------------------
package com.refinitiv.ema.rmtes.example;

import com.refinitiv.ema.access.*;
import com.refinitiv.ema.rdm.EmaRdm;


/**
 *  OmmConsumerClient implemented class, for handling incoming messages from the API
 */
class AppClientConsumer implements OmmConsumerClient {

    /**
     * Handle incoming Refresh Response message from the API
     * @param refreshMsg received Refresh response message from the Provider
     * @param event identifies open item for which this message is received
     */
    public void onRefreshMsg(RefreshMsg refreshMsg, OmmConsumerEvent event) {
        System.out.println("Refresh Message: ");
        System.out.printf("Item Name: %s%n", (refreshMsg.hasName() ? refreshMsg.name() : "<not set>") );
        System.out.printf("Service Name: %s%n", (refreshMsg.hasServiceName() ? refreshMsg.serviceName() : "<not set>"));

        System.out.printf("Item State: %s%n", refreshMsg.state());

        // Decoding incoming FieldList data
        if (DataType.DataTypes.FIELD_LIST == refreshMsg.payload().dataType())
            decode(refreshMsg.payload().fieldList());

        System.out.println();
    }

    /**
     * Handle incoming Update Response messages from the API
     * @param updateMsg received Update response messages from the Provider
     * @param event identifies open item for which this message is received
     */
    public void onUpdateMsg(UpdateMsg updateMsg, OmmConsumerEvent event) {
        System.out.println("Update Message: ");
        System.out.printf("Item Name: %s%n", (updateMsg.hasName() ? updateMsg.name() : "<not set>"));
        System.out.printf("Service Name: %s%n", (updateMsg.hasServiceName() ? updateMsg.serviceName() : "<not set>"));

        // Decoding incoming FieldList data
        if (DataType.DataTypes.FIELD_LIST == updateMsg.payload().dataType())
            decode(updateMsg.payload().fieldList());

        System.out.println();
    }

    /**
     * Handle incoming Status Response messages from the API
     * @param statusMsg received Status response messages from the Provider
     * @param event identifies open item for which this message is received
     */
    public void onStatusMsg(StatusMsg statusMsg, OmmConsumerEvent event) {
        System.out.println("Status Message: ");
        System.out.printf("Item Name: %s%n", (statusMsg.hasName() ? statusMsg.name() : "<not set>"));
        System.out.printf("Service Name: %s%n", (statusMsg.hasServiceName() ? statusMsg.serviceName() : "<not set>"));

        if (statusMsg.hasState())
            System.out.printf("Item State: %s%n", statusMsg.state());

        System.out.println();
    }

    public void onGenericMsg(GenericMsg genericMsg, OmmConsumerEvent consumerEvent) {
    }

    public void onAckMsg(AckMsg ackMsg, OmmConsumerEvent consumerEvent) {
    }

    public void onAllMsg(Msg msg, OmmConsumerEvent consumerEvent) {
    }

    /**
     * Decoding OMM Fieldlist object
     * @param fieldList incoming OMMFieldList object from the Provider
     */
    void decode(FieldList fieldList) {
        fieldList.forEach(fieldEntry -> {
            System.out.printf("Fid %d Name = %s DataType: %s Value: ", fieldEntry.fieldId(), fieldEntry.name(), DataType.asString(fieldEntry.load().dataType()));
            if (Data.DataCode.BLANK == fieldEntry.code())
                System.out.println(" blank");
            else
                //Handle each data type (fits to the type of requests FIDs)
                switch (fieldEntry.loadType()) {
                    case DataType.DataTypes.REAL:
                        System.out.println(fieldEntry.real().asDouble());
                        break;
                    case DataType.DataTypes.ASCII:
                        System.out.println(fieldEntry.ascii());
                        break;
                    case DataType.DataTypes.ENUM:
                        System.out.println(fieldEntry.hasEnumDisplay() ? fieldEntry.enumDisplay() : fieldEntry.enumValue());
                        break;
                    case DataType.DataTypes.RMTES:
                        System.out.println(fieldEntry.rmtes());
                        break;
                    case DataType.DataTypes.ERROR:
                        System.out.println("(" + fieldEntry.error().errorCodeAsString() + ")");
                        break;
                    default:
                        System.out.println();
                        break;
                }
        });
    }
}

/**
 * Main Consumer class
 */
public class RMTESConsumer {
    /**
     * Main method
     * @param args
     */
    public static void main(String[] args) {
        // Crate OmmConsumer class
        OmmConsumer consumer = null;
        try {
            // Initiate Consumer AppClient class
            AppClientConsumer appClient = new AppClientConsumer();

            OmmConsumerConfig config = EmaFactory.createOmmConsumerConfig();

            System.out.println("Consumer: Start");
            // Init connection to the Provider
            consumer = EmaFactory.createOmmConsumer(config.consumerName("Consumer_1").username("user"));

            // Construct the View Request
            ElementList view = EmaFactory.createElementList();
            OmmArray array = EmaFactory.createOmmArray();

            array.fixedWidth(2);
            array.add(EmaFactory.createOmmArrayEntry().intValue(15));
            array.add(EmaFactory.createOmmArrayEntry().intValue(22));
            array.add(EmaFactory.createOmmArrayEntry().intValue(25));
			array.add(EmaFactory.createOmmArrayEntry().intValue(30));
			array.add(EmaFactory.createOmmArrayEntry().intValue(31));
			array.add(EmaFactory.createOmmArrayEntry().intValue(260));
			array.add(EmaFactory.createOmmArrayEntry().intValue(1352));

            view.add(EmaFactory.createElementEntry().uintValue(EmaRdm.ENAME_VIEW_TYPE, 1));
            view.add(EmaFactory.createElementEntry().array(EmaRdm.ENAME_VIEW_DATA, array));

            System.out.println("Consumer: Sending Item request");
            //Send item request message with View
            consumer.registerClient(EmaFactory.createReqMsg().serviceName("DIRECT_FEED").name("/LSEG.L").payload(view), appClient);

            Thread.sleep(6000000); // API calls onRefreshMsg(), onUpdateMsg() and onStatusMsg()
        } catch (InterruptedException | OmmException excp) {
            System.out.println(excp.getMessage());
        } finally {
            if (consumer != null) consumer.uninitialize();
        }
    }
}
