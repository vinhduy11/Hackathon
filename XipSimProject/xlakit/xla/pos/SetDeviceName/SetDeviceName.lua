--[[ XLA: SetDeviceName
-- Description: Implements the SetDeviceName feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

function SDN_OnLoad ()
	xipdbg("Calling DisplayScreenFromRes")
	
	--  get the Old device name from flash
	devOldName = GetConfigValue("devName")
	if( devOldName == -1 ) then
		DisplayScreenFromRes("deviceNameEntryScreen", " " )
	else
		DisplayScreenFromRes("deviceNameEntryScreen", devOldName )
	end 
end

function SDN_OnDevNameOk (devName)

	if( devName ~= nil ) then 
		xipdbg("Calling deviceNameSuccessScreen, device name = " .. devName )
		--  update new device name into flash
		SetConfigValue("devName", devName )
		DispScreen("deviceNameSuccessScreen")		
	else
		DisplayScreenFromRes("deviceNameFailureScreen", "#IMPRDATA" )
	end 
end

function SDN_goHome ()
	ChangeXla("HomeScreen")
end

function DispScreen (scrName)
	xipdbg("DispScreen: Calling DisplayScreenFromRes for screen   " .. scrName )
	DisplayScreenFromRes(scrName)
end

function SDN_OnCancel()
	SDN_goHome()
end
