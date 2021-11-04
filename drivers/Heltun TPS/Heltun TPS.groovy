import groovy.transform.Field

@Field String VERSION = "1.0.1"

@Field List<String> LOG_LEVELS = ["error", "warn", "info", "debug", "trace"]

@Field String DEFAULT_LOG_LEVEL = LOG_LEVELS[1]

@Field List<Integer> PARAMETER_ENUMS = [
    7, 8, 9, 10, 11, // relay mode,
    31, 32, 33, 34, 35, // backlight control source
    41, 42, 43, 44, 45, // button hold mode
    51, 52, 53, 54, 55, // button click mode
    61, 62, 63, 64, 65, // reporting
    141, 142, 143, 144, 146 // relay control source
]

metadata {
    definition (name: "Heltun TPS", namespace: "bitlush", author: "Keith Wood") {
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"
        
        command "clearState"
        command "refreshPreferences"
        command "configurePreferences"
        
        attribute "numberOfButtons", "number"

        fingerprint mfr:"0344", prod:"0004"
        fingerprint deviceId: "0003", inClusters: "0x5E, 0x55, 0x98, 0x9F, 0x6C, 0x22", secureInClusters: "0x85, 0x59, 0x8E, 0x60, 0x86, 0x72, 0x5A, 0x73, 0x81, 0x87, 0x70, 0x31, 0x25, 0x5B, 0x32, 0x7A" // HE-TPS05
    }
    
    preferences {
        section {
            input name: "logLevel", title: "Log Level", type: "enum", options: LOG_LEVELS, defaultValue: DEFAULT_LOG_LEVEL, required: false
            input name: "logDescText", title: "Log Description Text", type: "bool", defaultValue: false, required: false
            
            input name: "param141", title: "Report Consumption Interval", type: "enum", options: [[1: "1m"], [2: "2m"], [3: "3m"], [4: "4m"], [5: "5m"],[10: "10m"], [15: "15m"], [30: "30m"], [60: "1h"], [120: "2h"]], defaultValue: 10, required: true
            input name: "param142", title: "Report Consumption on Change", type: "enum", options: [[0: "Disabled"], [1: "Enabled"]], defaultValue: 1, required: true
            input name: "param143", title: "Report Sensors Interval", type: "enum", options: [[1: "1m"], [2: "2m"], [3: "3m"], [4: "4m"], [5:"5m"],[10: "10m"], [15: "15m"], [30: "30m"], [60: "1h"], [120: "2h"]], defaultValue: 10, required: true
            input name: "param144", title: "Report Threshold Temperature", type: "enum", options: [[0: "Disabled"], [1:"0.1°C"], [2: "0.2°C"], [3: "0.3°C"], [4: "0.4°C"], [5: "0.5°C"], [6: "0.6°C"], [7: "0.7°C"], [8: "0.8°C"], [9: "0.9°C"], [10: "1°C"], [20: "2°C"], [30: "3°C"], [40: "4°C"], [50: "5°C"], [60: "6°C"], [70: "7°C"], [80: "8°C"], [90: "9°C"], [100: "10°C"]], defaultValue: 2, required: true
            input name: "param145", title: "Report Threshold Humidity", type: "enum", options: [[0: "Disabled"], [1: "1%"], [2: "2%"], [3: "3%"], [4: "4%"], [5: "5%"], [6: "6%"], [7: "8%"], [8: "8%"], [9: "9%"], [10: "10%"], [15: "15%"], [20: "20%"], [25: "25%"]], defaultValue: 2, required: true
            input name: "param146", title: "Report Threshold Light", type: "enum", options: [[0: "Disabled"], [10: "10%"], [15: "15%"], [20: "20%"], [25: "25%"], [30: "30%"], [35: "35%"], [40: "40%"], [45:"45%"], [50: "50%"], [55: "55%"], [60: "60%"], [65: "65%"], [70: "70%"], [75: "75%"], [80: "80%"], [85: "85%"], [90: "90%"], [95: "95%"]], defaultValue: 50, required: true
        
            if ((settingButtons as Integer) >= 1) input name: "param61", title: "Relay 1 Control Source", type: "enum", options: relayControlSourceOptions(), defaultValue: 1, required: true
            if ((settingButtons as Integer) >= 2) input name: "param62", title: "Relay 2 Control Source", type: "enum", options: relayControlSourceOptions(), defaultValue: 2, required: true
            if ((settingButtons as Integer) >= 3) input name: "param63", title: "Relay 3 Control Source", type: "enum", options: relayControlSourceOptions(), defaultValue: 3, required: true
            if ((settingButtons as Integer) >= 4) input name: "param64", title: "Relay 4 Control Source", type: "enum", options: relayControlSourceOptions(), defaultValue: 4, required: true
            if ((settingButtons as Integer) >= 5) input name: "param65", title: "Relay 5 Control Source", type: "enum", options: relayControlSourceOptions(), defaultValue: 5, required: true
      
            if ((settingButtons as Integer) >= 1) input name: "param7", title: "Relay 1 Mode", type: "enum", options: relayModeOptions(), defaultValue: 0, required: true
            if ((settingButtons as Integer) >= 2) input name: "param8", title: "Relay 2 Mode", type: "enum", options: relayModeOptions(), defaultValue: 0, required: true
            if ((settingButtons as Integer) >= 3) input name: "param9", title: "Relay 3 Mode", type: "enum", options: relayModeOptions(), defaultValue: 0, required: true
            if ((settingButtons as Integer) >= 4) input name: "param10", title: "Relay 4 Mode", type: "enum", options: relayModeOptions(), defaultValue: 0, required: true
            if ((settingButtons as Integer) >= 5) input name: "param11", title: "Relay 5 Mode", type: "enum", options: relayModeOptions(), defaultValue: 0, required: true
      
            if ((settingButtons as Integer) >= 1) input name: "param31", title: "Backlight 1 Control Source", description: "", type: "enum", options: backlightControlSourceOptions(), defaultValue: 1, required: true
            if ((settingButtons as Integer) >= 2) input name: "param32", title: "Backlight 2 control Source", description: "", type: "enum", options: backlightControlSourceOptions(), defaultValue: 1, required: true
            if ((settingButtons as Integer) >= 3) input name: "param33", title: "Backlight 3 Control Source", description: "", type: "enum", options: backlightControlSourceOptions(), defaultValue: 1, required: true
            if ((settingButtons as Integer) >= 4) input name: "param34", title: "Backlight 4 Control Source", description: "", type: "enum", options: backlightControlSourceOptions(), defaultValue: 1, required: true
            if ((settingButtons as Integer) >= 5) input name: "param35", title: "Backlight 5 Control Source", description: "", type: "enum", options: backlightControlSourceOptions(), defaultValue: 1, required: true
      
            if ((settingButtons as Integer) >= 1) input name: "param41", title: "Button 1 Hold mode", type: "enum", options: buttonHoldModeOptions(), defaultValue: 2, required: true
            if ((settingButtons as Integer) >= 2) input name: "param42", title: "Button 2 Hold mode", type: "enum", options: buttonHoldModeOptions(), defaultValue: 2, required: true
            if ((settingButtons as Integer) >= 3) input name: "param43", title: "Button 3 Hold mode", type: "enum", options: buttonHoldModeOptions(), defaultValue: 2, required: true
            if ((settingButtons as Integer) >= 4) input name: "param44", title: "Button 4 Hold mode", type: "enum", options: buttonHoldModeOptions(), defaultValue: 2, required: true
            if ((settingButtons as Integer) >= 5) input name: "param45", title: "Button 5 Hold mode", type: "enum", options: buttonHoldModeOptions(), defaultValue: 2, required: true
      
            if ((settingButtons as Integer) >= 1) input name: "param51", title: "Button 1 Click mode", type: "enum", options: buttonClickModeOptions(), defaultValue: 1, required: true
            if ((settingButtons as Integer) >= 2) input name: "param52", title: "Button 2 Click mode", type: "enum", options: buttonClickModeOptions(), defaultValue: 1, required: true
            if ((settingButtons as Integer) >= 3) input name: "param53", title: "Button 3 Click mode", type: "enum", options: buttonClickModeOptions(), defaultValue: 1, required: true
            if ((settingButtons as Integer) >= 4) input name: "param54", title: "Button 4 Click mode", type: "enum", options: buttonClickModeOptions(), defaultValue: 1, required: true
            if ((settingButtons as Integer) >= 5) input name: "param55", title: "Button 5 Click mode", type: "enum", options: buttonClickModeOptions(), defaultValue: 1, required: true
      
            //input name: "param12", title: "Button 1 Relay power load", description: "Watts", type: "number", range: "0..1100", defaultValue: 0, required: true
            //input name: "param13", title: "Button 2 Relay power load", description: "Watts", type: "number", range: "0..1100", defaultValue: 0, required: true
            //input name: "param14", title: "Button 3 Relay power load", description: "Watts", type: "number", range: "0..1100", defaultValue: 0, required: true
            //input name: "param15", title: "Button 4 Relay power load", description: "Watts", type: "number", range: "0..1100", defaultValue: 0, required: true
            //input name: "param16", title: "Button 5 Relay power load", description: "Watts", type: "number", range: "0..1100", defaultValue: 0, required: true
      
            //input name: "param71", title: "Button 1 Timer duration", description: "Seconds", type: "number", range: "0..43200", defaultValue: 0, required: true
            //input name: "param72", title: "Button 2 Timer duration", description: "Seconds", type: "number", range: "0..43200", defaultValue: 0, required: true
            //input name: "param73", title: "Button 3 Timer duration", description: "Seconds", type: "number", range: "0..43200", defaultValue: 0, required: true
            //input name: "param74", title: "Button 4 Timer duration", description: "Seconds", type: "number", range: "0..43200", defaultValue: 0, required: true
            //input name: "param75", title: "Button 5 Timer duration", description: "Seconds", type: "number", range: "0..43200", defaultValue: 0, required: true
        }
    }
}

