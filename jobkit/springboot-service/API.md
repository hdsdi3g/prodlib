# REST API
## Summary

 - [ **PUT** /v1/jobkit/action/all/disable](#put-/v1/jobkit/action/all/disable)
 - [ **PUT** /v1/jobkit/action/all/enable](#put-/v1/jobkit/action/all/enable)
 - [ **PUT** /v1/jobkit/action/shutdown](#put-/v1/jobkit/action/shutdown)
 - [ **PUT** /v1/jobkit/action/{uuid}/disable](#put-/v1/jobkit/action/{uuid}/disable)
 - [ **PUT** /v1/jobkit/action/{uuid}/enable](#put-/v1/jobkit/action/{uuid}/enable)
 - [ **PUT** /v1/jobkit/action/{uuid}/priority/{priority}](#put-/v1/jobkit/action/{uuid}/priority/{priority})
 - [ **PUT** /v1/jobkit/action/{uuid}/retry-after-time-factor/{factor}](#put-/v1/jobkit/action/{uuid}/retry-after-time-factor/{factor})
 - [ **PUT** /v1/jobkit/action/{uuid}/timed-interval/{duration}](#put-/v1/jobkit/action/{uuid}/timed-interval/{duration})
 - [ **GET** /v1/jobkit/state/conf](#get-/v1/jobkit/state/conf)
 - [ **GET** /v1/jobkit/state/ids](#get-/v1/jobkit/state/ids)
 - [ **GET** /v1/jobkit/state/status](#get-/v1/jobkit/state/status)


## **PUT** /v1/jobkit/action/all/disable

disableAll

```javascript
Response: "application/json" {
}
```

_Mandatory rights: jobkitAction_

[Go to the top](#rest-api) &bull; [JobKitActionController :: disableAll](/blob/master/src/main/java/tv/hd3g/jobkit/mod/controller/JobKitActionController.java#106)

## **PUT** /v1/jobkit/action/all/enable

enableAll

```javascript
Response: "application/json" {
}
```

_Mandatory rights: jobkitAction_

[Go to the top](#rest-api) &bull; [JobKitActionController :: enableAll](/blob/master/src/main/java/tv/hd3g/jobkit/mod/controller/JobKitActionController.java#98)

## **PUT** /v1/jobkit/action/shutdown

shutdown

```javascript
Response: "application/json" {
}
```

_Mandatory rights: jobkitAction_

[Go to the top](#rest-api) &bull; [JobKitActionController :: shutdown](/blob/master/src/main/java/tv/hd3g/jobkit/mod/controller/JobKitActionController.java#114)

## **PUT** /v1/jobkit/action/{uuid}/disable

disable

Parameters:
 - **uuid** String

```javascript
Response: "application/json" {
}
```

_Mandatory rights: jobkitAction_

[Go to the top](#rest-api) &bull; [JobKitActionController :: disable](/blob/master/src/main/java/tv/hd3g/jobkit/mod/controller/JobKitActionController.java#58)

## **PUT** /v1/jobkit/action/{uuid}/enable

enable

Parameters:
 - **uuid** String

```javascript
Response: "application/json" {
}
```

_Mandatory rights: jobkitAction_

[Go to the top](#rest-api) &bull; [JobKitActionController :: enable](/blob/master/src/main/java/tv/hd3g/jobkit/mod/controller/JobKitActionController.java#50)

## **PUT** /v1/jobkit/action/{uuid}/priority/{priority}

setPriority

Parameters:
 - **uuid** String
 - **priority** int

```javascript
Response: "application/json" {
}
```

_Mandatory rights: jobkitAction_

[Go to the top](#rest-api) &bull; [JobKitActionController :: setPriority](/blob/master/src/main/java/tv/hd3g/jobkit/mod/controller/JobKitActionController.java#75)

## **PUT** /v1/jobkit/action/{uuid}/retry-after-time-factor/{factor}

setRetryAfterTimeFactor

Parameters:
 - **uuid** String
 - **factor** double

```javascript
Response: "application/json" {
}
```

_Mandatory rights: jobkitAction_

[Go to the top](#rest-api) &bull; [JobKitActionController :: setRetryAfterTimeFactor](/blob/master/src/main/java/tv/hd3g/jobkit/mod/controller/JobKitActionController.java#84)

## **PUT** /v1/jobkit/action/{uuid}/timed-interval/{duration}

setTimedInterval

Parameters:
 - **uuid** String
 - **duration** long

```javascript
Response: "application/json" {
}
```

_Mandatory rights: jobkitAction_

[Go to the top](#rest-api) &bull; [JobKitActionController :: setTimedInterval](/blob/master/src/main/java/tv/hd3g/jobkit/mod/controller/JobKitActionController.java#66)

## **GET** /v1/jobkit/state/conf

getConf

```javascript
Response: "application/json" {
    execPath: [String, ...],
    links: Links,
    senderReference: String,
    services: [{
        commandLine: String,
        comment: String,
        env: [String, ...],
        name: String,
        spoolName: String,
        workingDir: File
    }, ...]
}
```

_Mandatory rights: (jobkitState) & (jobkitConf)_

[Go to the top](#rest-api) &bull; [JobKitStateController :: getConf](/blob/master/src/main/java/tv/hd3g/jobkit/mod/controller/JobKitStateController.java#50)

## **GET** /v1/jobkit/state/ids

getIds

```javascript
Response: "application/json" {
    links: Links,
    servicesIds: [Item, ...]
}
```

_Mandatory rights: (jobkitState) & (jobkitStatus)_

[Go to the top](#rest-api) &bull; [JobKitStateController :: getIds](/blob/master/src/main/java/tv/hd3g/jobkit/mod/controller/JobKitStateController.java#58)

## **GET** /v1/jobkit/state/status

getLastStatus

```javascript
Response: "application/json" {
    lastStatus: {
        backgroundServicesStatus: [{
            enabled: boolean,
            name: String,
            nextRunReferenceDelay: long,
            previousScheduledDate: long,
            priority: int,
            retryAfterTimeFactor: double,
            sequentialErrorCount: int,
            spoolName: String,
            task: String,
            timedInterval: long
        }, ...],
        spoolerStatus: {
            createdThreadsCount: long,
            shutdown: boolean,
            spoolExecutors: [{
                currentOperationName: String,
                currentThreadId: long,
                currentThreadName: String,
                currentThreadState: State,
                queue: [{
                    name: String,
                    priority: int,
                    spoolName: String
                }, ...],
                shutdown: boolean,
                spoolName: String
            }, ...]
        }
    },
    links: Links
}
```

_Mandatory rights: (jobkitState) & (jobkitStatus)_

[Go to the top](#rest-api) &bull; [JobKitStateController :: getLastStatus](/blob/master/src/main/java/tv/hd3g/jobkit/mod/controller/JobKitStateController.java#42)
