import groovy.transform.Field
import java.net.URLEncoder
import groovy.json.JsonBuilder
    
@Field String VERSION = "1.0.0"

@Field List<String> LOG_LEVELS = ["error", "warn", "info", "debug", "trace"]

@Field String DEFAULT_LOG_LEVEL = LOG_LEVELS[4]

@Field static retryLock = new Object()
@Field static retryQueue = [:]

metadata {
    definition (name: "TaHoma Switch", namespace: "bitlush", author: "Keith Wood") {
        capability "Configuration"
        capability "Initialize"
        
        command "clearDevices"
        command "register"
        command "refreshDevices"
        command "testCommands"
        //command "testIntegration"
  
        
    }
    
    preferences {
        section {
            input name: "tahomaUsername", type: "text", title: "Username", required: true
            input name: "tahomaPassword", type: "password", title: "Password", required: true
            input name: "tahomaPin", type: "password", title: "PIN", required: true
            input name: "tahomaHost", type: "text", title: "Host", required: false
            
            input name: "tahomaRetryCount", title: "Retries", type: "enum", options: [[0: "Don't retry"], [1: "1"], [2: "2"], [3: "3"]], defaultValue: 0, required: true
            
            input name: "tahomaRegion", title: "Region", type: "enum", options: [[1: "Europe, Middle East and Africa"], [2: "Asia and Pacific"], [4: "North America"]], defaultValue: 1, required: true
            
            input name: "logLevel", title: "Log Level", type: "enum", options: LOG_LEVELS, defaultValue: DEFAULT_LOG_LEVEL, required: false
            input name: "stateCheckIntervalMinutes", title: "State Check Interval", type: "enum", options:[[0:"Disabled"], [30:"30min"], [60:"1h"], [120:"2h"], [180:"3h"], [240:"4h"], [360:"6h"], [480:"8h"], [720: "12h"]], defaultValue: 720, required: true
        }
    }
}

def configure() {
    logMessage("debug", "configure()")
    
    scheduleTokenCheck()
}

def clearDevices() {
    logMessage("debug", "ClearState() - Clearing device states")
    
    childDevices?.each{ deleteChildDevice(it.deviceNetworkId) }
}

def scheduleTokenCheck() {
    def intervalMinutes = 120
    
    if (stateCheckIntervalMinutes) {
        intervalMinutes = stateCheckIntervalMinutes.toInteger()
    }
    
    if (intervalMinutes) {
        if (intervalMinutes < 60) {
            schedule("0 */${intervalMinutes} * ? * *", checkState)
        }
        else {
            def intervalHours = intervalMinutes / 60
            
            schedule("0 0 */${intervalHours} ? * *", checkState)
        }
    }
}

def checkState() {
    reregister()
}

def apiInvoke(path, body) {
    def params = [
        uri: tahomaSwitchUri(),
        path: "/enduser-mobile-web/1/enduserAPI" + path,
        ignoreSSLIssues: true,
        headers: ["Content-Type": "application/json", "Authorization": "Bearer ${state.tokenId}"],
        timeout: 30,
        body: body
    ]
    
    logMessage("trace", "api invoked: ${responseData}");
    
    return apiInvokeSynchronized(params, path, body)
}

def apiInvokeForDevice(path, body, deviceURL, retryCount) {
    def params = [
        uri: tahomaSwitchUri(),
        path: "/enduser-mobile-web/1/enduserAPI" + path,
        ignoreSSLIssues: true,
        headers: ["Content-Type": "application/json", "Authorization": "Bearer ${state.tokenId}"],
        timeout: 30,
        body: body
    ]
    
    def responseData = apiInvokeSynchronized(params, path, body);
    
    if (retryCount > 0) {
        retryLaterForDevice(path, body, deviceURL, retryCount - 1);
    }
    else {
        retryCancelForDevice(path, body, deviceURL);
    }
    
    logMessage("trace", "*********** api invoked for device ${deviceURL}: ${responseData}, body: ${body} retries: ${retryCount}")
}