def buttonClickModeOptions() {
    [[0: "Disabled"], [1: "Relay inverts based on relay state"], [2: "Relay inverts based on button backlight state"], [3: "Relay switches to ON"], [4: "Relay switches to OFF"], [5: "Timer On>Off"], [6: "Timer Off>On"]]
}

def buttonHoldModeOptions() {
    [[0: "Disabled"], [1: "Operate like click"], [2: "Momentary"], [3: "Momentary Reversed"], [4: "Momentary Toggle"]]
}

def backlightControlSourceOptions() {
    [[0: "Disabled (both color LEDs are turned off)"], [1: "Controlled by touch (reflects the button state)"], [2:"Controlled by gateway or associated device (the button state is ignored)"]]
}

def relayModeOptions() {
    [[0: "NO"], [1: "NC"]]
}

def relayControlSourceOptions() {
    def options = [[0: "Controlled by gateway or associated device"]]
    
    def buttons = getEndPoints() / 2;
    
    if (buttons == 1) {
        options = options + [[1: "Button 1 (Middle)"]]
    }
    
    if (buttons == 2) {
        options = options + [[1: "Button 1 (Left)"], [2: "Button 2 (Right)"]]
    }
    
    if (buttons == 3) {
        options = options + [[1: "Button 1 (Top)"], [2: "Button 2 (Middle)"], [3: "Button 3 (Bottom)"]]
    }
    
    if (buttons >= 4) {
        options = options + [[1: "Button 1 (Top Left)"], [2: "Button 2 (Top Right)"], [3: "Button 3 (Bottom Left)"], [4: "Button 4 (Bottom Right)"]]
        
        if (buttons == 5) {
            options = options + [[5: "Button 5 (Middle)"]]
        }
    }
    
    return options
}

