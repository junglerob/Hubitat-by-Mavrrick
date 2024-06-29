// Hubitat driver for Govee Appliances using Cloud API
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

// Includes of library objects
#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_RGB
#include Mavrrick.Govee_Cloud_Level
#include Mavrrick.Govee_Cloud_Life
//#include Mavrrick.Govee_Cloud_MQTT

import groovy.json.JsonSlurper 

metadata {
	definition(name: "Govee v2 Humidifier with RGB Driver", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
        capability "ColorControl"        
		capability "Actuator"
        capability "Initialize"
        capability "Refresh"
        capability "RelativeHumidityMeasurement"
        capability "SwitchLevel"
        capability "LightEffects"
        capability "Configuration"
        attribute "lackWaterEvent", "string"         
        
		attribute "gear", "number"
        attribute "mode", "number"
        attribute "modeValue", "number"
        attribute "modeDescription", "string"
        attribute "pollInterval", "number" 
        attribute "online", "string"
        attribute "cloudAPI", "string"
        attribute "nightLight", "string"
//        attribute "connectionState", "string"        
        attribute "lackWaterEvent", "string"        
        
        command "nightLighton_off", [[name: "Night Light", type: "ENUM", constraints: [ 'On',      'Off'] ] ]
        command "workingMode", [[name: "mode", type: "ENUM", constraints: [ 'Manual',      'Custom',       'Auto'], description: "Mode of device"],
                          [name: "gearMode", type: "NUMBER", description: "When Mode is Manual sets hudifier speed. When set to Auto sets desired Humidity"]]        
        command "changeInterval", [[name: "changeInterval", type: "NUMBER",  description: "Change Polling interval range from 0-600", range: 0-600, required: true]]
        
    }

	preferences {		
		section("Device Info") {
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)  
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
            input("descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true) 
		}
		
	}
}

//////////////////////////////////////
// Standard Methods for all drivers //
//////////////////////////////////////

// reset of device settings when preferences updated.
def updated() {
    if (debugLog) {log.info "updated(): device updated "}
    unschedule()
    if (debugLog) runIn(1800, logsOff)
    retrieveStateData()
    poll()
/*    disconnect()
	pauseExecution(1000)
    mqttConnectionAttempt() */
}

// linital setup when device is installed.
def installed(){
    retrieveStateData()
    poll()
/*    disconnect()
    pauseExecution(1000)
    mqttConnectionAttempt() */
}

// initialize devices upon install and reboot.
def initialize() {
     if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
    }
    unschedule()
    if (debugLog) runIn(1800, logsOff)
    retrieveStateData()
    if (pollRate > 0) {
        pollRateInt = pollRate.toInteger()
        randomOffset(pollRateInt)
        runIn(offset,poll)
    }
//    poll()
/*    disconnect()
    pauseExecution(1000)
    mqttConnectionAttempt() */
}

// update data for the device
def refresh() {
    if (debugLog) {log.info "refresh(): Performing refresh"}
    unschedule(poll)
    poll()
    if (device.currentValue("connectionState") == "connected") {
    }
}

// retrieve setup values and initialize polling and logging
def configure() {
    if (debugLog) {log.info "configure(): Driver Updated"}
    unschedule()
    if (pollRate > 0) runIn(pollRate,poll)     
    retrieveStateData()    
    if (debugLog) runIn(1800, logsOff)
/*    disconnect()
    pauseExecution(1000)
    mqttConnectionAttempt()  */
}

////////////////////
// Helper methods //
////////////////////

logsOff  // turn off logging for the device
def logsOff() {
    log.info "debug logging disabled..."
    device.updateSetting("debugLog", [value: "false", type: "bool"])
}

poll // retrieve device status
def poll() {
    if (debugLog) {log.info "poll(): Poll Initated"}
	getDeviceState()
    if (pollRate > 0) runIn(pollRate,poll)
}	

//////////////////////
// Driver Commands // 
/////////////////////

def on() {
    cloudOn()
}

def off() {
    cloudOff()
}

def setLevel(float v,duration = 0) {
    cloudSetLevel( v, 0)
}

def workingMode(mode, gear){
    log.debug "workingMode(): Processing Working Mode command. ${mode} ${gear}"
    sendEvent(name: "cloudAPI", value: "Pending")
    switch(mode){
        case "Manual":
            modenum = 1;
        
        break;
        case "Custom":
            modenum = 2;
            gear = 0;
        break;
        case "Auto":
            modenum = 3;
        break;
    default:
    log.debug "not valid value for mode";
    break;
    }
    values = '{"workMode":'+modenum+',"modeValue":'+gear+'}'
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}  

