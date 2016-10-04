--[[ XLA:DPIN
-- Description: Implements the DPIN entry screen for the POS XLA bundle. 
-- "0000" is the default PIN . All other values result in a buzz
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

array = {}

DPIN_devName = nil
dPinReset = "DPINRESET"
d1PinReset = "D1PINRESET"
DPIN_ResetSuccess = 0

function DPIN_OnLoad ()
	DPIN_devName = GetConfigValue("devName")
	ret = GetSessionValue ("dPinRstReg")
	TrigDetails = GetTrigDetails (d1PinReset)
	if (tonumber (TrigDetails) ~= -1) then
		xipdbg("D1PIN RESET Trigger Available: Message =  = " .. TrigDetails)
		SMSTriggerDPINResetCB (0, "0000", TrigDetails)
	else
		TrigDetails = GetTrigDetails (dPinReset)
		if (tonumber (TrigDetails) ~= -1) then
			xipdbg("DPIN RESET Trigger Available: Message =  = " .. TrigDetails)
			SMSTriggerDPINResetCB (0, "0000", TrigDetails)
		end
	end
	
	xipdbg("dPinRstReg = " .. ret )
	if(ret == -1) then
		XIP_SMS_TRIGGER = 0
		RegisterSmsUssdTriggerHandler(XIP_SMS_TRIGGER, "919916909072", dPinReset, "SMSTriggerDPINResetCB")
		RegisterSmsUssdTriggerHandler(XIP_SMS_TRIGGER, "919916909072", d1PinReset, "SMSTriggerDPINResetCB")
		SetSessionValue ("dPinRstReg", "1")
	end

	if (DPIN_ResetSuccess == 0) then
		if( DPIN_devName == -1 ) then
			DisplayScreenFromRes("DPINEntryScreen", " ", " ")
		else
			DisplayScreenFromRes("DPINEntryScreen", " ", DPIN_devName )
		end
	end
end

function DPIN_OnFetchTrigger ()
	ChangeXla("FetchTrigger")
end

function DPIN_OnOk(DPIN)
	-- xipdbg("DPIN called with = " .. DPIN )
	dPIN = GetConfigValue("dPIN")
	-- xipdbg("DPIN called with = " .. dPIN )
	if( dPIN == -1 ) then 
		-- xipdbg("In Lua: dPIN initialized to 0000")
		SetConfigValue("dPIN", "0000" )
		dPIN = GetConfigValue("dPIN")
		-- xipdbg("DPIN called with = " .. dPIN )
	end

	if (DPIN == dPIN and DPIN:len() == 4 ) then
		ChangeXla("HomeScreen")
	else		
		DPIN_Error()
		if( DPIN_devName == -1 ) then
			DisplayScreenFromRes("DPINEntryScreen", "#INCRCTPIN", " ")
		else
			DisplayScreenFromRes("DPINEntryScreen", "#INCRCTPIN", DPIN_devName )
		end
	end

end

function DPIN_Error ()
	xip_buzzer_viberator_sound( 1000 )
end

function SMSTriggerDPINResetCB ( typeSMS, source, pattern )
	xipdbg("SMSTrgDPINResetCB source:" .. source)
	xipdbg("SMSTrgDPINResetCB patten:" .. pattern)
	if( string.match( pattern, dPinReset ) ~= nil ) then 
		dPIN = string.sub(pattern, ( string.len(pattern) - 3 ), -1)
		xipdbg("dPIN reset" .. dPIN)
		SetConfigValue("dPIN", dPIN )
		ResetTrigDetails(dPinReset)
		DPIN_ResetSuccess = 1
		DisplayScreenFromRes("DPINResetSuccessScreen")
	end
	
	if( string.match( pattern, d1PinReset ) ~= nil ) then 
		dPIN = string.sub(pattern, ( string.len(pattern) - 3 ), -1)
		xipdbg("dPIN reset" .. dPIN)
		SetConfigValue("dPIN", dPIN )
		ResetTrigDetails(d1PinReset)
		DPIN_ResetSuccess = 1
		DisplayScreenFromRes("DPINResetSuccessScreen")
	end

end 

function DPINReset_OnOk()
	DPIN_ResetSuccess = 0
	DPIN_OnLoad ()
end 