def testCommands() {
    def devices = apiGet("/setup/devices")
    
    logMessage("trace", "getCommands: " + devices)
    
    child = childDevices[0]
    
   //apiInvokeForDevice("/exec/apply", '{ "label": "", "actions": [ { "commands": [{ "type": "ACTUATOR", "name": "' + command + '", "parameters": [] }], "deviceURL": "' + deviceUrl + '" } ] }', deviceUrl, tahomaRetryCount as Integer)

    def params = [
        uri: tahomaSwitchUri(),
        path: "/enduser-mobile-web/1/enduserAPI" + "/setup/devices/" + child.getDeviceUrl(),
        ignoreSSLIssues: true,
        headers: ["Content-Type": "application/json", "Authorization": "Bearer ${state.tokenId}"],
        timeout: 30,
        body: body
    ]
    
    def responseData = apiInvokeSynchronized(params, path, body)
    
    logMessage("trace", "getCommands ${deviceURL}: ${responseData}")
}

def retryCancelForDevice(path, body, deviceURL) {
    synchronized (retryLock) {
        retryQueue.remove(deviceURL)
    }
}

def retryLaterForDevice(path, body, deviceURL, retryCount) {
    synchronized (retryLock) {
        retryQueue[deviceURL] = [path: path, body: body, retryCount: retryCount]
    }
    
    def retryIndex = Integer.parseInt(tahomaRetryCount) - retryCount
    
    def retryInterval = 60 * (retryIndex)
    
    logMessage("trace", "retry state: ${atomicState}, retry ${retryIndex} of ${tahomaRetryCount}, retry interval ${retryInterval} seconds")
    
    runIn(retryInterval, "retry")
}

def retry(retries) {
    def queue
    
    synchronized (retryLock) {
        queue = retryQueue
        
        retryQueue = [:]
    }
    
    queue.each { entry -> 
        deviceURL = entry.key
        path = entry.value["path"]
        body = entry.value["body"]
        retryCount = entry.value["retryCount"]

        logMessage("trace", "retrying blinds $deviceURL")
        
        apiInvokeForDevice(path, body, deviceURL, retryCount as Integer)
    }
}


@groovy.transform.Synchronized
def apiInvokeSynchronized(params, path, body) {
    def responseData = null
    
    for (int i = 0; i < 4; i++) {
        def retry = false

        try {
            if (body == null) {
                httpGet(params) { response -> responseData = response.data }
            }
            else {
                httpPost(params) { response -> responseData = response.data }
            }
        }
        catch (org.apache.http.conn.ConnectTimeoutException error) {
            logHttpException(error)

            retry = true

            params.timeout = min(180, params.timeout * 2)
        }
        catch (Exception error) {
            logHttpException(error)
        }
        finally {
            if (!retry) {
                break
            }
        }
    }
    
    return responseData
}

def apiPost(path, body) {
    return apiInvoke(path, body)
}

def apiGet(path) {
    return apiInvoke(path, null)
}

def logHttpException(Exception error) {
    if (error instanceof org.apache.http.conn.ConnectTimeoutException) {
        logMessage("error", "callApi timeout token=${state.tokenId}")
    }
    else if (error instanceof groovyx.net.http.HttpResponseException) {
        logMessage("error", "callApi response error: ${error}, r = ${error.getResponse().getData()} token=${state.tokenId}")
    }
    else {
         logMessage("error", "callApi general error: ${error}, token=${state.tokenId}")
    }
}

def tahomaSwitchUri() {
    if (tahomaHost?.length() > 1) {
        return "https://${tahomaHost}:8443"
    }
    else {
        return "https://gateway-${tahomaPin}:8443"
    }
}

def register(force = false) {
    logMessage("debug", "generateToken()")
   
    if (force || !state.tokenId || state.tokenId.length() < 5) {
        state.uuid = UUID.randomUUID().toString()
        
        createNewSession()
        
        tokenId = generateToken()
    
        token = activateToken(tokenId)
        
        state.tokenId = tokenId
    }
    else {
        logMessage("debug", "already registered and got token ${state.tokenId}")
        
        reregister()
    }
}

/*def testIntegration() {
    reregister()
}*/

def getRemainingTokenTime(token) {
    def now = new Date()
    def expires = now
    
    if (token.expirationDate) {
        expires = new Date(token.expirationTime)
    }
    
    return groovy.time.TimeCategory.minus(expires, new Date())
}

