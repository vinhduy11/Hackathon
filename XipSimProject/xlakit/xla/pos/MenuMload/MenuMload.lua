--[[ XLA:MWCommonMenu
-- Description: Implements the Common Menu screens for the POS XLA bundle. 
-- Shows  transactions list and preferences menu
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

function MM_OnLoad ()
	DisplayScreenFromRes("MenuMloadScreen")
end

function OnGenericMenu()
	MenuText,MenuKeyIndex = GetMenuKeyIndex()
	xipdbg("MenuKeyIndex".. MenuKeyIndex )
	if (MenuKeyIndex == 1001 ) then
    xipdbg("MLTYPE ".. '1' )
    SetConfigValue ("MLTYPE", "1")
	elseif (MenuKeyIndex == 1002 ) then
	  SetConfigValue ("MLTYPE", "2")
	  xipdbg("MLTYPE ".. '2' )
	end
	ChangeXla ("SendMoneyMLoad")
end

function MM_goHome()
	ChangeXla ("HomeScreen")
end
