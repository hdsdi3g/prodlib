# REST API

## Summary <a name="top"></a>

* [Delete Demo **DELETE** `/serverPath/deleteActionControllerPath`](#goto546779669)
* [Get Demo **GET** `/serverPath/getActionController/{textValueVarName}/path?numValue=0`](#goto3292818944)
* [Patch Demo **PATCH** `/serverPath/patchActionControllerPath`](#goto3046218300)
* [Post Demo **POST** `/serverPath/postActionControllerPath`](#goto734994824)
* [Put Demo **PUT** `/serverPath/putActionControllerPath`](#goto1734623014)

## <a name="goto546779669"></a> Delete Demo
**DELETE** `/serverPath/deleteActionControllerPath`

deleteActionController

Headers: 

```javascript
Response: "application/json" {
}
```

_Mandatory rights: rightForDelete | alternateRightForDelete_

[Go to the top](#top) &bull; [DemoRestController :: deleteActionController](/blob/master/src/main/java/tv/hd3g/selfautorestdoc/demo/DemoRestController.java#70)

## <a name="goto3292818944"></a> Get Demo
**GET** `/serverPath/getActionController/{textValueVarName}/path?numValue=0`

A comment for get demo

Parameters:
 - **textValueVarName** String

Headers: 

```javascript
Response: "application/json" {
    links: Links,
    subInputDto: SubOutputDto,
    subList: [String, ...],
    subMap: {

    },
    textValue: String
}
```

[Go to the top](#top) &bull; [DemoRestController :: getActionController](/blob/master/src/main/java/tv/hd3g/selfautorestdoc/demo/DemoRestController.java#55)

## <a name="goto3046218300"></a> Patch Demo
**PATCH** `/serverPath/patchActionControllerPath`

patchActionController

Headers: 

```javascript
Response: "application/json" {
}
```

_Mandatory rights: anotherRightForPatch & rightForPatch_

[Go to the top](#top) &bull; [DemoRestController :: patchActionController](/blob/master/src/main/java/tv/hd3g/selfautorestdoc/demo/DemoRestController.java#76)

## <a name="goto734994824"></a> Post Demo
**POST** `/serverPath/postActionControllerPath`

postActionController

Headers: 

```javascript
Request body data: "text/xml" {
    subInputDto: SubInputDto,
    subList: [String, ...],
    subMap: {

    },
    text: String
}
```

```javascript
Response: "application/json" {
    links: Links,
    subInputDto: SubOutputDto,
    subList: [String, ...],
    subMap: {

    },
    textValue: String
}
```

[Go to the top](#top) &bull; [DemoRestController :: postActionController](/blob/master/src/main/java/tv/hd3g/selfautorestdoc/demo/DemoRestController.java#46)

## <a name="goto1734623014"></a> Put Demo
**PUT** `/serverPath/putActionControllerPath`

putActionController

Headers: 

```javascript
Response: "application/json" {
}
```

[Go to the top](#top) &bull; [DemoRestController :: putActionController](/blob/master/src/main/java/tv/hd3g/selfautorestdoc/demo/DemoRestController.java#63)

