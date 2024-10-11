# Encoding and Decoding non-ASCII text using EMA C# and Java

- version: 1.0.1
- Last Update: Oct 2024
- Environment:  or Windows
- Compiler: .NET 8.0 and Java SDK 11
- Prerequisite: [prerequisite](#prerequisite)

Example Code Disclaimer:
ALL EXAMPLE CODE IS PROVIDED ON AN “AS IS” AND “AS AVAILABLE” BASIS FOR ILLUSTRATIVE PURPOSES ONLY. REFINITIV MAKES NO REPRESENTATIONS OR WARRANTIES OF ANY KIND, EXPRESS OR IMPLIED, AS TO THE OPERATION OF THE EXAMPLE CODE, OR THE INFORMATION, CONTENT, OR MATERIALS USED IN CONNECTION WITH THE EXAMPLE CODE. YOU EXPRESSLY AGREE THAT YOUR USE OF THE EXAMPLE CODE IS AT YOUR SOLE RISK.

## Overview

This article is a sequel to my colleague's [Encoding and Decoding non-ASCII text using EMA and RFA C++/.NET](https://developers.lseg.com/en/article-catalog/article/encoding-and-decoding-non-ascii-text-using-ema-and-rfa-cnet). While that article describes how to encoding and decoding RMTES String data with the [EMA C++](https://developers.lseg.com/en/api-catalog/refinitiv-real-time-opnsrc/rt-sdk-cc) and the legacy RFA C++ APIs, the part two article shows the same process with the strategic [EMA C#](https://developers.lseg.com/en/api-catalog/refinitiv-real-time-opnsrc/refinitiv-real-time-csharp-sdk) and [EMA Java](https://developers.lseg.com/en/api-catalog/refinitiv-real-time-opnsrc/rt-sdk-java) APIs.

I am demonstrating with RTSDK Java 2.2.2.L1 (EMA Java 3.8.2.0) and RTSDK C# 2.2.2.L1 (EMA C# 3.3.3.0). 

## RMTES Recap: What is RMTES data?

Let me start by giving you a recap on what the RMTES data is.There are some fields on the data dictionary (*RDMFieldDictionary*) that use the **RMTES_String** data type. This data type is designed to use with local language (non-ASCII text) such as Chinese, Korean, Thai, etc. 

Example RMTES_String field:

```ini
DSPLY_NMLL "LCL LANG DSP NM"     1352  NULL        ALPHANUMERIC       32  RMTES_STRING    32
```

### RMTES Encoding

RMTES uses [ISO 2022](https://www.iso20022.org/) escape sequences to select the character sets used. RMTES provides support for Reuters Basic Character Set (RBCS), UTF-8, Japanese Latin and Katakana (JIS C 6220 - 1969), Japanese Kanji (JIS X 0208 - 1990), and Chinese National Standard (CNS 11643-1986). RMTES also supports sequences for character repetition and sequences for partial updates. 

Although there is no open RMTES encoder library provide for external developers, they can use the switching function provided for encoding RMTES string and switching from default ISO 2022 scheme to UTF-8 character set. That mean developers can use the UTF-8 character set to publish data for RMTES field type and then publish that string to the Real-Time system.

The switching function uses the first three bytes of the text as the escape sequence. The Real-Time Distribution System components use them for the text encoded with UTF-8. Those three bytes are **1B 25 30** as follows:

```txt
0x1B 0x25 0x30
```

An application can prepend 0x1B, 0x25, 0x30 to the UTF8 string and encode that way as an RMTES type. The escape sequence characters indicate to the RMTES parser or decoder that it’s supposed to be a UTF-8 string.

Please note that you need to be very careful with using that three-byte string, as it can cause the UTF-8 string to be longer than the cached dictionary values which are the size of RWF LEN column in a byte from RDMFieldDictionary. It can cause display issues if they’re going through the infra.

The non-ASCII character such as Chinese, Thai, Japanese and Korea language can be used UTF-8 character set, therefore, the application can use this way to encode the non-ASCII text instead. We will talk about the implementation in EMA C# and Java in the next section.

That covers the overview of RMTES data.

## <a id="pub_rmtes_csharp"></a>Publishing non-ASCII RMTES string in EMA application

So, now let’s look at how to publish the RMTES string data to downstream applications (either consumers or RTDS components).

Like my colleague's statement on the [original article](https://developers.lseg.com/en/article-catalog/article/encoding-and-decoding-non-ascii-text-using-ema-and-rfa-cnet), an OMM Publisher developers can just add the UTF-8 string with the three bytes escape sequences to the **FieldList** object  and then publish that data to the wire.

### EMA C# 

So, I will start off with the EMA C# OMM Provider application. The code is based on the EMA C# [130_MP_UserDispatch](https://github.com/Refinitiv/Real-Time-SDK/tree/master/CSharp/Ema/Examples/Training/IProvider/100_Series/130_MP_UserDispatch) example.

I am modifying the *AppClientProvider class* ```ProcessMarketPriceRequest()``` method to publish the following text data to downstream components:

- the UTF-8 string via the *DSPLY_NMLL* field (FID 1352)
- the ASCII string via the *SEG_FORW* field (FID 260) as a meaning of DSPLY_NMLL data

The first step is to create a variable to store our UTF-8 string. And then prepend the **1B 25 30** escape sequence bytes to the UTF-8 string bytes array.

```C#
void ProcessMarketPriceRequest(RequestMsg reqMsg, IOmmProviderEvent providerEvent)
{
    // ...
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
}
```

Now, the ```byteRMTES``` variable stores the RMTES data and ready to publish to downstream applications. 

The EMA C# FieldList object supports RMTES via the ```AddRmtes(int fieldId,EmaBuffer value)``` method, so an application needs to convert the ```bytesRMTES``` variable to the ```EmaBuffer``` object before construct the Refresh Response message payload.

```C#
// Convert the RMTES byte array string into a EmaBuffer
EmaBuffer emaBuffer = new();
emaBuffer.CopyFrom(byteRMTES);

FieldList fieldList = new FieldList();
//Additional ENUM, REAL fields
// ...
fieldList.AddAscii(260, "Simplified Chinese"); //SEG_FORW 
fieldList.AddRmtes(1352, emaBuffer); //DSPLY_NMLL

providerEvent.Provider.Submit(new RefreshMsg()
    .Name(reqMsg.Name()).ServiceName(reqMsg.ServiceName())
    .Solicited(true)
    .State(OmmState.StreamStates.OPEN, OmmState.DataStates.OK, OmmState.StatusCodes.NONE, "Refresh Completed")
    .Payload(fieldList.Complete()).Complete(true),
    providerEvent.Handle);

ItemHandle = providerEvent.Handle;
```

I also modified the *Provider.cs* file to send UTF-8 String for Simplified Chinese, Traditional Chinese, Japanese, Korean, and Thai languages messages randomly to downstream applications via the Update messages.

```C#
static void Main(string[] args)
{
    ..
    string[] utf8StringArray = { "伦敦证券交易所", "倫敦證券交易所", "ロンドン証券取引所", "런던 증권 거래소", "ตลาดหลักทรัพย์ลอนดอน" };
    string[] asciiStringArray = { "Simplified Chinese", "Traditional Chinese", "Japanese", "Korean", "Thai" };
    Random rnd = new Random();
    int index_lang = 0;
    EmaBuffer emaBuffer = new();
            
    while (DateTime.Now < endTime)
    {
        provider.Dispatch(1000);

        index_lang = rnd.Next(5);
        emaBuffer = EncodeRMTES(utf8StringArray[index_lang], emaBuffer);
        fieldList.Clear();
        // ...
        fieldList.AddAscii(260, asciiStringArray[index_lang]); //SEG_FORW 
        fieldList.AddRmtes(1352, emaBuffer); //DSPLY_NMLL

        provider.Submit(new UpdateMsg().Payload(fieldList.Complete()), appClient.ItemHandle);
        count++;
        // ...
    }   
    ..
}

// Create RMTES ByteBuffer with the input UTF8 String for the update messages
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
    // Convert the RMTES byte array string into a ByteBuffer
    emaBuffer.Clear();
    return emaBuffer.CopyFrom(byteRMTES);
}
```

That covers the EMA C# for publishing RMTES data.

### EMA Java 

Moving on to the EMA Java. The code is based on the EMA Java [ex130_MP_UserDispatch](https://github.com/Refinitiv/Real-Time-SDK/tree/master/Java/Ema/Examples/src/main/java/com/refinitiv/ema/examples/training/iprovider/series100/ex130_MP_UserDispatch) example which is equivalent to the EMA C# API. The modification on the *RMTESProvider.java* has the same business logic as the EMA C# *Provider.cs* example explained above.

The first step is to create a variable to store our UTF-8 string. And then prepend the **1B 25 30** escape sequence bytes to the UTF-8 string bytes array.

```Java
void processMarketPriceRequest(ReqMsg reqMsg, OmmProviderEvent event) {
    // ...
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
}
```

The EMA Java FieldList object supports RMTES via the ```FieldEntry.rmtes​(int fieldId, java.nio.ByteBuffer value)``` method, so an application needs to convert the ```bytesRMTES``` variable to the Java ```ByteBuffer``` object before construct the Refresh Response message payload.

```Java
// Convert the RMTES byte array string into a ByteBuffer
ByteBuffer emaBuffer = ByteBuffer.wrap(byteRMTES);

//Set Refresh Response OMM FieldList data
FieldList fieldList = EmaFactory.createFieldList();
//Additional ENUM, REAL fields
fieldList.add(EmaFactory.createFieldEntry().ascii(260, "Simplified Chinese")); //SEG_FORW
fieldList.add(EmaFactory.createFieldEntry().rmtes(1352, emaBuffer)); //DSPLY_NMLL

event.provider().submit(EmaFactory.createRefreshMsg().serviceName(reqMsg.serviceName()).name(reqMsg.name()).
        state(OmmState.StreamState.OPEN, OmmState.DataState.OK, OmmState.StatusCode.NONE, "Refresh Completed").solicited(true).
        payload(fieldList).complete(true), event.handle());

itemHandle = event.handle();
```

The *RMTESProvider.java* file also has been added a logic to send UTF-8 String for Simplified Chinese, Traditional Chinese, Japanese, Korean, and Thai languages messages randomly to downstream applications via the Update messages. The source code logic is the same as the EMA C# API. Please find more detail on the source code project on the [GitHub repository](https://github.com/LSEG-API-Samples/Article.EMA.Java.CSharp.RMTES/tree/main/RMTES_EMAJ).

That covers how to publish the RMTES string data.

## <a id="decode_rmtes_csharp"></a>Decoding non-ASCII RMTES string in EMA Consumer application

That brings us to how to decode the RMTES data on the consumer side. The EMA APIs (C++, Java, and C#) generally provided RMTES converter or parser interface for converting the encoded RMTES string payload received as part of the OMM data to a Unicode string. It helps display news in international languages with UCS2 format or transfer data through the network in ISO 2022 and UTF-8. The following section will provide a guideline for applications that want to display non-ASCII string correctly.

### Displaying non-ASCII RMTES string in EMA C# Consumer application

Let’s start with the EMA C# consumer application. The Consumer *Consumer.cs* class is based on the EMA C# [310_MP_Rmtes](https://github.com/Refinitiv/Real-Time-SDK/tree/master/CSharp/Ema/Examples/Training/Consumer/300_Series/310_MP_Rmtes) and [360_MP_View](https://github.com/Refinitiv/Real-Time-SDK/tree/master/CSharp/Ema/Examples/Training/Consumer/300_Series/360_MP_View) examples. 

To decode incoming RMTES data, an application can use the ```FieldEntry.OmmRmtesValue()``` method to get the OMM RMTES data:

```C#
// Consumer.cs
// Decoding FieldEntry
foreach (FieldEntry fieldEntry in fieldList)
{
    Console.Write($"Fid: {fieldEntry.FieldId} Name = {fieldEntry.Name} DataType: {DataType.AsString(fieldEntry.Load!.DataType)} Value: ");

    if (Data.DataCode.BLANK == fieldEntry.Code)
                Console.WriteLine(" blank");
    else
        // Handle each data type (fits to the type of requests FIDs)
        switch (fieldEntry.LoadType)
        {
            // ...
            case DataTypes.RMTES:
                Console.WriteLine(fieldEntry.OmmRmtesValue());
                break;
            default:
                Console.WriteLine();
                break;
        }
}
```

If an application needs to work with partial RMTES updates, developers can cache RMTES data from the ```FieldEntry.OmmRmtesValue()``` method to the ```RmtesBuffer ``` objects and apply all received changes to them. Please refer to [EMA C# Documents](https://developers.lseg.com/en/api-catalog/refinitiv-real-time-opnsrc/refinitiv-real-time-csharp-sdk/documentation#message-api-c-development-guides) for more information about RmtesBuffer  class.

```C#
// Consumer.cs
// Decoding FieldEntry
private readonly RmtesBuffer rmtesBuffer = new(new byte[0]);
//..
// In the below loop partial updates for the specific field of RMTES type are handled.
// Note that in case it is necessary to handle partial updates for multiple fields,
// the application has to cache each RMTES string in a separate RmtesBuffer
// (e.g., use a hashmap to track RmtesBuffer instances corresponding to specific FIDs)
// and apply the updates accordingly.
foreach (FieldEntry fieldEntry in fieldList)
{
    // ...
    if (Data.DataCode.BLANK == fieldEntry.Code)
                Console.WriteLine(" blank");
    else
        // Handle each data type (fits to the type of requests FIDs)
        switch (fieldEntry.LoadType)
        {
            // ...
            case DataTypes.RMTES:
                // If an application just cache RMTESBuffer objects and apply all received changes to them.
                Console.WriteLine(rmtesBuffer.Apply(fieldEntry.OmmRmtesValue()).ToString());
                break;
            default:
                Console.WriteLine();
                break;
        }
}
```
### Displaying non-ASCII RMTES string in EMA Java Consumer application

My next point is the EMA Java API. The Consumer *RMTESConsumer.java* class is based on the EMA Java [ex310_MP_Rmtes](https://github.com/Refinitiv/Real-Time-SDK/tree/master/Java/Ema/Examples/src/main/java/com/refinitiv/ema/examples/training/consumer/series300/ex310_MP_Rmtes) and [ex360_MP_View](https://github.com/Refinitiv/Real-Time-SDK/tree/master/Java/Ema/Examples/src/main/java/com/refinitiv/ema/examples/training/consumer/series300/ex360_MP_View) examples. 

Developers can use the EMA Java ```FieldEntry.rmtes()``` method to decode incoming RMTES data as follows.

```Java
// RMTESConsumer.java
// Decoding FieldEntry
fieldList.forEach(fieldEntry -> {
    //..
    if (Data.DataCode.BLANK == fieldEntry.code())
        System.out.println(" blank");
    else
        //Handle each data type (fits to the type of requests FIDs)
        switch (fieldEntry.loadType()) {
            //...
            case DataType.DataTypes.RMTES:
                System.out.println(fieldEntry.rmtes());
                break;
            default:
                System.out.println();
                break;
        }
});
```

Like the C# counterpart, you can cache RMTES data from the ```FieldEntry.rmtes()``` method to the ```RmtesBuffer ``` objects and apply all received changes to them if an application needs to work with partial RMTES updates.

If an application needs to work with partial RMTES updates, developers can cache RMTES data from the ```FieldEntry.OmmRmtesValue()``` method to the ```RmtesBuffer ``` objects and apply all received changes to them. Please refer to [EMA Java Documents](https://developers.lseg.com/en/api-catalog/refinitiv-real-time-opnsrc/rt-sdk-java/documentation#message-api-java-development-and-configuration-guides-with-examples) for more information about RmtesBuffer class.

```Java
// RMTESConsumer.java
// Decoding FieldEntry
import com.refinitiv.ema.access.RmtesBuffer;

private RmtesBuffer rmtesBuffer = EmaFactory.createRmtesBuffer();
//...
// In the below loop partial updates for the specific field of RMTES type are handled.
// Note that in case it is necessary to handle partial updates for multiple fields,
// the application has to cache each RMTES string in a separate RmtesBuffer
// (e.g., use a hashmap to track RmtesBuffer instances corresponding to specific FIDs)
// and apply the updates accordingly.
fieldList.forEach(fieldEntry -> {
    //..
    if (Data.DataCode.BLANK == fieldEntry.code())
        System.out.println(" blank");
    else
        //Handle each data type (fits to the type of requests FIDs)
        switch (fieldEntry.loadType()) {
            //...
            case DataType.DataTypes.RMTES:
                // If an application just cache RMTESBuffer objects and apply all received changes to them.
                System.out.println((rmtesBuffer.apply(fieldEntry.rmtes())).toString());
                break;
            default:
                System.out.println();
                break;
        }
});
```

The example result of the EMA Java ```System.out.println(fieldEntry.rmtes());``` and C# ```Console.WriteLine(fieldEntry.OmmRmtesValue());``` methods will be as follows:

```bash
consumer-1  | Fid 1352 Name = DSPLY_NMLL DataType: Rmtes Value: 런던 증권 거래소
...
consumer-1  | Fid 1352 Name = DSPLY_NMLL DataType: Rmtes Value: ตลาดหลักทรัพย์ลอนดอน
...
consumer-1  | Fid 1352 Name = DSPLY_NMLL DataType: Rmtes Value: 倫敦證券交易所
```

That’s all I have to say about how to decode RMTES data on the EMA Consumer application.

## <a id="conclusion"></a>Conclusion

The LSEG Real-Time platform uses RMTES data to store non-ASCII text data such as Chinese, Japanese, Korean, etc. languages. Developers can use the UTF-8 string with extra 3 bytes escape sequence prefix to encode that data and publish to downstream components as the RMTES data. 

The EMA API (either C++, Java, or C#) provides interfaces to encode and decode the RMTES data with just a few lines of code. 

That covers all I wanted to tell you about how to encode and decode non-ASCII text data via RMTES with the EMA APIs.