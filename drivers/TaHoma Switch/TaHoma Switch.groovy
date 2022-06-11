import groovy.transform.Field
import java.net.URLEncoder
import groovy.json.JsonBuilder
    
@Field String VERSION = "1.0.0"

@Field List<String> LOG_LEVELS = ["error", "warn", "info", "debug", "trace"]

@Field String DEFAULT_LOG_LEVEL = LOG_LEVELS[4]

metadata {
    definition (name: "TaHoma Switch", namespace: "bitlush", author: "Keith Wood") {
        capability "Configuration"
        capability "Initialize"
        
        command "clearDevices"
        command "register"
        command "refreshDevices"
    }
    
    preferences {
        section {
            input name: "tahomaUsername", type: "text", title: "Username", required: true
            input name: "tahomaPassword", type: "password", title: "Password", required: true
            input name: "tahomaPin", type: "password", title: "PIN", required: true
            
            input name: "logLevel", title: "Log Level", type: "enum", options: LOG_LEVELS, defaultValue: DEFAULT_LOG_LEVEL, required: false
            input name: "stateCheckIntervalMinutes", title: "State Check Interval", type: "enum", options:[[0:"Disabled"], [5:"5min"], [10:"10min"], [15:"15min"], [30:"30min"], [60:"1h"], [120:"2h"], [180:"3h"], [240:"4h"], [360:"6h"], [480:"8h"], [720: "12h"]], defaultValue: 360, required: true
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

def apiGet(path) {
    def params = [
        uri: "https://gateway-${tahomaPin}:8443",
        path: "/enduser-mobile-web/1/enduserAPI" + path,
        ignoreSSLIssues: true,
        headers: ["Authorization": "Bearer ${state.tokenId}"]
    ]
    
    try {
        httpGet(params) { response ->
            return response.data
        }
    }
    catch (error) {
        logMessage("error", "callApi error: ${error} token=${state.tokenId}")
    }
}

def apiPost(path, body) {
    def params = [
        uri: "https://gateway-${tahomaPin}:8443",
        path: "/enduser-mobile-web/1/enduserAPI" + path,
        ignoreSSLIssues: true,
        headers: ["Content-Type": "application/json", "Authorization": "Bearer ${state.tokenId}"],
        body: body
    ]
    
    try {
        httpPost(params) { response ->
            logMessage("debug", "apiPost: ${response.data}")
        }
    }
    catch (error) {
        logMessage("error", "callApi error: ${error}, r = ${error.getResponse().getData()} token=${state.tokenId}")
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

def reregister() {
    createNewSession()
    
    def token = getExistingToken()
    
    if (token) {
        def timeLeft = token.expirationTime - token.gatewayCreationTime
        
        logMessage("debug", "token time left ${timeLeft}")
        
        if (timeLeft < 1000000000) {
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
    
    def orphaned = [:]
    
     childDevices?.each {
        orphaned[it.getDataValue("deviceUrl")] = it.deviceNetworkId
     }
    
    devices.each() {
        def typeName = it.controllableName;
        def label = it.label
        
        orphaned.remove(it.deviceURL)
        
        if (typeName == "rts:BlindRTSComponent") {
            logMessage("debug", "rts:BlindRTSComponent ${label}")
            
            try {
                addTaHomaRtsBlind(it)
            }
            catch (error) {
                logMessage("debug", "error adding device ${label} ${error}")
            }
        }
    }
    
    for (orphan in orphaned) {
        deleteChildDevice(orphan.value)
    }
}

void addTaHomaRtsBlind(data) {
    def name = data.label
    
    def child = addChildDevice("bitlush", "TaHoma Switch - RTS Blind", device.deviceNetworkId + "-" + data.deviceURL, [name: "${name}", label: "${name}", isComponent: true])
    
    child.updateDataValue("deviceUrl", data.deviceURL)
}

void rtsBlindCommand(command, deviceUrl) {
    apiPost("/exec/apply", '{ "label": "", "actions": [ { "commands": [{ "type": "ACTUATOR", "name": "' + command + '", "parameters": [] }], "deviceURL": "' + deviceUrl + '" } ] }')
}

private getExistingToken() {
    def tokens = getAvailableTokens()
    
    def token = null
    
    tokens.each() {
        if (it.label == getTokenLabel()) {
            token = it
        }
    }
    
    return token
}

private getAvailableTokens() {
    def params = [
        uri: "https://ha101-1.overkiz.com/enduser-mobile-web/enduserAPI/config/${tahomaPin}/local/tokens/devmode",
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
        uri: "https://ha101-1.overkiz.com/enduser-mobile-web/enduserAPI/config/${tahomaPin}/local/tokens/generate",
        headers: ["Content-Type": "application/json", "Cookie": "JSESSIONID=${state.sessionId}"]
    ]
    
    try {
        httpGet(params) { response ->
            logMessage("debug", "generateToken: ${response.data}")
            
            def data = response.data
        
            //def data = "[token:62a481c3bd9e068d3cdf]"
        
            (_, tokenId) = (data =~ /\[token:([0-9a-f]*)\]/)[0]
        
            logMessage("debug", "tokenId: ${tokenId}")
        
            return tokenId
        }
    }
    catch (error) {
        logMessage("error", "generateToken error: ${error} JSESSIONID=${state.sessionId}")
    }
}

private deleteToken(id) {
    def params = [
        uri: "https://ha101-1.overkiz.com/enduser-mobile-web/enduserAPI/config/${tahomaPin}/local/tokens/${id}",
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
        uri: "https://ha101-1.overkiz.com",
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
        uri: "https://ha101-1.overkiz.com",
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
