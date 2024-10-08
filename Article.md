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

## RMTES Recap: What is RMTES?

There are some fields on the data dictionary (*RDMFieldDictionary*) that use the **RMTES_String** data type. This data type is designed to use with local language (non-ASCII text) such as Chinese, Korean, Thai, etc. 

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

## Publishing non-ASCII RMTES string in EMA application

Like my colleague's statement on the [original article](https://developers.lseg.com/en/article-catalog/article/encoding-and-decoding-non-ascii-text-using-ema-and-rfa-cnet), an OMM Publisher developers can just add the UTF-8 string with the three bytes escape sequences to the **FieldList** object  and then publish that data to the wire.

### EMA C# 

The code is based on the EMA C# [130_MP_UserDispatch](https://github.com/Refinitiv/Real-Time-SDK/tree/master/CSharp/Ema/Examples/Training/IProvider/100_Series/130_MP_UserDispatch) example.

I am modifying the *AppClientProvider class* ```ProcessMarketPriceRequest()``` method to publish the following text data to downstream components:

- the UTF-8 string via the *DSPLY_NMLL* field (FID 1352)
- the ASCII string via the *SEG_FORW* field (FID 260) as a meaning of DSPLY_NMLL data

The first step is to create a variable to store our UTF-8 string. And then prepend the **1B 25 30** escape sequence bytes to the UTF-8 string bytes array.

```C#
void ProcessMarketPriceRequest(RequestMsg reqMsg, IOmmProviderEvent providerEvent)
{
    ..
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

The EMA C# FieldList data supports RMTES via the ```AddRmtes(int fieldId,EmaBuffer value)``` method, so an application needs to convert the ```bytesRMTES``` variable to the ```EmaBuffer``` object before construct the Refresh Response message payload.

```C#
```