def durationToDays(duration) {
    return duration.years * 365 + duration.days
}

def durationToHours(duration) {
    return duration.years * 365 * 24 + duration.days * 24 + duration.hours
}

def reregister() {
    createNewSession()
    
    def token = getExistingToken()
    
    if (token) { 
        def remaining = getRemainingTokenTime(token)
        
        if (durationToDays(remaining) < 7) {
            token = null
            
            logMessage("info", "TaHoma Switch token will be refreshed")
        }
    }
    
    if (!token) {
        register(true)
    }
}

def refreshDevices() {
    register()
    
    def devices = apiGet("/setup/devices")
    
    logMessage("trace", "devices ${devices}")
    
    def orphaned = [:]
    
     childDevices?.each {
        orphaned[it.getDataValue("deviceUrl")] = it.deviceNetworkId
     }
    
    devices.each() {
        def typeName = it.controllableName;
        def label = it.label
        
        orphaned.remove(it.deviceURL)
        
        if (typeName.startsWith("rts:") && typeName.endsWith("VenetianBlindRTSComponent")) {
            logMessage("debug", "RTS Venetian Blind ${label}")
            
            //logMessage("debug", "TRY TO DELETE " + device.deviceNetworkId + "-" + it.deviceURL)
            
            //deleteChildDevice(device.deviceNetworkId + "-" + it.deviceURL)
            
            try {
                addTaHomaComponent(it, "TaHoma Switch - RTS Venetian Blind")
            }
            catch (error) {
                logMessage("debug", "error adding RTS device ${label} ${error}")
            }
        }
        else if (typeName.startsWith("rts:") && typeName.endsWith("RTSComponent")) {
            logMessage("debug", "RTS Component ${label}")
            
            try {
                addTaHomaComponent(it)
            }
            catch (error) {
                logMessage("debug", "error adding RTS device ${label} ${error}")
            }
        }
        else if (typeName.startsWith("io:") && typeName.endsWith("IOComponent")) {
            logMessage("debug", "IO Component ${label}")
            
            try {
                addTaHomaComponent(it)
            }
            catch (error) {
                logMessage("debug", "error adding IO device ${label} ${error}")
            }
        }
        else {
            logMessage("debug", "unknown device ${typeName}")
        }
    }
    
    for (orphan in orphaned) {
        deleteChildDevice(orphan.value)
    }
}

void addTaHomaComponent(data, type = "TaHoma Switch - RTS Blind") {
    def name = data.label
    
    def child = addChildDevice("bitlush", type, device.deviceNetworkId + "-" + data.deviceURL, [name: "${name}", label: "${name}", isComponent: true])
    
    child.updateDataValue("deviceUrl", data.deviceURL)
}

void rtsBlindCommand(command, deviceUrl) {
    apiInvokeForDevice("/exec/apply", '{ "label": "", "actions": [ { "commands": [{ "type": "ACTUATOR", "name": "' + command + '", "parameters": [] }], "deviceURL": "' + deviceUrl + '" } ] }', deviceUrl, tahomaRetryCount as Integer)
}

void rtsTiltCommand(tiltCommand, tilt, deviceUrl) {
    apiInvokeForDevice("/exec/apply", '{ "label": "", "actions": [ { "commands": [{ "type": "ACTUATOR", "name": "' + tiltCommand + '", "parameters": [' + tilt + ', 0] }], "deviceURL": "' + deviceUrl + '" } ] }', deviceUrl, 0)
}

private getExistingToken() {
    def tokens = getAvailableTokens()
    
    def token = null
    
    tokens.each() {
        if (it.label == getTokenLabel()) {
            if (token) {
                if (getRemainingTokenTime(token) < getRemainingTokenTime(it)) {
                    token = it
                }
            }
            else {
                token = it
            }
        }
    }
    
    return token
}

private getAvailableTokens() {
    def params = [
        uri: getOverkizUrl(),
        path: "/enduser-mobile-web/enduserAPI/config/${tahomaPin}/local/tokens/devmode",
        headers: ["Content-Type": "application/json", "Cookie": "JSESSIONID=${state.sessionId}"]
    ]
    
    try {
        httpGet(params) { response ->
            logMessage("debug", "getAvailableTokens: ${response.data}")
            
            return response.data;
        }
    }
    catch (error) {
        logMessage("error", "generateToken error: ${error}, r = ${error.getResponse().getData()} JSESSIONID=${state.sessionId}")
    }
}

