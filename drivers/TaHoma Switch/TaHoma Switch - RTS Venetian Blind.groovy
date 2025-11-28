import groovy.transform.Field

@Field String VERSION = "1.1.0"
@Field List<String> LOG_LEVELS = ["error", "warn", "info", "debug", "trace"]
@Field String DEFAULT_LOG_LEVEL = LOG_LEVELS[4]

metadata {
    definition (name: "TaHoma Switch - RTS Venetian Blind", namespace: "bitlush", author: "Keith Wood") {
        capability "WindowBlind"
		capability "SwitchLevel"
        capability "Switch"
		
		command "my" // run the Somfy My command
		command "pause" // Needed by Google Home integration
        command "resetGoogleState" // Needed if blinds don't appear in Google Home
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
        
    log.warn "Reset complete. Please manually ask Google to 'sync my devices'."
}

def parse(value) {
    
}

def getDeviceUrl() {
    return device.getDataValue("deviceUrl")
}

def rtsBlindCommand(command) {
    parent.rtsBlindCommand(command, getDeviceUrl())
}

def close() {
    rtsBlindCommand("close")
	sendEvent(name: "switch", value: "off")
	sendEvent(name: "windowShade", value:"closed")
	sendEvent(name: "position", value:0)
}

def open() {
    rtsBlindCommand("open")
	sendEvent(name: "switch", value: "on")
	sendEvent(name: "windowShade", value:"open")
	sendEvent(name: "position", value:100)
}

def my() {
    rtsBlindCommand("my")
	sendEvent(name: "windowShade", value:"partially open")
	sendEvent(name: "position", value:50)
}

// Implement on() and off() to act as open() and close(). Needed by Google Home...
def on() { open() }
def off() { close() }

// This function is used by Google home to control the blind and for google, a posision of 0 means the blind is fully unrolled and 100, the blind is full rolled up
def setPosition(position) {    
    logMessage("trace","setPosition(): position=>${position}")
    level = 100 - position.toInteger()
    logMessage("trace","setPosition(): level=>${level}")
    setLevel(level)
}

def setLevel(level) {
    if (level <= 25) {
        open()
    }
    else if (level >= 75) {
        close()
    }
    else {
        my()
    }
}

def setTiltLevel(position) {
    logMessage("trace", "set tilt: " + position)
    
    parent.rtsTiltCommand("tiltPositive", position, getDeviceUrl())
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
    rtsBlindCommand("stop")
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