def zwaveEvent(hubitat.zwave.commands.configurationv4.ConfigurationReport command) {
    logMessage("trace", "ConfigurationReport ${command.parameterNumber} = ${command.scaledConfigurationValue}")
    
    def p = command.parameterNumber
    def v = command.scaledConfigurationValue.toString()
    
    if (PARAMETER_ENUMS.contains(p)) {
        device.updateSetting("param${p}", [value: v, type: "enum"])
    }
    
    []
}

def refreshPreferences() { 
    def params = PARAMETER_ENUMS
    
    def configGets = params.collect { p -> zwave.configurationV4.configurationGet(parameterNumber: p) }
    
    sequenceCommands(configGets)
}

def configurePreferences() {
    def params = PARAMETER_ENUMS
    
    def configSets = params.collect { p -> zwave.configurationV4.configurationSet(parameterNumber: p, size: 1, scaledConfigurationValue: settings["param${p}"].toInteger()) }
    
    sequenceCommands(configSets)
}

def installed() {
    initialize()
}

def initialize() {
    def endpoints = getEndPoints();
    
    if (endpoints) {
        def commands = []
    
        (1..endpoints).each() {
            commands << endpoint(zwave.switchBinaryV2.switchBinaryGet(), it as Integer)
        }
    
        sendHubCommand(new hubitat.device.HubMultiAction(sequenceCommands(commands), hubitat.device.Protocol.ZWAVE))
    }
}