private generateToken() {
    def params = [
        uri: getOverkizUrl(),
        path: "/enduser-mobile-web/enduserAPI/config/${tahomaPin}/local/tokens/generate",
        headers: ["Content-Type": "application/json", "Cookie": "JSESSIONID=${state.sessionId}"]
    ]
    
    try {
        httpGet(params) { response ->
            logMessage("debug", "generateToken: ${response.data}")
            
            def data = response.data
        
            (_, tokenId) = (data =~ /\[token:([0-9a-f]*)\]/)[0]
        
            logMessage("debug", "tokenId: ${tokenId}")
        
            return tokenId
        }
    }
    catch (error) {
        logMessage("error", "generateToken error: ${error} JSESSIONID=${state.sessionId}")
    }
}

private getOverkizUrl() {
    return "https://ha" + tahomaRegion + "01-1.overkiz.com";
}

private deleteToken(id) {
    def params = [
        uri: getOverkizUrl(),
        path: "/enduser-mobile-web/enduserAPI/config/${tahomaPin}/local/tokens/${id}",
        headers: ["Content-Type": "application/json", "Cookie": "JSESSIONID=${state.sessionId}"]
    ]
    
    try {
        httpDelete(params) { response ->
            logMessage("debug", "deleteToken: ${response.data}")
        }
    }
    catch (error) {
        logMessage("error", "generateToken error: ${error}, r = ${error.getResponse()} JSESSIONID=${state.sessionId}")
    }
}

def getTokenLabel() {
    return "Hubitat:" + state.uuid;
}

private activateToken(tokenId) {
    def params = [
        uri: getOverkizUrl(),
        path: "/enduser-mobile-web/enduserAPI/config/${tahomaPin}/local/tokens",
        headers: ["Content-Type": "application/json", "Cookie": "JSESSIONID=${state.sessionId}"],
        body: '{ "label": "' + getTokenLabel() + '", "token": "' + tokenId + '", "scope": "devmode" }'
    ]
    
    try {
        httpPost(params) { response ->
            logMessage("debug", "activateToken: ${response.data}")
            
            return response.data
        }
    }
    catch (error) {
        logMessage("error", "activateToken error: ${error} ${params.body}")
    }
}

private createNewSession() {
    def params = [
        uri: getOverkizUrl(),
        path: "/enduser-mobile-web/enduserAPI/login",
        headers: ["Content-Type": "application/x-www-form-urlencoded"],
        body: "userId=" + URLEncoder.encode(tahomaUsername, "UTF-8") + "&" + "userPassword=" + URLEncoder.encode(tahomaPassword, "UTF-8")
    ]
    
    try {
        httpPost(params) { response ->
            headers = response.getHeaders()
            header = headers["Set-Cookie"]
            (_, sessionId) = (header =~ /JSESSIONID=([^;]*)/)[0]
            logMessage("debug", "createNewSession: ${sessionId} cookie: ${header}")
            
            state.sessionId = sessionId
        }
    }
    catch (error) {
        logMessage("error", "createNewSession error: ${error}")
    }
}

def locationHandler(evt) {
    logMessage("debug", "locationHandler()")
}

def installed() {
    initialize()
}

def initialize() {
   
}

def parse(String description) {
    logMessage("trace", "parse() - description: ${description.inspect()}")

    def result = []

    def command = zwave.parse(description, getCommandClassVersions())
    
    if (command) {
        result = zwaveEvent(command)
    }
    else {
        logMessage("error", "parse() - Non-parsed - description: ${description?.inspect()}")
    }
    
    result
}

private logMessage(level, message) {
    if (level && message) {
        Integer levelIndex = LOG_LEVELS.indexOf(level)
        Integer setLevelIndex = LOG_LEVELS.indexOf(logLevel)
        
        if (setLevelIndex < 0) {
            setLevelIndex = LOG_LEVELS.indexOf(DEFAULT_LOG_LEVEL)
        }
        
        if (levelIndex <= setLevelIndex) {
            log."${level}" "${device.displayName} ${message}"
        }
    }
}
