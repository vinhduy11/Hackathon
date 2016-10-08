--[[ XLA: SendMoney
-- Description: Implements the SM feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

SM_xmsConn = 0
SM_Amount = nil
SM_PHONE  = nil
SM_MPIN = 0
array = {}

function SM_OnLoad ()
	xipdbg("Calling DisplayScreenFromRes")
	--DispScreen("PhoneScreen")
end