def configure() {
    logMessage("debug", "configure()")
    
    sequenceCommands([
        zwave.associationV2.associationRemove(groupingIdentifier: 1, nodeId: []),
        zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:1, multiChannelNodeIds: [[nodeId: zwaveHubNodeId, bitAddress: 0, endPointId: 0]]),
        zwave.versionV3.versionGet(),
        zwave.manufacturerSpecificV2.deviceSpecificGet(),
        //zwave.firmwareUpdateMdV5.firmwareMdGet(),
        zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
        zwave.multiChannelV4.multiChannelEndPointGet(),
        //zwave.associationV2.associationGroupingsGet(),
        //zwave.multiChannelAssociationV2.multiChannelAssociationGroupingsGet()
    ])
}

def clearState() {
    logMessage("debug", "ClearState() - Clearing device states")
    
    state.clear()

    if (state?.driverInfo == null) {
        state.driverInfo = [:]
    }
    else {
        state.driverInfo.clear()
    }

    if (state?.deviceInfo == null) {
        state.deviceInfo = [:]
    }
    else {
        state.deviceInfo.clear()
    }
    
    childDevices?.each{ deleteChildDevice(it.deviceNetworkId) }
    
    updateDataValue("MSR", "")
   
    installed()
}

def parse(String description) {
    //logMessage("debug", "parse() - description: ${description.inspect()}")

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

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.DeviceSpecificReport command) {
    logMessage("trace", "zwaveEvent(DeviceSpecificReport) - command: ${command.inspect()}")
    
    if (command.deviceIdType == 1) {
        def serialNumber = ""
        
        if (command.deviceIdDataFormat == 1) {
            command.deviceIdData.each { serialNumber += hubitat.helper.HexUtils.integerToHexString(it & 0xff,1).padLeft(2, '0')}
        } else {
            command.deviceIdData.each { serialNumber += (char) it }
        }
        
        device.updateDataValue("serialNumber", serialNumber)
    }
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport command) {
    logMessage("trace", "version2 report: ${command}")
    
    device.updateDataValue("firmwareVersion", "${command.firmware0Version}.${command.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${command.zWaveProtocolVersion}.${command.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${command.hardwareVersion}")
}

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport command) {
    logMessage("trace", "zwaveEvent(ManufacturerSpecificReport) - command: ${command.inspect()}")

    //state.deviceInfo['manufacturerId'] = "${command.manufacturerId}"
    //state.deviceInfo['productId'] = "${command.productId}"
    //state.deviceInfo['productTypeId'] = "${command.productTypeId}"

    def msr = String.format("%04X-%04X-%04X", command.manufacturerId, command.productTypeId, command.productId)
    
    updateDataValue("MSR", msr)
    
    if (command?.manufacturerName && command?.manufacturerName != "") {
        updateDataValue("manufacturer", command.manufacturerName)
        
        //state.deviceInfo['manufacturerName'] = "${command.manufacturerName}"
    } else if (command?.manufacturerId != "") {
        updateDataValue("manufacturer", command?.manufacturerId?.toString())
    }
    
    []
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationGroupingsReport command) {
    /*state.groups = command.supportedGroupings
    
    if (command.supportedGroupings > 1) {
        [response(zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier:2, listMode:1))]
    }*/
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet command, Integer endPoint = null) {
    []
}

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet command, endPoint = 0) {
    hubitat.zwave.Command encapsulatedCommand = command.encapsulatedCommand(CMD_CLASS_VERS)
    
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand, endPoint)
    }
    
    if (endPoint > 0) {
        sendHubCommand(new hubitat.device.HubAction(zwaveSecureEncap(zwave.multiChannelV4.multiChannelCmdEncap(sourceEndPoint: 0, bitAddress: 0, res01: 0, destinationEndPoint: endPoint).encapsulate(zwave.supervisionV1.supervisionReport(sessionID: command.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0)).format()), hubitat.device.Protocol.ZWAVE))
    } else {
        sendHubCommand(new hubitat.device.HubAction(zwaveSecureEncap(zwave.supervisionV1.supervisionReport(sessionID: command.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0).format()), hubitat.device.Protocol.ZWAVE))
    }
}

def zwaveEvent(hubitat.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport command) {
    []
}

