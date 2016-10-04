--[[ XLA:BalanceEnquiry
-- Description: Implements the BE feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

BE_xmsConn = 0
BE_MPIN    = 0
BE_nfcSess = 0
BE_Xid	   = 0
BE_Exid	   = 0
BE_Type	   = 0

-- variables to display language type strings
BE_SVAmount = 0

function BE_OnLoad ()
	collectgarbage("collect")
	xipdbg("In Lua: garbage count= " .. collectgarbage("count"))

	BE_SVAmount = GetConfigValue("sta")
	totTxn = GetConfigValue("stt")
	lastSync = GetConfigValue("lss")
	
	DisplayScreenFromRes("NFCProgress")
	BE_nfcSess = sysnfc_init("BE_OnNFCReadDetectData")
	xipdbg("In Lua: BE_OnLoad " .. BE_nfcSess)
	if( BE_nfcSess == -1 ) then 
		DisplayScreenFromRes("CommonStatusScreen", "#Global:STATUS", " ", " ", "#NFCFAIL", "#TRYAGAIN", 					 	" ", " ", " ", "#Global:OK", " ", "BE_goHome", "BE_goHome" )
	end 
end

function BE_OnNFCReadDetectData (status, tagdata, tagdatalen)
	BE_Xid = tagdata
	xipdbg("In Lua: OnNFCReadData " .. status)
	
	if (status == "true") then
		ePurseId = GetEpurseID()
		xipdbg("In Lua: RCV_OnNFCGetCardData ePurseId " .. ePurseId)
		sysnfc_svEpurseSetId(BE_nfcSess, ePurseId )
		sysnfc_svEpurseReadAmount(BE_nfcSess,"BE_OnNFCReadAmountCB")
	else
		DisplayScreenFromRes("CommonStatusScreen", "#Global:STATUS", " ", " ", " ", "#TAPTO", " ", " ", " ", "#Global:OK", " ", "BE_goHome", "BE_goHome" )
	end
end

function BE_OnNFCReadAmountCB(status, amount, expiry)
	xipdbg("In Lua: BE_OnNFCReadAmountCB amount:" .. amount .. "status " .. status)
	xipdbg("In Lua: BE_OnNFCReadAmountCB expiry:" .. expiry)
	BE_CBal = sysnfc_svEpurseConvAmtInt2Dec(amount)
	BE_nfcSess = 0
	if (status == "1") then
		DisplayScreenFromRes("CommonStatusScreen", "#Global:STATUS", " ", " ", "#URCRD", 
						"#TRYAGAIN", " ", " ", "#Global:CNCL", "#Global:RETRY", "BE_OnCancel", "BE_retry", "BE_retry" )
	elseif(status == "2" or status == "6" or status == "7") then
		DisplayScreenFromRes("CommonStatusScreen", "#Global:STATUS", " ", " ", "#INVALID1", "#INVALID2", " ", " ", " ", "#Global:OK", " ", "BE_goHome", "BE_goHome" )
	elseif(sysnfc_svEpurseGetEpochNow() > tonumber( expiry ) ) then
		DisplayScreenFromRes("CommonStatusScreen", "#Global:STATUS", " ", " ", " ", "#EXPIRED", " ", " ", " ", "#Global:OK", " ", "BE_goHome", "BE_goHome" )
	else
		if( BE_CBal ~= 0 ) then
			datetime = sysnfc_svEpurseGetDateTimeFromEpoch(expiry)
			Date = string.sub(datetime, 1, 2).."/"..string.sub(datetime, 3, 4).."/"..string.sub(datetime, 5, 8).." "..string.sub(datetime, 9, 10)..":"..string.sub(datetime, 11, 12)..":"..string.sub(datetime, 13, 14)
			xipdbg("In Lua: BE_OnNFCReadAmountCB datetime:" .. datetime.."Date " ..Date)
			DisplayScreenFromRes("balanceCustSuccessScreen", "#BAL1", "#SVCUSTBAL0", GetCurrencySymbol().." "..BE_CBal, "#EXPIRYDATE", Date)
		else
			DisplayScreenFromRes("CommonStatusScreen", "#Global:STATUS", " ", " ", " ", "#SVBAL5", " ", " ", " ", "#Global:OK", " ", "BE_goHome", "BE_goHome" )
		end	
	end
end

function BE_retry()
	if(BE_nfcSess ~= 0) then 
		sysnfc_nfc_cancel(BE_nfcSess)
	end
	DisplayScreenFromRes("NFCProgress")
	BE_nfcSess = sysnfc_init("BE_OnNFCReadDetectData")
	xipdbg("In Lua: BE_OnLoad " .. BE_nfcSess)
	if( BE_nfcSess == -1 ) then 
		DisplayScreenFromRes("CommonStatusScreen", "#Global:STATUS", " ", " ", "#NFCFAIL", "#TRYAGAIN", 					 	" ", " ", " ", "#Global:OK", " ", "BE_goHome", "BE_goHome" )
	end 
end

function BE_goHome ()
	ChangeXla("HomeScreen")
end

function BE_OnCancel()
	if(BE_nfcSess ~= 0) then 
		sysnfc_nfc_cancel(BE_nfcSess)
	end

	BE_goHome()
end

