import groovy.transform.Field

@Field String VERSION = "1.0.0"
@Field List<String> LOG_LEVELS = ["error", "warn", "info", "debug", "trace"]
@Field String DEFAULT_LOG_LEVEL = LOG_LEVELS[4]

metadata {
    definition (name: "TaHoma Switch - RTS Blind", namespace: "bitlush", author: "Keith Wood") {
        capability "WindowShade"
    }
    
    preferences {
        section {
            input name: "logLevel", title: "Log Level", type: "enum", options: LOG_LEVELS, defaultValue: DEFAULT_LOG_LEVEL, required: false
        }
    }
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
}

def open() {
    rtsBlindCommand("open")
}

def setPosition(position) {
    if (position <= 25) {
        rtsBlindCommand("open")
    }
    else if (position >= 75) {
        rtsBlindCommand("close")
    }
    else {
        rtsBlindCommand("my")
    }
}

def startPositionChange(direction) {
    if (direction == "open") {
        rtsBlindCommand("open")
    }
    else {
        rtsBlindCommand("close")
    }
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