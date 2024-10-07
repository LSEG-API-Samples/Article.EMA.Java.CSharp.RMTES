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

## <a id="prerequisite"></a>Prerequisite

Before I am going further, there is some prerequisite, dependencies, and libraries that the project is needed.

### Docker Desktop Application

You can build and run each EMA C#/Java Provider and Consumer applications manually. However, it is easier to build and run with a simple ```Docker``` compose command. 

The [Docker Desktop](https://www.docker.com/products/docker-desktop/) application is required to run all projects.

### Internet Access

The EMA C# library available on the [NuGet](https://www.nuget.org/) repository. While the EMA Java library is also available on the [Maven Central](https://central.sonatype.com/) repository.

This project download the EMA libraries over internet to build and run applications.

### EMA C# Projects Prerequisite

#### .NET SDK

Firstly, you need [.NET 8 SDK](https://dotnet.microsoft.com/en-us/download/dotnet/8.0).

Please check [How to check that .NET is already installed](https://learn.microsoft.com/en-us/dotnet/core/install/how-to-detect-installed-versions) to verify installed .NET versions on your machine.

#### Visual Studio Code or Visual Studio IDE

The EMA C# projects support both [VS Code](https://code.visualstudio.com/) editor and [Visual Studio 2022](https://visualstudio.microsoft.com/) IDE.

### EMA Java Project Prerequisite

#### Java SDK

For the Java project, you need Java SDK version 11, 17, or 21 (either Oracle JDK or OpenJDK). 

#### Apache

The Java project uses [Apache Maven](https://maven.apache.org/) as a project build automation tool. 

#### IntelliJ IDEA

The EMA Java project supports [IntelliJ IDEA](https://www.jetbrains.com/idea/) IDE. However, any IDEs or Editors that support [Maven Standard Directory Layout](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html) should be fine.

That covers the prerequisite of this RMTES project.

## How to run

To build and run the Provider and Consumer projects with Docker, please go to the *RMTES_EMACSharp* or *RMTES_EMAJava* folder via a command prompt and run the following [Docker Compose](https://docs.docker.com/compose/) command.

```bash
docker compose up
```
To stop the projects, use the following Docker Compose command inside the same folder.

```bash
docker compose down
```

Example Results (from EMA C# projects):

```bash
[+] Running 3/3
 ✔ Network emacsharp_rmtes_default       Created                                                                                                   0.1s 
 ✔ Container emacsharp_rmtes-provider-1  Created                                                                                                   0.1s 
 ✔ Container emacsharp_rmtes-consumer-1  Created                                                                                                   0.0s 
Attaching to consumer-1, provider-1
provider-1  | Provider: Start
consumer-1  | Consumer: Start
provider-1  | Provider: Login request accepted
consumer-1  | Consumer: Sending Item request
provider-1  | Provider: Item request accepted
provider-1  | Provider: Item refresh message sent
consumer-1  | Item Name: /LSEG.L
consumer-1  | Service Name: DIRECT_FEED
consumer-1  | Item State: Open / Ok / None / 'Refresh Completed'
consumer-1  | Fid: 22 Name = BID DataType: Real Value: 39.9
consumer-1  | Fid: 25 Name = ASK DataType: Real Value: 39.94
consumer-1  | Fid: 30 Name = BIDSIZE DataType: Real Value: 9
consumer-1  | Fid: 31 Name = ASKSIZE DataType: Real Value: 19
consumer-1  | Fid: 260 Name = SEG_FORW DataType: Ascii Value: Simplified Chinese
consumer-1  | Fid: 1352 Name = DSPLY_NMLL DataType: Rmtes Value: 伦敦证券交易所
consumer-1  | 
...
consumer-1  | Item Name: /LSEG.L
consumer-1  | Service Name: DIRECT_FEED
consumer-1  | Fid: 22 Name = BID DataType: Real Value: 39.92
consumer-1  | Fid: 30 Name = BIDSIZE DataType: Real Value: 11
consumer-1  | Fid: 260 Name = SEG_FORW DataType: Ascii Value: Korean
consumer-1  | Fid: 1352 Name = DSPLY_NMLL DataType: Rmtes Value: 런던 증권 거래소
consumer-1  | 
...
consumer-1  | Item Name: /LSEG.L
consumer-1  | Service Name: DIRECT_FEED
consumer-1  | Fid: 22 Name = BID DataType: Real Value: 39.94
consumer-1  | Fid: 30 Name = BIDSIZE DataType: Real Value: 13
consumer-1  | Fid: 260 Name = SEG_FORW DataType: Ascii Value: Japanese
consumer-1  | Fid: 1352 Name = DSPLY_NMLL DataType: Rmtes Value: ロンドン証券取引所
consumer-1  | 
...
consumer-1  | Item Name: /LSEG.L
consumer-1  | Service Name: DIRECT_FEED
consumer-1  | Fid: 22 Name = BID DataType: Real Value: 39.99
consumer-1  | Fid: 30 Name = BIDSIZE DataType: Real Value: 18
consumer-1  | Fid: 260 Name = SEG_FORW DataType: Ascii Value: Thai
consumer-1  | Fid: 1352 Name = DSPLY_NMLL DataType: Rmtes Value: ตลาดหลักทรัพย์ลอนดอน
consumer-1  | 
...
consumer-1  | Item Name: /LSEG.L
consumer-1  | Service Name: DIRECT_FEED
consumer-1  | Fid: 22 Name = BID DataType: Real Value: 40.04
consumer-1  | Fid: 30 Name = BIDSIZE DataType: Real Value: 23
consumer-1  | Fid: 260 Name = SEG_FORW DataType: Ascii Value: Traditional Chinese
consumer-1  | Fid: 1352 Name = DSPLY_NMLL DataType: Rmtes Value: 倫敦證券交易所
consumer-1  | 
...
```

## <a id="ref"></a>References

For further details, please check out the following resources:
- [Real-Time SDK C#](https://developers.lseg.com/en/api-catalog/refinitiv-real-time-opnsrc/refinitiv-real-time-csharp-sdk) and [Real-Time SDK Java](https://developers.lseg.com/en/api-catalog/refinitiv-real-time-opnsrc/rt-sdk-java) pages on the [LSEG Developer Community](https://developers.lseg.com/) website.
- [Real-Time SDK Family](https://developers.lseg.com/en/use-cases-catalog/refinitiv-real-time) page.
- [Real-Time SDK C# Quick Start](https://developers.lseg.com/en/api-catalog/refinitiv-real-time-opnsrc/refinitiv-real-time-csharp-sdk/quick-start).
- [Real-Time SDK Java Quick Start](https://developers.lseg.com/en/api-catalog/refinitiv-real-time-opnsrc/rt-sdk-java/quick-start).
- [Developer Article: 10 important things you need to know before you write an Enterprise Real Time application](https://developers.lseg.com/article/10-important-things-you-need-know-you-write-elektron-real-time-application).
- [Developer Webinar: Introduction to Enterprise App Creation With Open-Source Enterprise Message API](https://www.youtube.com/watch?v=2pyhYmgHxlU).
- [Encoding and Decoding non-ASCII text using EMA and RFA C++/.NET](https://developers.lseg.com/en/article-catalog/article/encoding-and-decoding-non-ascii-text-using-ema-and-rfa-cnet) article.

For any question related to this article or the RTSDK page, please use the Developer Community [Q&A Forum](https://community.developers.refinitiv.com/).

[TBD]