def zwaveEvent(hubitat.zwave.commands.multichannelv4.MultiChannelEndPointReport command) {
    logMessage("trace", "zwaveEvent(MultiChannelEndPointReport) - command: ${command.inspect()}")
    
    if (state?.deviceInfo == null) {
        state.deviceInfo = [:]
    }
    
    state.deviceInfo['endPoints'] = command.endPoints
    
    def numberOfButtons = command.endPoints / 2
    
    device.updateSetting("settingButtons", [value: numberOfButtons.toString(), type: "Integer"])
    
    sendEvent(name: "numberOfButtons", value: numberOfButtons, displayed: true)

    if (!childDevices && command.endPoints > 1) {
        (1..numberOfButtons).each() {
            addChildEndPoint("Button", it as Integer, command.endPoints)
        }
        
        (1 + numberOfButtons..command.endPoints).each() {
            addChildEndPoint("Relay", (it - numberOfButtons) as Integer, command.endPoints)
        }
    }
}

void addChildEndPoint(name, number, endPoints) {
    def child = addChildDevice("bitlush", "Heltun TPS - ${name}", "${device.deviceNetworkId}-${number}", [name: "${name} ${number} (${device.displayName})", label: "${name} ${number} (${device.displayName})", isComponent: true])
            
    child.updateDataValue("endPoints", endPoints.toString())
    child.updateDataValue("number", number.toString())
}

def getEndPoints() {
    def v = state.deviceInfo['endPoints'] as Integer

    return v == null ? 0 : v;
}

def zwaveEvent(hubitat.zwave.commands.clockv1.ClockReport command) {
    logMessage("trace", "zwaveEvent(ClockReport) - command: ${command.inspect()}")
  
    []
}

def zwaveEvent(hubitat.zwave.commands.basicv2.BasicReport command, Integer endPoint = null) {
    logMessage("debug", "zwaveEvent(BasicReport) - command: ${command.inspect()}, endPoint: ${endPoint}")
    
    []
}

def zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport command) {
    logMessage("trace", "zwaveEvent(SensorMultilevelReport) - command: ${command.inspect()}")
    
    []
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv2.SwitchBinaryReport command, Integer endPoint = null) {
    logMessage("debug", "zwaveEvent(SwitchBinaryReport) - command: ${command.inspect()}, endPoint: ${endPoint}")
    
    def value = command.value > 0 ? 'on' : 'off';
    
    getChildDevice("${device.deviceNetworkId}-${endPoint}").parse([[name: "switch", value: value, type: "physical"]])
    
    []
}

void zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation command) {
    def encapsulatedCommand = command.encapsulatedCommand(commandClassVersions)
    
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    } else {
        logMessage("warn", "Unable to extract encapsulated command from $command")
    }
}

def zwaveEvent(hubitat.zwave.commands.meterv5.MeterReport report) {
    logMessage("trace", "zwaveEvent(MeterReport) - command: ${report.inspect()}")
    
    []
}

def zwaveEvent(hubitat.zwave.commands.centralscenev3.CentralSceneNotification notification) {
    logMessage("trace", "zwaveEvent(CentralSceneNotification) - command: ${notification.inspect()}")

    def result = []

    result << createEvent(name: "lastPressed", value: notification.sceneNumber, isStateChange: true)

    if (notification.keyAttributes == 0) {
        handleSceneAction("pushed", notification.sceneNumber)
    }

    if (notification.keyAttributes == 1) {
        //getChildDevice("${device.deviceNetworkId}-${notification.sceneNumber}").refresh()
    }

    if (notification.keyAttributes == 2) {
        handleSceneAction("held", notification.sceneNumber)
    }

    result
}

def handleSceneAction(action, sceneNumber) {
    getChildDevice("${device.deviceNetworkId}-${sceneNumber}").parse([[name: action, value: 1, isStateChange: true, descriptionText: "Button ${sceneNumber} was ${action}", type: "physical"]])
}

def zwaveEvent(hubitat.zwave.commands.multichannelv4.MultiChannelCmdEncap command) {
    logMessage("debug", "zwaveEvent(MultiChannelCmdEncap) - command: ${command.inspect()} !!!!")
    
    def encapsulatedCommand = command.encapsulatedCommand(getCommandClassVersions())
    
    if (encapsulatedCommand) { 
        zwaveEvent(encapsulatedCommand, command.sourceEndPoint as Integer)
    }
    else {
        logMessage("error", "zwaveEvent(MultiChannelCmdEncap) - Unable to extract MultiChannel command from: ${command.inspect()}")
        
        []
    }
}

void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport report) {
}

