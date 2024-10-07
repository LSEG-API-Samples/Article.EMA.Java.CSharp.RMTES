///*|----------------------------------------------------------------------------------------------------
// *|            This source code is provided under the Apache 2.0 license
// *|  and is provided AS IS with no warranty or guarantee of fit for purpose.
// *|                See the project's LICENSE.md for details.
// *|           Copyright (C) 2024 LSEG. All rights reserved.
///*|----------------------------------------------------------------------------------------------------

package com.refinitiv.ema.rmtes.example;

import com.refinitiv.ema.access.*;
import com.refinitiv.ema.rdm.EmaRdm;

import java.nio.ByteBuffer;
import java.util.Random;

// OmmProviderClient implemented class, for handling incoming consumer request messages from the API
class AppClientProvider implements OmmProviderClient {
    public long itemHandle = 0;

    /**
     * Handle incoming consumer request messages (Login, Item, etc)
     * @param reqMsg received request messages from Consumer
     * @param providerEvent identifies open item for which this message is received
     */
    public void onReqMsg(ReqMsg reqMsg, OmmProviderEvent providerEvent) {
        switch (reqMsg.domainType()) {
            case EmaRdm.MMT_LOGIN:
                processLoginRequest(reqMsg, providerEvent);
                break;
            case EmaRdm.MMT_MARKET_PRICE:
                processMarketPriceRequest(reqMsg, providerEvent);
                break;
            default:
                processInvalidItemRequest(reqMsg, providerEvent);
                break;
        }
    }

    public void onRefreshMsg(RefreshMsg refreshMsg, OmmProviderEvent providerEvent) {
    }

    public void onStatusMsg(StatusMsg statusMsg, OmmProviderEvent providerEvent) {
    }

    public void onGenericMsg(GenericMsg genericMsg, OmmProviderEvent providerEvent) {
    }

    public void onPostMsg(PostMsg postMsg, OmmProviderEvent providerEvent) {
    }

    public void onReissue(ReqMsg reqMsg, OmmProviderEvent providerEvent) {
    }

    public void onClose(ReqMsg reqMsg, OmmProviderEvent providerEvent) {
    }

    public void onAllMsg(Msg msg, OmmProviderEvent providerEvent) {
    }

    /**
     * Send a Login Refresh Response message back to a consumer
     * @param reqMsg received Login request message from Consumer
     * @param event identifies open item for which this message is received
     */
    void processLoginRequest(ReqMsg reqMsg, OmmProviderEvent event) {
        System.out.println("Provider: Login request accepted");
        event.provider().submit(EmaFactory.createRefreshMsg().domainType(EmaRdm.MMT_LOGIN).name(reqMsg.name()).nameType(EmaRdm.USER_NAME).
                complete(true).solicited(true).state(OmmState.StreamState.OPEN, OmmState.DataState.OK, OmmState.StatusCode.NONE, "Login accepted").
                attrib(EmaFactory.createElementList()), event.handle());

        System.out.println("Provider: Login refresh message sent");
    }

    /**
     * Send an Item Refresh Response message back to a consumer
     * @param reqMsg received Item request from Consumer
     * @param event identifies open item for which this message is received
     */
    void processMarketPriceRequest(ReqMsg reqMsg, OmmProviderEvent event) {
        if (itemHandle != 0) {
            processInvalidItemRequest(reqMsg, event);
            return;
        }

        System.out.println("Provider: Item request accepted");

        String utf8String = "伦敦证券交易所";
        // Set a byte array of RMTES three bytes escape sequence
        byte[] bytesOne = {0x1B, 0x25, 0x30};
        // Convert our UTF8 String to a byte array
        byte[] bytesTwo = utf8String.getBytes();
        // prepend 0x1B, 0x25, 0x30 to the UTF8 string
        byte[] byteRMTES = new byte[bytesOne.length + bytesTwo.length];
        ByteBuffer buffer = ByteBuffer.wrap(byteRMTES);
        buffer.put(bytesOne);
        buffer.put(bytesTwo);
        byteRMTES = buffer.array();
        // Convert the RMTES byte array string into a ByteBuffer
        ByteBuffer emaBuffer = ByteBuffer.wrap(byteRMTES);

        //Set Refresh Response OMM FieldList data
        FieldList fieldList = EmaFactory.createFieldList();
        fieldList.add(EmaFactory.createFieldEntry().ascii(3, reqMsg.name()));
        fieldList.add(EmaFactory.createFieldEntry().enumValue(15, 840));
        fieldList.add(EmaFactory.createFieldEntry().real(22, 3990, OmmReal.MagnitudeType.EXPONENT_NEG_2));
        fieldList.add(EmaFactory.createFieldEntry().real(25, 3994, OmmReal.MagnitudeType.EXPONENT_NEG_2));
        fieldList.add(EmaFactory.createFieldEntry().real(30, 9, OmmReal.MagnitudeType.EXPONENT_0));
        fieldList.add(EmaFactory.createFieldEntry().real(31, 19, OmmReal.MagnitudeType.EXPONENT_0));
        fieldList.add(EmaFactory.createFieldEntry().ascii(260, "Simplified Chinese")); //SEG_FORW
        fieldList.add(EmaFactory.createFieldEntry().rmtes(1352, emaBuffer));

        event.provider().submit(EmaFactory.createRefreshMsg().serviceName(reqMsg.serviceName()).name(reqMsg.name()).
                state(OmmState.StreamState.OPEN, OmmState.DataState.OK, OmmState.StatusCode.NONE, "Refresh Completed").solicited(true).
                payload(fieldList).complete(true), event.handle());

        itemHandle = event.handle();
        System.out.println("Provider: Item refresh message sent");
    }

