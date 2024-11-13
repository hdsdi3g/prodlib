# Datablock internal format

## Document and Chunks

A document contain an header and some chunks.

A chunk contain and header and some data (binary), limited to `2^32/2-1` bytes.

Numbers in headers are **big endian** coded (as default in Java's ByteBuffers).

### Document format

```
________________________________________________________________________________________________________________________________________
| MAGIC NUMBER | DOCUMENT TYPE | TYPE VERSION | DOCUMENT VERSION | BLANK [| CHUNK0 | CHUNK SEPARATOR [| CHUNKn | CHUNK SEPARATOR ]...] | EOF
|    BYTES     |     BYTES     |    SHORT     |      INT         | BYTES  | CHUNK  |      BYTES       | CHUNK  |        BYTES          |
|<---- 8 ----->|<----- 8 ----->|<---- 2 ----->|<----- 4 -------> |<- 2 -->|        |<------ 1 ------->|        |<-------- 1 ---------->|
________________________________________________________________________________________________________________________________________
```

With:

|------------|--|
|MAGIC NUMBER|AA|
|------------|--|

 - MAGIC NUMBER
 - DOCUMENT TYPE
 - TYPE VERSION
 - DOCUMENT VERSION
 - BLANK
 - CHUNK, see below
 - CHUNK SEPARATOR, one 0 byte