private endpoint(hubitat.zwave.Command command, Integer endPoint) {
    logMessage("debug", "multiChannelCmdEncap destinationEndPoint ${endPoint}")
    
    zwave.multiChannelV4.multiChannelCmdEncap(destinationEndPoint:endPoint).encapsulate(command)
}

void componentOff(childDevice){
    logMessage("debug", "componentOff() - child device: ${childDevice.inspect()}")
    
    def buttonNumber = getChildEndPoint(childDevice)
    
    if (buttonNumber != null) {
        def commands = sequenceCommands([
            endpoint(zwave.switchBinaryV2.switchBinarySet(switchValue: 0x00), buttonNumber)
        ])
        
        sendHubCommand(new hubitat.device.HubMultiAction(commands, hubitat.device.Protocol.ZWAVE))
    }
}

def getChildEndPoint(childDevice) {
    def endPoint = childDevice?.deviceNetworkId?.split('-')[1]
    
    return endPoint == null ? null : endPoint as Integer;
}

void componentOn(childDevice){
    logMessage("debug", "componentOn() - child device: ${childDevice.inspect()}")
    
    def buttonNumber = getChildEndPoint(childDevice)
    
    if (buttonNumber != null) {
        def commands = sequenceCommands([
            endpoint(zwave.switchBinaryV2.switchBinarySet(switchValue: 0xFF), buttonNumber)
        ])
        
        sendHubCommand(new hubitat.device.HubMultiAction(commands, hubitat.device.Protocol.ZWAVE))
    }
}

private sequenceCommands(Collection commands, Integer delayBetweenCommands = 100) {
    logMessage("trace", "sequenceCommands(Command) - commands: ${commands.inspect()} delayBetweenArgs: ${delayBetweenArgs}")
    
    delayBetween(commands.collect{ encapsulateCommand(it) }, delayBetweenCommands)
}

private encapsulateCommand(hubitat.zwave.Command command) {
    logMessage("trace", "command(Command) - command: ${command.inspect()} isSecured(): ${isSecured()} S2: ${isS2()}")

    if (isSecured()) {
        if (isS2()) {
            zwaveSecureEncap(command)
        }
        else {
            zwave.securityV1.securityMessageEncapsulation().encapsulate(command).format()
        }
    }
    else {
        command.format()
    }
}

private isS2() {
    getDataValue("S2") != null
}

private isSecured() {
    getDataValue("zwaveSecurePairingComplete") == "true"
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

private getCommandClassVersions() {
    return [
        0x22: 1, // COMMAND_CLASS_APPLICATION_STATUS
        0x25: 2, // COMMAND_CLASS_SWITCH_BINARY
        0x31: 11, // COMMAND_CLASS_SENSOR_MULTILEVEL
        0x32: 5, // COMMAND_CLASS_METER
        0x55: 2, // COMMAND_CLASS_TRANSPORT_SERVICE
        0x59: 3, // COMMAND_CLASS_ASSOCIATION_GRP_INFO (Secure)
        0x5A: 1, // COMMAND_CLASS_DEVICE_RESET_LOCALLY (Insecure)
        0x5B: 3, // COMMAND_CLASS_CENTRAL_SCENE
        0x5E: 2, // COMMAND_CLASS_ZWAVEPLUS_INFO (Insecure)
        0x60: 4, // COMMAND_CLASS_MULTI_CHANNEL
        0x6C: 1, // COMMAND_CLASS_SUPERVISION
        0x70: 4, // COMMAND_CLASS_CONFIGURATION (Secure)
        0x72: 2, // COMMAND_CLASS_MANUFACTURER_SPECIFIC (Insecure)
        0x73: 1, // COMMAND_CLASS_POWERLEVEL (Insecure)
        0x7A: 5, // COMMAND_CLASS_FIRMWARE_UPDATE_MD (Insecure)
        0x81: 1, // COMMAND_CLASS_CLOCK
        0x85: 2, // COMMAND_CLASS_ASSOCIATION (Secure)
        0x86: 3, // COMMAND_CLASS_VERSION (Insecure)
        0x87: 3, // COMMAND_CLASS_INDICATOR
        0x8E: 3, // COMMAND_CLASS_MULTI_CHANNEL_ASSOCIATION (Secure)
        0x98: 1, // COMMAND_CLASS_SECURITY (Secure)
        0x9F: 1 // COMMAND_CLASS_SECURITY_2 (Secure)
    ]
}