package com.refinitiv.ema.rmtes.example;

import com.refinitiv.ema.access.*;
import com.refinitiv.ema.rdm.EmaRdm;

class AppClientConsumer implements OmmConsumerClient {
    public void onRefreshMsg(RefreshMsg refreshMsg, OmmConsumerEvent event) {
        System.out.println("Refresh Message: ");
        System.out.printf("Item Name: %s%n", (refreshMsg.hasName() ? refreshMsg.name() : "<not set>") );
        System.out.printf("Service Name: %s%n", (refreshMsg.hasServiceName() ? refreshMsg.serviceName() : "<not set>"));

        System.out.printf("Item State: %s%n", refreshMsg.state());


        if (DataType.DataTypes.FIELD_LIST == refreshMsg.payload().dataType())
            decode(refreshMsg.payload().fieldList());

        System.out.println();
    }

    public void onUpdateMsg(UpdateMsg updateMsg, OmmConsumerEvent event) {
        System.out.println("Update Message: ");
        System.out.printf("Item Name: %s%n", (updateMsg.hasName() ? updateMsg.name() : "<not set>"));
        System.out.printf("Service Name: %s%n", (updateMsg.hasServiceName() ? updateMsg.serviceName() : "<not set>"));

        if (DataType.DataTypes.FIELD_LIST == updateMsg.payload().dataType())
            decode(updateMsg.payload().fieldList());

        System.out.println();
    }

    public void onStatusMsg(StatusMsg statusMsg, OmmConsumerEvent event) {
        System.out.println("Status Message: ");
        System.out.printf("Item Name: %s%n", (statusMsg.hasName() ? statusMsg.name() : "<not set>"));
        System.out.printf("Service Name: %s%n", (statusMsg.hasServiceName() ? statusMsg.serviceName() : "<not set>"));

        if (statusMsg.hasState())
            System.out.println("Item State: " +statusMsg.state());

        System.out.println();
    }

    public void onGenericMsg(GenericMsg genericMsg, OmmConsumerEvent consumerEvent) {
    }

    public void onAckMsg(AckMsg ackMsg, OmmConsumerEvent consumerEvent) {
    }

    public void onAllMsg(Msg msg, OmmConsumerEvent consumerEvent) {
    }

    void decode(FieldList fieldList) {
        fieldList.forEach(fieldEntry -> {
            System.out.printf("Fid %d Name = %s DataType: %s Value: ", fieldEntry.fieldId(), fieldEntry.name(), DataType.asString(fieldEntry.load().dataType()));
            if (Data.DataCode.BLANK == fieldEntry.code())
                System.out.println(" blank");
            else
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

public class RMTESConsumer {



    public static void main(String[] args) {
        OmmConsumer consumer = null;
        try {
            AppClientConsumer appClient = new AppClientConsumer();

            OmmConsumerConfig config = EmaFactory.createOmmConsumerConfig();

            consumer = EmaFactory.createOmmConsumer(config.host("localhost:14002").username("user"));

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

            ReqMsg reqMsg = EmaFactory.createReqMsg();

            consumer.registerClient(reqMsg.serviceName("DIRECT_FEED").name("/LSEG.L").payload(view), appClient);

            Thread.sleep(6000000);
        } catch (InterruptedException | OmmException excp) {
            System.out.println(excp.getMessage());
        } finally {
            if (consumer != null) consumer.uninitialize();
        }
    }
}
