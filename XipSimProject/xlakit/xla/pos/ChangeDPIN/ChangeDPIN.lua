--[[ XLA: ChangeDPIN
-- Description: Implements the Change dPIN feature for the POS XLA bundle. 
-- Date: 2 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

CHNGDPIN_OldDPIN   = 0
CHNGDPIN_NewDPIN   = 0

function CHNGDPIN_OnLoad ()
	xipdbg("Calling DisplayScreenFromRes")
	DisplayScreenFromRes("changeDPinOldDPinEntryScreen", " ")
end

function CHNGDPIN_OnOldDPINNext (dPIN)
	if( dPIN ~= nil and dPIN:len() == 4 ) then
		CHNGDPIN_OldDPIN = dPIN
		DisplayScreenFromRes("changeDPinNewDPinEntryScreen", " ")	
	else
		DisplayScreenFromRes("changeDPinOldDPinEntryScreen", "#INCRCTPIN")
	end
	
end

function CHNGDPIN_OnNewDPINNext (dPIN)
	if( dPIN ~= nil and dPIN:len() == 4 ) then
		CHNGDPIN_NewDPIN = dPIN
		DisplayScreenFromRes("changeDPinReEnterNewDPinEntryScreen", " ")
	else
		DisplayScreenFromRes("changeDPinNewDPinEntryScreen", "#INCRCTPIN" )
	end
	
end

function CHNGDPIN_OnReNewDPINNext (dPIN)
	if( dPIN ~= nil and dPIN:len() == 4 ) then
		-- get the old dPIN from flash
		oldDPIN = GetConfigValue("dPIN")
		-- xipdbg("old device dPIN = " .. oldDPIN )
		if ( ( CHNGDPIN_NewDPIN == dPIN  ) and ( CHNGDPIN_OldDPIN == oldDPIN ) ) then
			-- update new dPIN into flash
			SetConfigValue("dPIN", CHNGDPIN_NewDPIN )
			DispScreen("changeDPinSuccessScreen")
		elseif( CHNGDPIN_NewDPIN ~= dPIN ) then 
			DispScreen("changeDPinDPinsNotMatchedScreen")
		elseif( CHNGDPIN_OldDPIN ~= oldDPIN ) then 
			DispScreen("changeDPinOldDPinNotMatchedScreen")
		end 
	else
		DisplayScreenFromRes("changeDPinReEnterNewDPinEntryScreen", "#INCRCTPIN" )
	end
end

function CHNGDPIN_goHome ()
	ChangeXla("HomeScreen")
end

function DispScreen (scrName)
	DisplayScreenFromRes(scrName)
end

function CHNGDPIN_OnCancel()
	CHNGDPIN_goHome()
end