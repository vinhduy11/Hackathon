--[[ XLA:TopUp
-- Description: Implements the TopUp feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

TU_Amount = 0
TU_CMND = 0
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
  DisplayScreenFromRes("NFCProgress", "#NFCPRGS", "TU_OnCancel" )
  TU_nfcSess = sysnfc_init("Topup_OnNFCReadDetectData") 
  if( TU_nfcSess == -1 ) then 
    DisplayScreenFromRes("topupFailedScreen", "#Global:STATUS", " ", " ", "#NFCFAIL", "#TRYAGAIN",            " ", " ", " ", "#Global:OK", " ", "TU_goHome", "TU_goHome" )
  end
    
    
	--RecStoreAvl = xal_isStoreAvailable()
	--xipdbg("TU_OnLoad: RecStoreAvl----"..RecStoreAvl)

	--check Record store available 
	--if( RecStoreAvl == 0 ) then
		--TU_goHome ()
	--else
		--DisplayScreenFromRes("topupInputEntryScreen", "#AMT", " ", " ", " ", "1", "16", "TU_OnAmountNext","TU_OnAmountNext" )
		--maxAmt = GetPinlessEndAmount()
		--DisplaySetMaxInputDataLen("99999")
	--end
end

function TU_OnMPINOk (mPIN)
	if( mPIN ~= nil and mPIN:len() == 6 )then  
		TU_MPIN = mPIN
		XmsRequest_TopUp()
	else
	 TU_goHome()
	end
end

function Topup_OnNFCReadDetectData (status, tagdata, tagdatalen)
	TU_Xid = tagdata
	xipdbg("Topup_OnNFCReadDetectData: TU_Xid "..TU_Xid .. " status " ..status .. " tagdata " .. tagdata .. "tagdatalen" .. tagdatalen)
	--sysnfc_svEpurseReadAmount(TU_nfcSess,"Topup_OnNFCReadAmountCB")
	if (status == "true") then
	  DisplayScreenFromRes("MoneyScreenInputAmount")
	
	 -- TU_epid = GetEpurse/ID()
	--  xipdbg("Topup_OnNFCReadDetectData: TU_Xid1 "..TU_Xid .. TU_epid)
		--TU_epid = 100000
		--xipdbg("Topup_OnNFCReadDetectData: epid "..TU_epid)/
	--	sysnfc_svEpurseSetId(TU_nfcSess, TU_epid)
		--xipdbg("Topup_OnNFCReadDetectData1: epid "..TU_epid)
		
		--xipdbg("Topup_OnNFCReadDetectData2: epid "..TU_epid .. "TU_nfcSess " ..TU_nfcSess)
	else
		DisplayScreenFromRes("topupFailedScreen", "#Global:STATUS", " ", " ", " ", "#TAPTO", " ", " ", " ", "#Global:OK", " ", "TU_goHome", "TU_goHome" )
	end
end

function Topup_OnNFCReadAmountCB(status, amount, expiry, ltamount, lttime)
	xipdbg("In Lua: Topup_OnNFCReadAmountCB amount:" .. amount)
	xipdbg("In Lua: Topup_OnNFCReadAmountCB status:" .. status)
	xipdbg("expiry " .. expiry)
	TU_CBal = sysnfc_svEpurseConvAmtInt2Dec(amount)
	xipdbg("In Lua: To/pup_OnNFCReadAmountCB Card Bal " .. TU_CBal)
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
		DisplayScreenFromRes("topupProgressScreen",  "#TUPRGS", "#TU", TU_Amount, "#Global:TO", TU_Xid, "#CNCL", "TopUpCancel" )
		if( cntType == -1 ) then txnType = "TEAM_TEST".."|".. "7/f"
		else txnType = "TEAM_TEST".."|"..cntType end
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
	--xal_xms_add_params( TU_XmsConn, "xid", TU_Xid )		
	xal_xms_add_params( TU_XmsConn, "epid", TU_epid )
	xal_xms_add_params( TU_XmsConn, "mp", TU_MPIN )
	exid = GetDeviceExid()
  
  
	
  xid = GetDeviceXID()
  xal_xms_add_params( TU_XmsConn, "xid", xid )
  mccMnc = GetMncMcc()
  xal_xms_add_params( TU_XmsConn, "mcc", string.sub(mccMnc, 1, 3) )
  xal_xms_add_params( TU_XmsConn, "mnc", string.sub(mccMnc, 4, -1) )
  exid = GetDeviceExid()
  xal_xms_add_params( TU_XmsConn, "exid", exid  )
  xipdbg("Adding Param amount = " .. TU_Amount)   
  xal_xms_add_params( TU_XmsConn, "amt", TU_Amount )
  xipdbg("Adding Param Mode of com= " .. "2")   
  xal_xms_add_params( TU_XmsConn, "com", "2" )
  -- xipdbg("Adding Param MPIN = " .. SM_MPIN)  
  xal_xms_add_params( TU_XmsConn, "mp", TU_MPIN )
  xal_xms_add_params( TU_XmsConn, "msgType", "CASHIN" )
  xal_xms_add_params( TU_XmsConn, "pin", TU_MPIN )
  xal_xms_add_params( TU_XmsConn, "cardId", TU_Xid )
  xal_xms_add_params( TU_XmsConn, "cmnd", TU_CMND )
  xal_xms_add_params( TU_XmsConn, "balance", TU_Amount )
  xal_xms_add_params( TU_XmsConn, "createDate", "" )
  xal_xms_add_params( TU_XmsConn, "lastUpade", "" )
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
  xipdbg("In Lua: perso Status" .. xmsSC)
  
  txnId = xal_xms_get_params (TU_XmsConn, "txnId")  
  errorDesc = xal_xms_get_params (TU_XmsConn, "errorDesc")
  balance = xal_xms_get_params (TU_XmsConn, "balance")
  xal_xms_deInit(SM_xmsConn)
  SM_xmsConn = 0
  
  if tonumber (xmsSC)  ==  0 then
    if errorDesc == "THANH CONG" then
        xipdbg("In Lua: perso Status" .. errorDesc)
        DisplayScreenFromRes("topupScreen", errorDesc, "So du hien tai:".. balance)
    else
        DisplayScreenFromRes("topupScreen", "",errorDesc)
    end
  elseif tonumber (xmsSC)  ==  8888 then
    DisplayScreenFromRes("balanceTimeout")
  else
    if string.len(SCDetails[2]) > 20 then
      GetMultipleLines(SCDetails[2])
      DisplayScreenFromRes("balanceFailureScreen", xmsSC, array[1], array[2], array[3], array[4], array[5] )
    else
      DisplayScreenFromRes("balanceFailureScreen", xmsSC, SCDetails[2] )
    end
  end
  
  DisplayScreenFromRes("sendMoneyFailure", xmsSC,errorDesc )
 
end

function Topup_OnNFCWriteDetectData (status, tagdata, tagdatalen)
	xid = tagdata
	if (status == "true") then
		if( xid == TU_Xid ) then
			sysnfc_svEpurseSetId(TU_nfcSess, TU_epid)
			intamt = sysnfc_svEpurseConvAmtDec2Int(TU_TopupBal)
			sysnfc_svEpurseWriteAmount( TU_nfcSess, intamt, TU_epoch, "OnNFCWriteTopUpAmount" )	
			xipdbg("xal_xms_get_params: TU_epid "..TU_epid)
      xipdbg("xal_xms_get_params: TU_TopupBal "..TU_TopupBal)
      xipdbg("xal_xms_get_params: TU_epoch "..TU_epoch)
      xipdbg("xal_xms_get_params: xid "..xid)
			
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

function TU_OnCMNDNext(cmnd)
  if( cmnd ~= nil and cmnd:len() == 9 )then
    TU_CMND = cmnd
    DisplayScreenFromRes("topupInputEntryScreen")
  else
    DisplayScreenFromRes("CMNDEntryScreen", "#CMND_E")
  end 
end

function TU_OnAmountNext (amount)
  DisplaySetMaxInputDataLen("0")
  -- xipdbg("In Lua: Amouont = " .. amount)
  TU_Amount = amount
  if( TU_Amount ~= nil and tonumber( TU_Amount ) > 0 ) then 
    xipdbg("In Lua: Displaying sendMoneyMPinEntryScreen: amount = " .. TU_Amount)
    DisplayScreenFromRes("topupInputEntryScreen", " ", "So tien:"..TU_Amount, "")
  else
    TU_goHome ()
  end
end

