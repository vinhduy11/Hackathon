--[[ XLA: WakeUp
-- Description: Implements the WakeUp feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

function W_OnLoad ()
	xipdbg("In Lua: XPW_OnLoad")

	SyncMinTxnCnt = GetMiniRecordCount() 
	xipdbg("In Lua: SyncMinTxnCnt: " .. SyncMinTxnCnt )
	
	txnCount = xal_getRecordCount()
	xipdbg("In Lua: txnCount is ----"..txnCount)
	
	if(  txnCount >= tonumber( SyncMinTxnCnt) ) then
		-- start store value sync up txn  
		SetSessionValue("SyncUp", "1" )
		ChangeXla ("SyncSV")
	else
		ChangeXla ("Home")
	end
end



