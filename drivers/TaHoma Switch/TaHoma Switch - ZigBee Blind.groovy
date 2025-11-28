import groovy.transform.Field

@Field String VERSION = "1.1.0"
@Field List<String> LOG_LEVELS = ["error", "warn", "info", "debug", "trace"]
@Field String DEFAULT_LOG_LEVEL = LOG_LEVELS[4]

metadata {
    definition (name: "TaHoma Switch - Zigbee Blind", namespace: "bitlush", author: "Laurent de Barry") {
        capability "WindowShade"
        capability "Refresh"
        capability "Battery"
        capability "SwitchLevel"
        capability "Switch"
        
        command "my" // run the Somfy My command
        command "pause" // Needed by Google Home integration
        command "resetGoogleState" // Needed if blinds don't appear in Google Home
        
        attribute 'lastBatteryDate', 'date'
        attribute 'windowShadeState', 'string'
    }
    
    preferences {
        section {
            input name: "logLevel", title: "Log Level", type: "enum", options: LOG_LEVELS, defaultValue: DEFAULT_LOG_LEVEL, required: false
        }
    }
}

def resetGoogleState() {
    log.warn "--- FORCING STATE AND EVENT RESET FOR GOOGLE SYNC ---"
    
    // A. CLEAR ATTRIBUTE HISTORY
    device.deleteCurrentState("level")
    device.deleteCurrentState("windowShade")
    device.deleteCurrentState("switch")
    log.info "Core attributes cleared."

    // B. IMMEDIATE INITIALIZATION
    // Hubitat requires these events to be present for the capabilities to be recognized.
    // We set it to a known state (e.g., closed)
    sendEvent(name: "level", value: 0) // WindowShade position (0% = Open)
    sendEvent(name: "position", value: 100) // WindowShade position (position of 100 = Open)
    sendEvent(name: "windowShade", value: "open") // WindowShade state
    sendEvent(name: "switch", value: "on") // Switch state (Open = On)    
    log.info "Initial attributes set (Level=0, position=100, Shade=Open, Switch=On)."
    
    // C. Trigger the platform to see the change
    runIn(2, "refresh")
    
    log.warn "Reset complete. Please manually ask Google to 'sync my devices'."
}

def parse(event) {
    eventName = event.name[0]
    eventValue = event.value[0]
    sendEvent(name: eventName, value: eventValue)
    
    logMessage("trace", "Zigbee Bling parse() => event: ${eventName}:${eventValue} | ${event}")
    
    //For battery levels, operationalStatusState and closureState we need to do a little more
    if (eventName == "core:ClosureState") {
        def percentClosed = eventValue.toInteger()
        
        if (percentClosed == 0) {
            sendEvent(name: "windowShade", value:"open")
            sendEvent(name: "switch", value: "on")
        }
        else if (percentClosed == 100) {
            sendEvent(name: "windowShade", value:"closed")
            sendEvent(name: "switch", value: "off")
        }
        else {
        	sendEvent(name: "windowShade", value:"partially open")            
        }
        
        // 2. Report the level and position - position is used by Google Home
        def percentOpen = 100 - percentClosed
        sendEvent(name: "level", value:percentClosed)
        sendEvent(name: "position", value:percentOpen)
    }
    else if (eventName == "core:OperationalStatusState") {
        sendEvent(name: "windowShadeState", value:eventValue)
        
        // If the window shade is not moving anymore, we can cancel the state refresh loop
        if (eventValue == "not moving") {
            stopStateRefreshLoop()
        }        
    }
    else if (eventName == "core:BatteryLevelState") {
        currentTime = new Date()
    	sendEvent(name: 'battery', value: eventValue)
        sendEvent(name:'battery', value:eventValue, unit:'%', descriptionText:"Battery is ${percentage}% full")
        sendEvent(name:'lastBatteryDate', value:currentTime, descriptionText:"Last battery report time is ${currentTime}")
    }
}

def getDeviceUrl() {
    return device.getDataValue("deviceUrl")
}

def startStateRefreshLoop() {
    atomicState.refreshLoopCounter = 30
    state.refreshLoopPeriod = 1
    runIn(state.refreshLoopPeriod, "stateRefreshLoop")
}

def stopStateRefreshLoop() {
    unschedule("stateRefreshLoop")
    atomicState.refreshLoopCounter = 0
}

def stateRefreshLoop() {
    refreshLoopCounter = atomicState.refreshLoopCounter
    if (refreshLoopCounter > 0) {
        // Decrement counter, perform action and schedule
        atomicState.refreshLoopCounter = refreshLoopCounter - 1
        refresh()
        runIn(state.refreshLoopPeriod, "stateRefreshLoop")
    } 
    else {
        stopStateRefreshLoop()
    }
}

def zigbeeBlindCommand(command) {
    parent.zigbeeBlindCommand(command,[], getDeviceUrl())
    startStateRefreshLoop()
}

def zigbeeBlindCommand(command, parameters) {
    parent.zigbeeBlindCommand(command,parameters, getDeviceUrl())
    startStateRefreshLoop()
}

def zigbeeGetState(StateName, deviceUrl) {
    stateValue = null
    attributes = parent.zigbeeGetDeviceAttributes(deviceUrl)
    if (attributes != null) {
    	state = attributes.states.find {it.name == "core:" + StateName}
    	stateValue = state?.value
    }
    
    return stateValue
}

def refresh() {
    logMessage("trace", "Zigbee Bling refresh()")
    parent.refresh()
}

def close() {
    zigbeeBlindCommand("close")
    sendEvent(name: "switch", value: "off")
}

def open() {
    zigbeeBlindCommand("open")
    sendEvent(name: "switch", value: "on")
}

// Implement on() and off() to act as open() and close(). Needed by Google Home...
def on() { open() }
def off() { close() }

def my() {
    zigbeeBlindCommand("my")
}

def setLevel(level) {
    zigbeeBlindCommand("setPosition", [level])
}

// This function is used by Google home to control the blind and for google, a posision of 0 means the blind is fully unrolled and 100, the blind is full rolled up
def setPosition(position) {    
    logMessage("trace","setPosition(): position=>${position}")
    level = 100 - position.toInteger()
    logMessage("trace","setPosition(): level=>${level}")
    setLevel(level)
}

def startPositionChange(direction) {
    if (direction == "open") {
        open()
    }
    else {
        close()
    }
}

// Required for Google Home Integration
def pause() {
    stopPositionChange()
}

def stopPositionChange() {
    zigbeeBlindCommand("stop")
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