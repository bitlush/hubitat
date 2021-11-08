import groovy.transform.Field

@Field String VERSION = "1.0.0"

@Field List<String> LOG_LEVELS = ["error", "warn", "info", "debug", "trace"]
@Field String DEFAULT_LOG_LEVEL = LOG_LEVELS[2]

metadata {
    definition (name: "Heltun TPS - Relay", namespace: "bitlush", author: "Keith Wood") {
        capability "Actuator"
        capability "Switch"
    }
    
    preferences {
        section {
            input name: "logLevel", title: "Log Level", type: "enum", options: LOG_LEVELS, defaultValue: DEFAULT_LOG_LEVEL, required: false
        }
    }
}

def parse(value) {
    logMessage("debug", "Relay parse (${value})")
    
    if (value) {
        def name = switchEventName(value)
        
        if (name == "switch") {
            def v = switchEventValue(value)
            
            if (v != device.currentValue('switch')) {
                sendEvent(value)
                
                logMessage("debug", "Switch turned ${v}")
            }
        }
        else {
            logMessage("trace", "parse (${value})")
            
            sendEvent(value)
        }
    }
}

def switchEventName(value) {
    return value.name[0]
}

def switchEventValue(value) {
    return value.value[0]
}

def installed() {
}

def isOn() {
    return device.currentValue('switch') == 'on';
}

def on() {
    logMessage("debug", "on()")
    
    if (!isOn()) {
        parse([[name: "switch", value: "on", type: "digital"]])
        
        parent.componentOn(device)
    }
}

def off() {
    logMessage("debug", "off()")
    
    if (isOn()) {
        parse([[name: "switch", value: "off", type: "digital"]])
        
        parent.componentOff(device)
    }
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