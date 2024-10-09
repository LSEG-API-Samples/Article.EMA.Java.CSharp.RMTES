/*|-----------------------------------------------------------------------------
 *|            This source code is provided under the Apache 2.0 license
 *|  and is provided AS IS with no warranty or guarantee of fit for purpose.
 *|                See the project's LICENSE.Md for details.
 *|           Copyright (C) 2024 LSEG. All rights reserved.     
 *|-----------------------------------------------------------------------------
 */

namespace RMTESProvider;

using System;
using System.Text;
using System.Threading;

using LSEG.Ema.Access;
using LSEG.Ema.Rdm;

/// <summary>
/// IOmmProviderClient implemented class, for handling incoming consumer request messages from the API
/// </summary>
internal class AppClientProvider: IOmmProviderClient
{
    public long ItemHandle = 0;
    /// <summary>
    /// Handle incoming consumer request messages (Login, Item, etc)
    /// <param name="reqMsg">received request messages from Consumer</param>
    /// <param name="providerEvent">identifies open item for which this message is received</param>
    /// </summary>
    public void OnReqMsg(RequestMsg reqMsg, IOmmProviderEvent providerEvent)
    {
        switch (reqMsg.DomainType())
        {
            case EmaRdm.MMT_LOGIN:
                ProcessLoginRequest(reqMsg, providerEvent);
                break;
            case EmaRdm.MMT_MARKET_PRICE:
                ProcessMarketPriceRequest(reqMsg, providerEvent);
                break;
            default:
                ProcessInvalidItemRequest(reqMsg, providerEvent);
                break;
        }
    }

    /// <summary>
    /// Send an Item Refresh Response message back to a consumer
    /// <param name="reqMsg">received Login request message from Consumer</param>
    /// <param name="providerEvent">identifies open item for which this message is received</param>
    /// </summary>
    void ProcessLoginRequest(RequestMsg reqMsg, IOmmProviderEvent providerEvent)
    {
        Console.WriteLine("Provider: Login request accepted");
        providerEvent.Provider.Submit(new RefreshMsg().DomainType(EmaRdm.MMT_LOGIN)
            .Name(reqMsg.Name()).NameType(EmaRdm.USER_NAME)
            .Complete(true).Solicited(true)
            .State(OmmState.StreamStates.OPEN, OmmState.DataStates.OK, OmmState.StatusCodes.NONE, "Login accepted"),
            providerEvent.Handle);
    }

    /// <summary>
    /// Send a Login Refresh Response message back to a consumer
    /// <param name="reqMsg">received Item request from Consumer</param>
    /// <param name="providerEvent">identifies open item for which this message is received</param>
    /// </summary>
    void ProcessMarketPriceRequest(RequestMsg reqMsg, IOmmProviderEvent providerEvent)
    {
        if (ItemHandle != 0)
        {
            ProcessInvalidItemRequest(reqMsg, providerEvent);
            return;
        }

        Console.WriteLine("Provider: Item request accepted");

        string utf8String = "伦敦证券交易所"; //Simplified Chinese
        // Set a byte array of RMTES three bytes escape sequence
        var bytesOne = new byte[] { 0x1B, 0x25, 0x30 };
        // Convert our UTF8 String to a byte array
        var bytesTwo = Encoding.UTF8.GetBytes(utf8String);
        // prepend 0x1B, 0x25, 0x30 to the UTF8 string
        byte[] byteRMTES = new byte[bytesOne.Length + bytesTwo.Length];
        for (int i = 0; i < byteRMTES.Length; ++i)
        {
            byteRMTES[i] = i < bytesOne.Length ? bytesOne[i] : bytesTwo[i - bytesOne.Length];
        }
        // Convert the RMTES byte array string into a EmaBufer object
        EmaBuffer emaBuffer = new();
        emaBuffer.CopyFrom(byteRMTES);

        FieldList fieldList = new FieldList();
        fieldList.AddAscii(3, reqMsg.Name()); //DSPLY_NAME
        fieldList.AddEnumValue(15, 840); //CURRENCY 
        fieldList.AddReal(22, 3990, OmmReal.MagnitudeTypes.EXPONENT_NEG_2); //BID   
        fieldList.AddReal(25, 3994, OmmReal.MagnitudeTypes.EXPONENT_NEG_2); //ASK
        fieldList.AddReal(30, 9, OmmReal.MagnitudeTypes.EXPONENT_0); //BIDSIZE
        fieldList.AddReal(31, 19, OmmReal.MagnitudeTypes.EXPONENT_0); //ASKSIZE
        fieldList.AddAscii(260, "Simplified Chinese"); //SEG_FORW 
        fieldList.AddRmtes(1352, emaBuffer); //DSPLY_NMLL

        providerEvent.Provider.Submit(new RefreshMsg()
            .Name(reqMsg.Name()).ServiceName(reqMsg.ServiceName())
            .Solicited(true)
            .State(OmmState.StreamStates.OPEN, OmmState.DataStates.OK, OmmState.StatusCodes.NONE, "Refresh Completed")
            .Payload(fieldList.Complete()).Complete(true),
            providerEvent.Handle);

        ItemHandle = providerEvent.Handle;
        Console.WriteLine("Provider: Item refresh message sent");
    }

