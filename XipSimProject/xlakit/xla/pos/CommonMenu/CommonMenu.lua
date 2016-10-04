--[[ XLA:MWCommonMenu
-- Description: Implements the Common Menu screens for the POS XLA bundle. 
-- Shows  transactions list and preferences menu
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

function CM_OnLoad ()
	DisplayScreenFromRes("CommonMenuScreen")
end

function OnGenericMenu()
	MenuText,MenuKeyIndex = GetMenuKeyIndex()
	if (MenuKeyIndex == 1001 ) then
		ChangeXla ("SendMoney")
	elseif (MenuKeyIndex == 1002 ) then
		ChangeXla ("ChangeMPIN")
	elseif (MenuKeyIndex == 1003 ) then
		ChangeXla ("SyncSV")
	elseif (MenuKeyIndex == 1004 ) then
		ChangeXla ("PreferencesMenu")
	end
end

function CM_goHome()
	ChangeXla ("HomeScreen")
end
