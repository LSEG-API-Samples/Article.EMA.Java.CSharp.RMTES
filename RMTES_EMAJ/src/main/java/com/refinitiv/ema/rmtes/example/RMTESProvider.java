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

class AppClientProvider implements OmmProviderClient {
    public long itemHandle = 0;

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

    void processLoginRequest(ReqMsg reqMsg, OmmProviderEvent event) {
        System.out.println("Provider: Login request accepted");
        event.provider().submit(EmaFactory.createRefreshMsg().domainType(EmaRdm.MMT_LOGIN).name(reqMsg.name()).nameType(EmaRdm.USER_NAME).
                complete(true).solicited(true).state(OmmState.StreamState.OPEN, OmmState.DataState.OK, OmmState.StatusCode.NONE, "Login accepted").
                attrib(EmaFactory.createElementList()), event.handle());

        System.out.println("Provider: Login refresh message sent");
    }

    void processMarketPriceRequest(ReqMsg reqMsg, OmmProviderEvent event) {
        if (itemHandle != 0) {
            processInvalidItemRequest(reqMsg, event);
            return;
        }

        System.out.println("Provider: Item request accepted");

        String utf8String = "伦敦证券交易所";

        byte[] bytesOne = {0x1B, 0x25, 0x30};
        byte[] bytesTwo = utf8String.getBytes();
        byte[] byteRMTES = new byte[bytesOne.length + bytesTwo.length];

        ByteBuffer buffer = ByteBuffer.wrap(byteRMTES);
        buffer.put(bytesOne);
        buffer.put(bytesTwo);
        byteRMTES = buffer.array();

        ByteBuffer emaBuffer = ByteBuffer.wrap(byteRMTES);

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

    void processInvalidItemRequest(ReqMsg reqMsg, OmmProviderEvent event) {
        event.provider().submit(EmaFactory.createStatusMsg().name(reqMsg.name()).serviceName(reqMsg.serviceName()).
                        domainType(reqMsg.domainType()).
                        state(OmmState.StreamState.CLOSED, OmmState.DataState.SUSPECT, OmmState.StatusCode.NOT_FOUND, "Item not found"),
                event.handle());
    }

}

public class RMTESProvider {
    public static void main(String[] args) {
        OmmProvider provider = null;
        try {
            AppClientProvider appClient = new AppClientProvider();
            FieldList fieldList = EmaFactory.createFieldList();
            UpdateMsg updateMsg = EmaFactory.createUpdateMsg();

            System.out.println("Provider: Start");
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

    private static ByteBuffer encodeRMTES (String utf8Message){
        byte[] bytesOne = {0x1B, 0x25, 0x30};
        byte[] bytesTwo = utf8Message.getBytes();
        byte[] byteRMTES = new byte[bytesOne.length + bytesTwo.length];

        ByteBuffer buffer = ByteBuffer.wrap(byteRMTES);
        buffer.put(bytesOne);
        buffer.put(bytesTwo);
        byteRMTES = buffer.array();

        return ByteBuffer.wrap(byteRMTES);
    }
}