    /**
     * Handle invalid consumer request messages
     * @param reqMsg received invalid Item request from Consumer
     * @param event identifies open item for which this message is received
     */
    void processInvalidItemRequest(ReqMsg reqMsg, OmmProviderEvent event) {
        event.provider().submit(EmaFactory.createStatusMsg().name(reqMsg.name()).serviceName(reqMsg.serviceName()).
                        domainType(reqMsg.domainType()).
                        state(OmmState.StreamState.CLOSED, OmmState.DataState.SUSPECT, OmmState.StatusCode.NOT_FOUND, "Item not found"),
                event.handle());
    }

}

/**
 * Main Provider class
 */
public class RMTESProvider {
    /**
     * Main method
     * @param args
     */
    public static void main(String[] args) {
        // Crate OmmProvider class
        OmmProvider provider = null;
        try {
            // Initiate Provider AppClient class
            AppClientProvider appClient = new AppClientProvider();

            FieldList fieldList = EmaFactory.createFieldList();
            UpdateMsg updateMsg = EmaFactory.createUpdateMsg();

            System.out.println("Provider: Start");
            // Start OMM Provider
            provider = EmaFactory.createOmmProvider(EmaFactory.createOmmIProviderConfig().operationModel(OmmIProviderConfig.OperationModel.USER_DISPATCH),
                    appClient);

            while (appClient.itemHandle == 0) {
                provider.dispatch(1000);
                Thread.sleep(1000);
            }

            String[]  utf8StringArray = {"伦敦证券交易所", "倫敦證券交易所" , "ロンドン証券取引所" ,"런던 증권 거래소","ตลาดหลักทรัพย์ลอนดอน" };
            String[]  asciiStringArray = {"Simplified Chinese", "Traditional Chinese" , "Japanese" ,"Korean", "Thai"};
            Random rand = new Random();
            int index_lang = 0;
            System.out.println("Provider: Sending Item update messages");
            // Sending Update messages
            for (int index = 0; index < 60; index++) {
                provider.dispatch(1000);

                index_lang = rand.nextInt(5);
                ByteBuffer emaBuffer = encodeRMTES(utf8StringArray[index_lang]);

                fieldList.clear();
                fieldList.add(EmaFactory.createFieldEntry().real(22, 3991 + index, OmmReal.MagnitudeType.EXPONENT_NEG_2));
                fieldList.add(EmaFactory.createFieldEntry().real(25, 3994 + index, OmmReal.MagnitudeType.EXPONENT_NEG_2));
                fieldList.add(EmaFactory.createFieldEntry().real(30, 10 + index, OmmReal.MagnitudeType.EXPONENT_0));
                fieldList.add(EmaFactory.createFieldEntry().real(31, 19 + index, OmmReal.MagnitudeType.EXPONENT_0));
                fieldList.add(EmaFactory.createFieldEntry().ascii(260, asciiStringArray[index_lang])); //SEG_FORW
                fieldList.add(EmaFactory.createFieldEntry().rmtes(1352, emaBuffer));

                provider.submit(updateMsg.clear().payload(fieldList), appClient.itemHandle);

                Thread.sleep(1000);
            }
        } catch (OmmException | InterruptedException excp) {
            System.out.println(excp.getMessage());
        } finally {
            if (provider != null) provider.uninitialize();
        }
    }

    /**
     * Create RMTES ByteBuffer with the input UTF8 String for the update messages
     * @param utf8Message input UTF8-String
     * @return outgoing ByteBuffer object to send as RMTES
     */
    private static ByteBuffer encodeRMTES (String utf8Message){
        // Set a byte array of RMTES three bytes escape sequence
        byte[] bytesOne = {0x1B, 0x25, 0x30};
        // Convert our UTF8 String to a byte array
        byte[] bytesTwo = utf8Message.getBytes();
        // prepend 0x1B, 0x25, 0x30 to the UTF8 string
        byte[] byteRMTES = new byte[bytesOne.length + bytesTwo.length];
        ByteBuffer buffer = ByteBuffer.wrap(byteRMTES);
        buffer.put(bytesOne);
        buffer.put(bytesTwo);
        byteRMTES = buffer.array();
        // Convert the RMTES byte array string into a ByteBuffer
        return ByteBuffer.wrap(byteRMTES);
    }
}
