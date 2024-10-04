# Encoding and Decoding non-ASCII text using EMA C# and Java

## Overview

This article is a sequel to my colleague's [Encoding and Decoding non-ASCII text using EMA and RFA C++/.NET](https://developers.lseg.com/en/article-catalog/article/encoding-and-decoding-non-ascii-text-using-ema-and-rfa-cnet). While that article describes how to encoding and decoding RMTES String data with the [EMA C++ API](https://developers.lseg.com/en/api-catalog/refinitiv-real-time-opnsrc/rt-sdk-cc), the part two article shows the same process with the [RTSDK C#](https://developers.lseg.com/en/api-catalog/refinitiv-real-time-opnsrc/refinitiv-real-time-csharp-sdk) and [RTSDK Java](https://developers.lseg.com/en/api-catalog/refinitiv-real-time-opnsrc/rt-sdk-java).

## How to run

### EMA C# Provider - Consumer projects

```bash
docker compose up
```
To stop

```bash
docker compose down
```

To start just a Provider
```bash
docker build . -t emaprovider
docker run -it --name emaprovider -p 14002:14002 emaprovider 
```

To start just a Consumer
```bash
docker build . -t emaconsumer
docker run -it --name emaconsumer emaconsumer
```

Example Results:

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


[TBD]