/*|-----------------------------------------------------------------------------
 *|            This source code is provided under the Apache 2.0 license
 *|  and is provided AS IS with no warranty or guarantee of fit for purpose.
 *|                See the project's LICENSE.Md for details.
 *|           Copyright (C) 2024 LSEG. All rights reserved.     
 *|-----------------------------------------------------------------------------
 */

namespace RMTESConsumer;

using LSEG.Ema.Access;
using System.Threading;
using System;
using static LSEG.Ema.Access.DataType;
using LSEG.Ema.Rdm;

/// <summary>
/// IOmmConsumerClient implemented class, for handling incoming messages from the API
/// </summary>
internal class AppClient : IOmmConsumerClient
{
    /// <summary>
    /// Handle incoming Refresh Response message from the API
    /// </summary>
    /// <param name="refreshMsg">received Refresh response message from the Provider</param>
    /// <param name="consumerEvent">identifies open item for which this message is received</param>
    public void OnRefreshMsg(RefreshMsg refreshMsg, IOmmConsumerEvent consumerEvent)
    {
        Console.WriteLine("Refresh Message:");
        Console.WriteLine($"Item Name: " + (refreshMsg.HasName ? refreshMsg.Name() : "<not set>"));
        Console.WriteLine($"Service Name: {(refreshMsg.HasServiceName ? refreshMsg.ServiceName() : "<not set>")}");

        Console.WriteLine($"Item State: {refreshMsg.State()}");

        if (DataType.DataTypes.FIELD_LIST == refreshMsg.Payload().DataType)
            Decode(refreshMsg.Payload().FieldList());

        Console.WriteLine();
    }

    /// <summary>
    /// Handle incoming Update Response messages from the API
    /// </summary>
    /// <param name="updateMsg">received Update response messages from the Provider</param>
    /// <param name="consumerEvent">identifies open item for which this message is received</param>
    public void OnUpdateMsg(UpdateMsg updateMsg, IOmmConsumerEvent consumerEvent)
    {
        Console.WriteLine("Update Message:");
        Console.WriteLine($"Item Name: " + (updateMsg.HasName ? updateMsg.Name() : "<not set>"));
        Console.WriteLine($"Service Name: {(updateMsg.HasServiceName ? updateMsg.ServiceName() : "<not set>")}");
 

        if (DataTypes.FIELD_LIST == updateMsg.Payload().DataType)
            Decode(updateMsg.Payload().FieldList());

        Console.WriteLine();
    }

    /// <summary>
    /// Handle incoming Status Response messages from the API
    /// </summary>
    /// <param name="statusMsg">received Status response messages from the Provider</param>
    /// <param name="consumerEvent">identifies open item for which this message is received</param>
    public void OnStatusMsg(StatusMsg statusMsg, IOmmConsumerEvent consumerEvent)
    {
        Console.WriteLine("Status Message:");
        Console.WriteLine($"Item Name: " + (statusMsg.HasName ? statusMsg.Name() : "<not set>"));
        Console.WriteLine($"Service Name: {(statusMsg.HasServiceName ? statusMsg.ServiceName() : "<not set>")}");


        if (statusMsg.HasState)
            Console.WriteLine($"Item State: {statusMsg.State()}");

        Console.WriteLine();
    }

    /// <summary>
    /// Decoding OMM Fieldlist object
    /// </summary>
    /// <param name="fieldList">incoming OMMFieldList object from the Provider</param>
    void Decode(FieldList fieldList)
    {
        foreach (FieldEntry fieldEntry in fieldList)
        {
            Console.Write($"Fid: {fieldEntry.FieldId} Name = {fieldEntry.Name} DataType: {DataType.AsString(fieldEntry.Load!.DataType)} Value: ");

            if (Data.DataCode.BLANK == fieldEntry.Code)
                Console.WriteLine(" blank");
            else
                // Handle each data type (fits to the type of requests FIDs)
                switch (fieldEntry.LoadType)
                {
                    case DataTypes.REAL:
                        Console.WriteLine(fieldEntry.OmmRealValue().AsDouble());
                        break;
                    case DataTypes.ASCII:
                        Console.WriteLine(fieldEntry.OmmAsciiValue());
                        break;
                    case DataTypes.ENUM:
                        Console.WriteLine(fieldEntry.HasEnumDisplay ? fieldEntry.EnumDisplay() : fieldEntry.EnumValue());
                        break;
                    case DataTypes.RMTES:
                        Console.WriteLine(fieldEntry.OmmRmtesValue());
                        break;
                    case DataTypes.ERROR:
                        Console.WriteLine($"({fieldEntry.OmmErrorValue().ErrorCodeAsString()})");
                        break;
                    default:
                        Console.WriteLine();
                        break;
                }
        }
    }
}
/// <summary>
/// Main Consumer class
/// </summary>
class Consumer
{
    /// <summary>
    /// Main method
    /// </summary>
    /// <param name="args"></param>
    static void Main(string[] args)
    {
        // Crate OmmConsumer class
        OmmConsumer? consumer = null;
        try
        {
            // Initiate Consumer AppClient class
            AppClient appClient = new();

            Console.WriteLine("Consumer: Start");
            // Init connection to the Provider
            OmmConsumerConfig config = new OmmConsumerConfig().UserName("user").ConsumerName("Consumer_1");
            consumer = new OmmConsumer(config);

            // Construct the View Request
            OmmArray array = new()
            {
                FixedWidth = 2
            };

            array.AddInt(15)
               .AddInt(22)
               .AddInt(25)
               .AddInt(260)
               .AddInt(1352)
               .Complete();

            var view = new ElementList()
                .AddUInt(EmaRdm.ENAME_VIEW_TYPE, 1)
                .AddArray(EmaRdm.ENAME_VIEW_DATA, array)
                .Complete();

            Console.WriteLine("Consumer: Sending Item request");
            //Send item request message with View
            consumer.RegisterClient(new RequestMsg().ServiceName("DIRECT_FEED").Name("/LSEG.L").Payload(view), appClient);
            Thread.Sleep(60000); // API calls OnRefreshMsg(), OnUpdateMsg() and OnStatusMsg()
        }
        catch (OmmException excp)
        {
            Console.WriteLine(excp.Message);
        }
        finally
        {
            consumer?.Uninitialize();
        }
    }
}
