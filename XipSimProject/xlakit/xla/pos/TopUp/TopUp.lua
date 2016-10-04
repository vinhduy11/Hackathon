--[[ XLA:TopUp
-- Description: Implements the TopUp feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

TU_Amount = 0
TU_MPIN = 0
TU_nfcSess = 0
TU_Xid = 0
TU_epid = 0
TU_CBal = nil
TU_XmsConn = 0
TU_TxnId = 0
TU_epoch = 0
TU_TAGepoch = 0
TU_TopupBal = nil
TU_PurseAction = "A"
TU_TopUpErr = 0
rec_id = 0
txnId = 0
array = {}
TopupSucess = 0

function TU_OnLoad ()
	RecStoreAvl = xal_isStoreAvailable()
	xipdbg("TU_OnLoad: RecStoreAvl----"..RecStoreAvl)

	--check Record store available 
	if( RecStoreAvl == 0 ) then
		TU_goHome ()
	else
		DisplayScreenFromRes("topupInputEntryScreen", "#AMT", " ", " ", " ", "1", "16", "TU_OnAmountNext","TU_OnAmountNext" )
		maxAmt = GetPinlessEndAmount()
		DisplaySetMaxInputDataLen("99999")
	end
end

function TU_OnAmountNext (amount)
	DisplaySetMaxInputDataLen("0")
	if( amount ~= nil and tonumber( amount ) > 0 ) then
		TU_Amount	= amount
		DisplayScreenFromRes("topupInputEntryScreen", "#CMPIN", " ", "#CMPIN_1", GetCurrencySymbol()..						TU_Amount, "0", "4", "TU_OnMPINOk", "TU_OnMPINOk" )
	else
		TU_goHome ()
	end 
end

function TU_OnMPINOk (mPIN)
	if( mPIN ~= nil and mPIN:len() == 4 )then  
		TU_MPIN = mPIN
		DisplayScreenFromRes("NFCProgress", "#NFCPRGS", GetCurrencySymbol(),TU_Amount, "TU_OnCancel" )
		TU_nfcSess = sysnfc_init("Topup_OnNFCReadDetectData")	
		if( TU_nfcSess == -1 ) then 
			DisplayScreenFromRes("topupFailedScreen", "#Global:STATUS", " ", " ", "#NFCFAIL", "#TRYAGAIN", 					 	" ", " ", " ", "#Global:OK", " ", "TU_goHome", "TU_goHome" )
		end 
	else
		DisplayScreenFromRes("topupInputEntryScreen", "#CMPIN", "#INCRCT", "#CMPIN_1", GetCurrencySymbol()..TU_Amount, "0", "4", "TU_OnMPINOk", "TU_OnMPINOk" )
	end	
end

function Topup_OnNFCReadDetectData (status, tagdata, tagdatalen)
	TU_Xid = tagdata
	--xipdbg("Topup_OnNFCReadDetectData: TU_Xid "..TU_Xid)
	if (status == "true") then
		TU_epid = GetEpurseID()
		--xipdbg("Topup_OnNFCReadDetectData: epid "..TU_epid)
		sysnfc_svEpurseSetId(TU_nfcSess, TU_epid)
		sysnfc_svEpurseReadAmount(TU_nfcSess,"Topup_OnNFCReadAmountCB")
	else
		DisplayScreenFromRes("topupFailedScreen", "#Global:STATUS", " ", " ", " ", "#TAPTO", " ", " ", " ", "#Global:OK", " ", "TU_goHome", "TU_goHome" )
	end
end

function Topup_OnNFCReadAmountCB(status, amount, expiry, ltamount, lttime)
	--xipdbg("In Lua: Topup_OnNFCReadAmountCB amount:" .. amount)
	--xipdbg("In Lua: Topup_OnNFCReadAmountCB status:" .. status)
	--xipdbg("expiry " .. expiry)
	TU_CBal = sysnfc_svEpurseConvAmtInt2Dec(amount)
	--xipdbg("In Lua: Topup_OnNFCReadAmountCB Card Bal " .. TU_CBal)
	if (status == "1") then
		DisplayScreenFromRes("topupFailedScreen", "#Global:STATUS", " ", " ", "#URCRD", 
						"#TRYAGAIN", " ", " ", "#Global:CNCL", "#Global:RETRY", "TU_OnCancel", "TU_retry", "TU_retry" )
	elseif (status == "7") then
		DisplayScreenFromRes("topupFailedScreen", "#Global:STATUS", " ", " ", "#INVALID1", "#INVALID2", " ", " ", " ", "#Global:OK", " ", "TU_goHome", "TU_goHome" )
	else
		TU_TxnId = generateUUID()
		TU_TAGepoch = expiry
		xipdbg("Topup_OnNFCReadAmountCB: TU_TAGepoch "..TU_TAGepoch)
		StoreValueRecord()
		XmsRequest_TopUp()
	end
end

function TU_retry()
	TU_deInit()
	DisplayScreenFromRes("NFCProgress", "#NFCPRGS", GetCurrencySymbol(),TU_Amount, "TU_OnCancel" )
	TU_nfcSess = sysnfc_init("Topup_OnNFCReadDetectData")	
	if( TU_nfcSess == -1 ) then 
		DisplayScreenFromRes("topupFailedScreen", "#Global:STATUS", " ", " ", "#NFCFAIL", "#TRYAGAIN", 					 	" ", " ", " ", "#Global:OK", " ", "TU_goHome", "TU_goHome" )
	end 
end

function XmsRequest_TopUp()
	cntType = xal_xms_getcontentType()
	--xipdbg("In Lua: XmsRequest_TopUp content Type:".. cntType )
	if( TU_TopUpErr == 0 ) then
		DisplayScreenFromRes("topupProgressScreen",  "#TUPRGS", "#TU", GetCurrencySymbol().." "..TU_Amount, "#Global:TO", TU_Xid, "#Global:CNCL", "TopUpCancel" )
		if( cntType == -1 ) then txnType = "TOPUP".."|".. "7/f"
		else txnType = "TOPUP".."|"..cntType end
		--xipdbg("In Lua: XmsRequest_TopUp txnType:".. txnType )
		TU_XmsConn=xal_xms_init("NULL", txnType, 0, "TU_CB")
	else
		DisplayScreenFromRes("topupProgressScreen",  "#CLPRGS", " ", " ", " ", " ", " ", " " )
		if( cntType == -1 ) then txnType = "TOPERR".."|".. "7/f"
		else txnType = "TOPERR".."|"..cntType end
		--xipdbg("In Lua: XmsRequest_TopUp txnType:".. txnType )
		TU_XmsConn=xal_xms_init("NULL", txnType, 0, "TU_CB")
	end
	xal_xms_add_params( TU_XmsConn, "amt", TU_Amount )
	xal_xms_add_params( TU_XmsConn, "xid", TU_Xid )		
	xal_xms_add_params( TU_XmsConn, "epid", TU_epid )
	xal_xms_add_params( TU_XmsConn, "mp", TU_MPIN )
	exid = GetDeviceExid()
	xal_xms_add_params( TU_XmsConn, "exid", exid  )	
	xal_xms_add_params( TU_XmsConn, "cbal", TU_CBal )	
	xal_xms_add_params( TU_XmsConn, "txnId", TU_TxnId )
	xal_xms_add_params( TU_XmsConn, "tt", "A" )	
	xal_xms_add_params( TU_XmsConn, "ced", TU_TAGepoch )

	ret = xal_xms_request(TU_XmsConn, 1)
end

function TopUpCancel()
	TU_deInit()
	TU_TopUpErr = 1
	XmsRequest_TopUp()
end

function ErrorCancel()
	TU_deInit()	
	DisplayScreenFromRes("topupSuccessScreen", "#TUFAIL", "#CANCL", "#TU", GetCurrencySymbol()..	TU_Amount, " ", " ", " ", " " )
end

function TU_CB ()
	XMSSCData = xal_xms_get_params (TU_XmsConn, "sc")
	SCDetails = mysplit (XMSSCData,"|")
	xmsSC = SCDetails[1]
	uBalAmt = xal_xms_get_params (TU_XmsConn, "cbal")
	--xipdbg("xal_xms_get_params: uBalAmt "..uBalAmt)
	tempAmt = sysnfc_svEpurseConvAmtDec2Int(uBalAmt)
	TU_TopupBal = sysnfc_svEpurseConvAmtInt2Dec( tostring(tempAmt) )
	--xipdbg("xal_xms_get_params: TU_TopupBal "..TU_TopupBal)

	txnId = xal_xms_get_params (TU_XmsConn, "txnId")
	cedVal = xal_xms_get_params (TU_XmsConn, "ced")
	TU_epoch = tonumber(cedVal)
	--xipdbg("TU_CB: TU_epoch "..TU_epoch)
	xal_xms_deInit(TU_XmsConn)
	TU_XmsConn = 0
	
	if tonumber (xmsSC)  ==  0 or tonumber (xmsSC)  ==  0100 then
		if( TU_TopUpErr == 0 ) then
			DisplayScreenFromRes("NFCProgress", "#NFCPRGS1", GetCurrencySymbol(),TU_Amount, "TopUpCancel")
			TU_nfcSess = sysnfc_init("Topup_OnNFCWriteDetectData")
		else
			if( TopupSucess == 0 ) then
				 deleteRecord()
				 DisplayScreenFromRes("topupSuccessScreen", "#TUFAIL", "#CANCL", "#TU", GetCurrencySymbol()..	TU_Amount , " ", " ", " ", " " )
			 end
		end
	elseif tonumber (xmsSC)  ==  8888 then
		if( TU_TopUpErr == 0 ) then 
			TU_TopUpErr = 1
			XmsRequest_TopUp()
		else
			if( TopupSucess == 0 ) then ErrorCancel() end	
		end
	else		
		if( TU_TopUpErr == 0 ) then
			deleteRecord()
			if string.len(SCDetails[2]) > 20 then
				GetMultipleLines(SCDetails[2])
				DisplayScreenFromRes("topupFailedScreen", "#Global:STATUS", xmsSC, array[1], array[2], array[3], array[4], array[5], " ", "#Global:OK", " ", "TU_goHome", "TU_goHome" )
			else
				DisplayScreenFromRes("topupFailedScreen", "#Global:STATUS", xmsSC, SCDetails[2], " ", " ", 						 " ", " ", " ", "#Global:OK", " ", "TU_goHome", "TU_goHome" )
			end
		else
			if( TopupSucess == 0 ) then ErrorCancel() end
		end
	end
end

function Topup_OnNFCWriteDetectData (status, tagdata, tagdatalen)
	xid = tagdata
	if (status == "true") then
		if( xid == TU_Xid ) then
			sysnfc_svEpurseSetId(TU_nfcSess, TU_epid)
			intamt = sysnfc_svEpurseConvAmtDec2Int(TU_TopupBal)
			sysnfc_svEpurseWriteAmount( TU_nfcSess, intamt, TU_epoch, "OnNFCWriteTopUpAmount" )	
		else
			DisplayScreenFromRes("topupFailedScreen", "#Global:STATUS", " ", " ", " ", "#TAPORGTAG", " ", " ", "#Global:CNCL", "#Global:OK", "TopUpCancel", "TU_tapOrg", "TU_tapOrg" )
		end 
	else
		if( TU_TopUpErr == 0 ) then
			DisplayScreenFromRes("topupFailedScreen", "#Global:STATUS", " ", " ", "#TAPTO", 
								 "#TRYAGAIN", " ", " ", "#Global:CNCL", "#Global:RETRY", "TopUpCancel", "TU_tapOrg", "TU_tapOrg" )
		end
	end
end

function TU_tapOrg()
	DisplayScreenFromRes("NFCProgress", "#NFCPRGS1", GetCurrencySymbol(),TU_Amount, "TopUpCancel")
	TU_nfcSess = sysnfc_init("Topup_OnNFCWriteDetectData")
end

function OnNFCWriteTopUpAmount(status)
	--xipdbg("In Lua: OnNFCWriteTopUpAmount status " .. status)

	if( status == "true" ) then
	   TopupSucess = 1
	   TU_deInit()
	   deleteRecord()
	   --xipdbg("xal_xms_get_params: TU_TopupBal "..TU_TopupBal)
	   DisplayScreenFromRes("topupSuccessScreen", "#TUSUCS", "#SUCC", "#TU", GetCurrencySymbol()..TU_Amount, "#CBAL", GetCurrencySymbol()..TU_TopupBal, "#Global:TID",  txnId )
	else
		DisplayScreenFromRes("topupFailedScreen", "#Global:STATUS", " ", " ", "#WRFAIL", 
								 "#TRYAGAIN", " ", " ", "#Global:CNCL", "#Global:RETRY", "TopUpCancel", "TU_tapOrg", "TU_tapOrg" )						 
	end
end

function StoreValueRecord()
	TS = xal_xms_timeStamp()
	Type = "TU"
	TXN_STATUS = "F"
	amtVal = sysnfc_svEpurseConvAmtDec2Int(TU_Amount)
	TU_EntAmt = sysnfc_svEpurseConvAmtInt2Dec( tostring(amtVal) )
	TU_SVversion = "V1"	
	TU_UpTime = GetUpTime()
	TU_Location = GetLocationDetails()
	TU_SignalStr = GetSignalStrength()
	TU_Msisdn = GetDeviceMSISDN()
	
	RecordData = TU_SVversion.."~"..TS.."~"..Type.."~"..TU_TxnId.."~"..TU_epid.."~".. TU_PurseAction .."~"..TU_EntAmt.."~"..TU_Xid.."~"..TXN_STATUS .. "~" .. TU_CBal .."~".. TU_TAGepoch .."~".. TU_UpTime.."~".. TU_Location.."~".. TU_SignalStr .."~".. TU_Msisdn
	iRet, rec_id=xal_addRecord(RecordData,string.len(RecordData),1 )

	-- SV Total txns
	SVCurTxns = GetConfigValue("stt")
	if( SVCurTxns == -1 ) then
		SetConfigValue("stt", "1" )
	else
		SVTotalTxns = tonumber( SVCurTxns ) + 1
		xipdbg("In Lua: StoreValueRecord: TotalTxns = " .. SVTotalTxns )
		SetConfigValue("stt", tostring(SVTotalTxns) )
	end
end

function deleteRecord()
	iRet = xal_deleteRecord( rec_id )
	if( iRet == 0 ) then 
		SVCurTxns = GetConfigValue("stt")
		SVTotalTxns = tonumber( SVCurTxns ) - 1
		SetConfigValue("stt", tostring(SVTotalTxns) )
	end
end

function TU_goHome ()
	ChangeXla("HomeScreen")
end

function TU_OnCancel()
	DisplaySetMaxInputDataLen("0")
	TU_deInit()
	TU_goHome()
end

function TU_deInit()
	if( TU_nfcSess ~= 0 ) then 
		sysnfc_nfc_cancel(TU_nfcSess)
	end
	
	if(TU_XmsConn ~= 0) then 
		xal_xms_deInit(TU_XmsConn)
	end
end

function mysplit(inputstr, sep)
	if sep == nil then
			sep = "%s"
	end
	local t={} ; i=1	
	for str in string.gmatch(inputstr, "([^"..sep.."]+)") do
		t[i] = str	
		i = i + 1
	end
	return t
end

function GetMultipleLines (buf)
	count = 1	
	result = ""
    for word in buf:gmatch("%w+") do
        if(string.len(result)+string.len(word)+1 > 20) then
			array[count]=result
			count = count + 1
            result = word
        elseif(string.len(result)>0) then
            result = result.." "..word
        else
            result = word
        end
    end
	array[count]=result
	count = count + 1
	while(count <= 5) do
		array[count]=" "
		count = count + 1
	end
end

function syncTxnCheck()
	PrevTxnTime = GetConfigValue("SVST")
	currentTxnTime = sysnfc_svEpurseGetEpochNow()
	if( PrevTxnTime == -1 ) then 
		timeInvl = 0
	else
		timeInvl =  currentTxnTime  - tonumber( PrevTxnTime )
	end
	
	SyncInterval = GetConfigValue("sti") 
	SyncMaxTxnNo = GetConfigValue("stc") 
	txnCount=xal_getRecordCount()
	if( ( timeInvl >= tonumber( SyncInterval ) and txnCount >= 1 )  or  txnCount >= tonumber( SyncMaxTxnNo) ) then
		-- update the transaction time
		SetConfigValue("SVST", tostring(currentTxnTime) )
		ChangeXla("SyncSV")
	else
		TU_goHome()
	end
end

