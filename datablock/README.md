# Datablock internal format

## Document and Chunks

A document contain an header and some chunks.

A chunk contain an header and some data (binary), limited to $\dfrac{2^{32}}{2}-1$ bytes (max Java signed integer).

Numbers in headers are **big endian** coded (as default in Java's ByteBuffers).

### Document format

```
________________________________________________________________________________________________________________________________________
| MAGIC NUMBER | DOCUMENT TYPE | TYPE VERSION | DOCUMENT VERSION | BLANK [| CHUNK0 | CHUNK SEPARATOR [| CHUNKn | CHUNK SEPARATOR ]...] | EOF
|    BYTES     |     BYTES     |    SHORT     |      INT         | BYTES  | CHUNK  |      BYTES       | CHUNK  |        BYTES          |
|<---- 8 ----->|<----- 8 ----->|<---- 2 ----->|<------ 4 ------->|<- 2 -->|        |<------ 1 ------->|        |<-------- 1 ---------->|
________________________________________________________________________________________________________________________________________
```

With:

| DATA TYPE | USE CASE |
| --- | --- |
| MAGIC NUMBER | To identify the file with "magic" tools. Should tie in with file extension |
| DOCUMENT TYPE | To identify the kind of managed document |
| TYPE VERSION |  To identify the parser version to manage this document |
| DOCUMENT VERSION | To keep a trace if the document change (overwrite) with a new version. |
| BLANK | To separate document header and first chunk (0 byte) |
| CHUNK | Chunk internal format | To store chunks |
| CHUNK SEPARATOR | To mark chunks and protect document integrity (0 byte) |

### Chunk format

```
_______________________________________________________________________________________
| FOURCC | VERSION | PAYLOAD SIZE | CREATED DATE | STATUS TAG | BLANK  | DATA PAYLOAD |
| ASCII  |  SHORT  |     INT      |    LONG      |    BYTE    | BYTES  |    BYTES     |
|<- 4 -->|<-- 2 -->|<---- 4 ----->|<---- 8 ----->|<--- 1 ---->|<- 13 ->|<PAYLOAD SIZE>| 
_______________________________________________________________________________________
```

With:

| DATA TYPE | USE CASE |
| --- | --- |
| FOURCC | 4 ASCII bytes to identify and route to process the chunk |
| VERSION | Chunk type version |
| PAYLOAD SIZE | Data payload size |
| CREATED DATE | Unix time (UTC to milliseconds) when the chunk are created |
| STATUS TAG | Store archived (`0x02`), deleted (`0x01`), and both (`0x03`) tag |
| BLANK  | Padding 0 byte values |
| DATA PAYLOAD | Data to store in chunk |
