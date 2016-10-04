--[[ XLA:PreferencesMenu
-- Description: Implements the Preferences Menu screen for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

function PRM_OnLoad ()
	xipdbg("Calling DisplayScreenFromRes")
	DisplayScreenFromRes("preferencesMenuScreen")
end

function PRM_OnMenuBtn1 ()
	xipdbg("In Lua: PRM_OnMenuBtn1 ChangeDPIN")
	ChangeXla ("ChangeDPIN")
end

function PRM_OnMenuBtn2 ()
	xipdbg("In Lua: PRM_OnMenuBtn2 SetDeviceName")
	ChangeXla ("SetDeviceName")
end

function PRM_OnMenuBtn3 ()
	xipdbg("In Lua: PRM_OnMenuBtn3 SysInfo")
	ChangeXla ("SysInfo")
end

function PRM_OnMenuBtn4 ()
	xipdbg("In Lua: PRM_OnMenuBtn4 SetAuth")
	ChangeXla ("SetAuth")
end

function PRM_OnMenuBtn5 ()
	xipdbg("In Lua: PRM_OnMenuBtn5 GetConfig")
	ChangeXla ("GetConfig")
end


function PRM_OnMenuBtn6 ()
	xipdbg("In Lua: PRM_OnMenuBtn6 LanguageSel")
	ChangeXla ("LanguageSel")
end

function PRM_OnMenuBtn7 ()
	xipdbg("In Lua: PRM_OnMenuBtn7 Server Sync")
	ChangeXla ("ServerSync")
end

function PRM_OnMenuBtn8 ()
	xipdbg("In Lua: PRM_OnMenuBtn8 Printer setup")
	ChangeXla ("PrinterSetup")
end

function PRM_goHome()
	ChangeXla("HomeScreen")
end