    /// <summary>
    /// Handle invalid consumer request messages
    /// <param name="reqMsg">received invalid Item request from Consumer</param>
    /// <param name="providerEvent">identifies open item for which this message is received</param>
    /// </summary>
    void ProcessInvalidItemRequest(RequestMsg reqMsg, IOmmProviderEvent providerEvent)
    {
        providerEvent.Provider.Submit(new StatusMsg()
            .Name(reqMsg.Name()).ServiceName(reqMsg.ServiceName())
            .State(OmmState.StreamStates.CLOSED, OmmState.DataStates.SUSPECT, OmmState.StatusCodes.NOT_FOUND, "Item not found"),
            providerEvent.Handle);
    }
}

/// <summary>
/// Main Provider class
/// </summary>
class Provider
{
    /// <summary>
    /// Main method
    /// </summary>
    /// <param name="args"></param>
    static void Main(string[] args)
    {
        // Crate OmmProvider class
        OmmProvider? provider = null;
        try
        {
            // Initiate Provider AppClient class
            AppClientProvider appClient = new AppClientProvider();

            FieldList fieldList = new FieldList();
            OmmIProviderConfig config = new OmmIProviderConfig();

            Console.WriteLine("Provider: Start");
            // Start OMM Provider
            provider = new OmmProvider(config.OperationModel(OmmIProviderConfig.OperationModelMode.USER_DISPATCH).Port("14002"), appClient);

            while (appClient.ItemHandle == 0)
            {
                provider.Dispatch(1000);
                Thread.Sleep(1000);
            }
            int count = 0;
            var endTime = System.DateTime.Now + TimeSpan.FromMilliseconds(60_000);

            string[] utf8StringArray = { "伦敦证券交易所", "倫敦證券交易所", "ロンドン証券取引所", "런던 증권 거래소", "ตลาดหลักทรัพย์ลอนดอน" };
            string[] asciiStringArray = { "Simplified Chinese", "Traditional Chinese", "Japanese", "Korean", "Thai" };
            Random rnd = new Random();
            int index_lang = 0;
            EmaBuffer emaBuffer = new();
            Console.WriteLine("Provider: Sending Item update messages");
            
            while (DateTime.Now < endTime)
            {
                provider.Dispatch(1000);

                index_lang = rnd.Next(5);
                emaBuffer = EncodeRMTES(utf8StringArray[index_lang], emaBuffer);
                fieldList.Clear();
                fieldList.AddReal(22, 3991 + count, OmmReal.MagnitudeTypes.EXPONENT_NEG_2); //BID 
                fieldList.AddReal(25, 3994 + count, OmmReal.MagnitudeTypes.EXPONENT_NEG_2); //ASK
                fieldList.AddReal(30, 10 + count, OmmReal.MagnitudeTypes.EXPONENT_0); //BIDSIZE
                fieldList.AddReal(31, 19 + count, OmmReal.MagnitudeTypes.EXPONENT_0); //ASKSIZE
                fieldList.AddAscii(260, asciiStringArray[index_lang]); //SEG_FORW 
                fieldList.AddRmtes(1352, emaBuffer); //DSPLY_NMLL

                provider.Submit(new UpdateMsg().Payload(fieldList.Complete()), appClient.ItemHandle);
                count++;

                Thread.Sleep(1000);
            }

        }
        catch (OmmException excp)
        {
            Console.WriteLine(excp.Message);
        }
        finally
        {
            provider?.Uninitialize();
        }
    }
    /// <summary>
    /// Create RMTES ByteBuffer with the input UTF8 String for the update messages
    /// </summary>
    /// <param name="utf8Message">input UTF8-String</param>
    /// <param name="emaBuffer">outgoing EmaBuffer object to send as RMTES</param>
    /// <returns></returns>
    private static EmaBuffer EncodeRMTES (string utf8Message, EmaBuffer emaBuffer)
    {
        // Set a byte array of RMTES three bytes escape sequence
        var bytesOne = new byte[] { 0x1B, 0x25, 0x30 };
        // Convert our UTF8 String to a byte array
        var bytesTwo = Encoding.UTF8.GetBytes(utf8Message);
        // prepend 0x1B, 0x25, 0x30 to the UTF8 string
        byte[] byteRMTES = new byte[bytesOne.Length + bytesTwo.Length];
        for (int i = 0; i < byteRMTES.Length; ++i)
        {
            byteRMTES[i] = i < bytesOne.Length ? bytesOne[i] : bytesTwo[i - bytesOne.Length];
        }
        // Convert the RMTES byte array string into a EmaBufer object
        emaBuffer.Clear();
        return emaBuffer.CopyFrom(byteRMTES);
    }
}
