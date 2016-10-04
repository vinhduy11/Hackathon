--[[ XLA:Receive
-- Description: Implements the Receive feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

RCV_Amount = 0
RCV_nfcSess = 0
RCV_PurseAction = "A"
RCV_TagBal = 0
RCV_TagExpDate = 0
ePurseId = 0
RCV_Xid = 0
RCV_SVXid = 0
array = {}
RCV_MenuFlag = 0

function RCV_OnLoad ()
	SVStatus = GetSVStatus()
	RecStoreAvl = xal_isStoreAvailable()
	xipdbg("RCV_OnLoad: RecStoreAvl----"..RecStoreAvl)
	--check Record store available 
	if( RecStoreAvl == 0 ) then
		RCV_goHome ()
	else
		MaxRecCount = GetConfigValue("xat")
		xipdbg("RCV_OnLoad: MaxRecCount is ----"..MaxRecCount)
		txnCount=xal_getRecordCount()
		xipdbg("RCV_OnLoad: txnCount is ----"..txnCount)
		if( txnCount >= tonumber( MaxRecCount ) ) then
			DisplayScreenFromRes("receiveSyncScreen", " ", " ")
		else
			RCV_amount()
		end
	end
end

function RCV_SyncNow()
	ChangeXla("SyncSV")
end

function RCV_amount()
	ret = GetSessionValue ("rcvamount")
	DisplayScreenFromRes("receiveAmountEntryScreen")
	maxAmt = GetDeviceMaxAmount()
	DisplaySetMaxInputDataLen(maxAmt)
end

function RCV_OnAmountNext (amount)
	DisplaySetMaxInputDataLen("0")
	if( amount ~= nil and tonumber( amount ) > 0 ) then
		RCV_Amount = amount	
		DisplayScreenFromRes("NFCProgress", GetCurrencySymbol(),RCV_Amount)
--			xipdbg("In Lua: RCV_OnAmountNext " .. RCV_Amount)
		RCV_nfcSess = sysnfc_init("RCV_OnNFCGetCardData")
		if( RCV_nfcSess == -1 ) then 
			DisplayScreenFromRes("receiveFailureScreen", " ", " ", "#NFCFAIL", "#TRYAGN")
		end 
	else
		RCV_goHome ()
	end 
end

function RCV_OnNFCGetCardData(status, tagdata, tagdatalen)
	RCV_Xid = tagdata
--	xipdbg("In Lua: RCV_OnNFCGetCardData RCV_Xid " .. RCV_Xid)
--	xipdbg("In Lua: RCV_OnNFCGetCardData " .. status)
	ePurseId = GetEpurseID()
--	xipdbg("In Lua: RCV_OnNFCGetCardData ePurseId " .. ePurseId)
	sysnfc_svEpurseSetId(RCV_nfcSess, ePurseId )
	
	if (status == "true") then
		intamt = sysnfc_svEpurseConvAmtDec2Int(RCV_Amount)
--			xipdbg("In Lua: RCV_OnNFCGetCardData intamt " .. intamt)
		SVPMMax = GetSVPMMaxAmount()
		if( intamt > sysnfc_svEpurseConvAmtDec2Int( SVPMMax ) ) then
			DisplayScreenFromRes("receiveFailureScreen", " ", " ", "#PMMAX", "#LTEXD" )
		else
			RCV_OfflineFlowHandling()
		end
	else
		DisplayScreenFromRes("receiveFailureScreen", " ", " ", "#TAPTO")
	end
end

function RCV_OfflineFlowHandling()
--		xipdbg("In Lua: YN RCV_OnLoad RCV_Xid " .. RCV_Xid )
	intamt = sysnfc_svEpurseConvAmtDec2Int(RCV_Amount)
--		xipdbg("In Lua: RCV_OnLoad intamt " .. intamt)
	SVPMMax = GetSVPMMaxAmount()
	if( intamt > sysnfc_svEpurseConvAmtDec2Int(SVPMMax) ) then
		DisplayScreenFromRes("receiveFailureScreen", "#Global:STATUS", " ", " ", "#PMMAX", "#LTEXD", " ", " ", " ", "#Global:OK", " ", "RCV_goHome", "RCV_goHome" )
	else
		ePurseId = GetEpurseID()
--			xipdbg("In Lua: RCV_OnLoad ePurseId " .. ePurseId)
		sysnfc_svEpurseSetId(RCV_nfcSess, ePurseId )
		sysnfc_svEpurseDebitAmount(RCV_nfcSess, intamt, "StoreValueRecord")
	end
end

function RCV_offlineReadDetectCard(status, tagdata, tagdatalen)
	RCV_SVXid = tagdata
	RCV_Xid = GetSessionValue("RCVSVXID")

--	xipdbg("In Lua: RCV_offlineReadDetectCard RCV_SVXid " .. RCV_SVXid .. "RCV_Xid " .. RCV_Xid )
	
	if (status == "true") then
		if( RCV_SVXid == RCV_Xid ) then 
			intamt = sysnfc_svEpurseConvAmtDec2Int(RCV_Amount)
--			xipdbg("In Lua: RCV_offlineReadDetectCard intamt " .. intamt)
			SVPMMax = GetSVPMMaxAmount()
			if( intamt > sysnfc_svEpurseConvAmtDec2Int(SVPMMax) ) then
				DisplayScreenFromRes("receiveFailureScreen", "#Global:STATUS", " ", " ", "#PMMAX", "#LTEXD", " ", " ", " ", "#Global:OK", " ", "RCV_goHome", "RCV_goHome" )
			else
				ePurseId = GetEpurseID()
--				xipdbg("In Lua: RCV_OnNFCGetCardData ePurseId " .. ePurseId)
				sysnfc_svEpurseSetId(RCV_nfcSess, ePurseId )
				sysnfc_svEpurseDebitAmount(RCV_nfcSess, intamt, "StoreValueRecord")
			end
		else
			DisplayScreenFromRes("receiveSVMultiTagFailureScreen")
		end
	else
		DisplayScreenFromRes("receiveFailureScreen", "#Global:STATUS", " ", " ", " ", "#TAPTO", " ", " ", " ", "#Global:OK", " ", "RCV_goHome", "RCV_goHome" )
	end
end

function RCV_OnSVTagFailOK()
	if( RCV_nfcSess ~= 0 ) then 
		sysnfc_nfc_cancel(RCV_nfcSess)
	end

--	xipdbg("In Lua: RCV_OnSVTagFailOK ")
	DisplayScreenFromRes("NFCProgress", GetCurrencySymbol(),RCV_Amount)
	RCV_nfcSess = sysnfc_init("RCV_offlineReadDetectCard")
end

function RCV_goHome ()
	ChangeXla("HomeScreen")
end

function RCV_OnCancel()
	DisplaySetMaxInputDataLen("0")
	
	if( RCV_nfcSess ~= 0 ) then 
		sysnfc_nfc_cancel(RCV_nfcSess)
	end
	RCV_goHome()
end

function StoreValueRecord( status, amount, expiryDate )
	xipdbg("In Lua: StoreValueRecord amount " .. amount)
	xipdbg("In Lua: StoreValueRecord status " .. status)
	xipdbg("expiryDate " .. expiryDate )
	
	if(status == "0") then
		TS = xal_xms_timeStamp()
		Type = "PM"
		TxnId = generateUUID()
		TXN_STATUS = "S"
		RCV_TagBal = sysnfc_svEpurseConvAmtInt2Dec( amount )
		RCV_TagExpDate = expiryDate
		amtVal = sysnfc_svEpurseConvAmtDec2Int(RCV_Amount)
		RCV_EntAmt = sysnfc_svEpurseConvAmtInt2Dec( tostring(amtVal) )

		RCV_SVversion = "V1"
		RCV_UpTime = GetUpTime()
		RCV_Location = GetLocationDetails()
		RCV_SignalStr = GetSignalStrength()
		RCV_Msisdn = GetDeviceMSISDN()
	
		RecordData = RCV_SVversion.."~"..TS.."~"..Type.."~"..TxnId.."~"..GetEpurseID().."~".. RCV_PurseAction .."~"..RCV_EntAmt.."~"..RCV_Xid.."~"..TXN_STATUS .. "~" .. RCV_TagBal .."~".. RCV_TagExpDate .."~".. RCV_UpTime.."~".. RCV_Location.."~".. RCV_SignalStr .."~".. RCV_Msisdn
--		xipdbg("In Lua: StoreValueRecord: RecordData = " .. RecordData )
--		xipdbg("In Lua: StoreValueRecord: RecordData = " ..RCV_Xid.."|"..TXN_STATUS .. "|" .. RCV_TagBal .."|".. RCV_TagExpDate )
--		xipdbg("In Lua: StoreValueRecord: RecordData = " ..RCV_UpTime.."|"..RCV_Location .. "|" .. RCV_SignalStr .."|".. RCV_Msisdn )

--		xipdbg("In Lua: StoreValueRecord1: Record len ----" .. string.len(RecordData) )
		iRet, rec_id=xal_addRecord(RecordData,string.len(RecordData),1 )
--		xipdbg("In Lua: StoreValueRecord: Record id ----" .. rec_id)
--		xipdbg("In Lua: StoreValueRecord: iRet ----" .. iRet)

		SVCurAmt = GetConfigValue("sta")
--		xipdbg("In Lua: StoreValueRecord: SVCurAmt = " .. SVCurAmt )
		if( SVCurAmt == -1 ) then
			SetConfigValue("sta", RCV_Amount )
		else
			SVTotalAmt = sysnfc_svEpurseConvAmtDec2Int(SVCurAmt) + sysnfc_svEpurseConvAmtDec2Int(RCV_Amount)
			totalamt = sysnfc_svEpurseConvAmtInt2Dec(SVTotalAmt)
--			xipdbg("In Lua: StoreValueRecord: TotalAmt = " .. totalamt )
			SetConfigValue("sta", totalamt )
		end

		SVCurTxns = GetConfigValue("stt")
--		xipdbg("In Lua: StoreValueRecord: SVCurTxns = " .. SVCurTxns )
		if( SVCurTxns == -1 ) then
			SetConfigValue("stt", "1" )
		else
			SVTotalTxns = tonumber( SVCurTxns ) + 1
--			xipdbg("In Lua: StoreValueRecord: TotalTxns = " .. SVTotalTxns )
			SetConfigValue("stt", tostring(SVTotalTxns) )
		end

--		xipdbg("In Lua: Enable and Disable LED and Vibrator" )
		xip_led_viberator_enable()
		tagXid = string.sub(RCV_Xid, 1, 1).."-"..string.sub(RCV_Xid, 2, 5).."-".."xxxx".."-"..string.sub(RCV_Xid, 14, 17)
		DisplayScreenFromRes("receiveSuccessScreen", GetCurrencySymbol().." "..RCV_Amount, tagXid )
	elseif(status == "1") then
		DisplayScreenFromRes("receiveFailureScreen", "#Global:STATUS", " ", " ", "#UNREAD", 
						"#TRYAGN", " ", " ", "#Global:CNCL", "#Global:RETRY", "RCV_OnCancel", "RCV_retry", "RCV_retry" )
	elseif(status == "2" or status == "6" or status == "7") then
		DisplayScreenFromRes("receiveFailureScreen", "#Global:STATUS", " ", "#INVLDCARD", "#INVLDCARD1", " ", " ", " ", " ", "#Global:OK", " ", "RCV_goHome", "RCV_goHome" )
	elseif(status == "3") then
		DisplayScreenFromRes("receiveFailureScreen", "#Global:STATUS", " ", " ", "#CARDEXPR", " ", " ", " ", " ", "#Global:OK", " ", "RCV_goHome", "RCV_goHome" )
	elseif(status == "5") then
		DisplayScreenFromRes("receiveFailureScreen", "#Global:STATUS", " ", " ", "#INSBAL", " ", " ", " ", " ", "#Global:OK", " ", "RCV_goHome", "RCV_goHome" )
	else
		DisplayScreenFromRes("receiveFailureScreen", "#Global:STATUS", " ", " ", "#NOSV", "#ONLNRCV", " ", " ", " ", "#Global:OK", " ", "RCV_goHome", "RCV_goHome" )
	end 
end 

function syncTxnCheck()
	PrevTxnTime = GetConfigValue("SVST")
--	xipdbg("In Lua: PrevTxnTime  ----"..PrevTxnTime)
	currentTxnTime = sysnfc_svEpurseGetEpochNow()
--	xipdbg("In Lua: currentTxnTime  ----"..currentTxnTime )
	if( PrevTxnTime == -1 ) then 
		timeInvl = 0
	else
		timeInvl =  currentTxnTime  - tonumber( PrevTxnTime )
	end
--	xipdbg("In Lua: timeInvl  ----"..timeInvl )
	
	SyncInterval = GetConfigValue("sti") 
--	xipdbg("In Lua: SyncInterval: " .. SyncInterval )
	SyncMaxTxnNo = GetConfigValue("stc") 
--	xipdbg("In Lua: SyncMaxTxnNo: " .. SyncMaxTxnNo )
	txnCount=xal_getRecordCount()
--	xipdbg("In Lua: txnCount is ----"..txnCount)
	
	if( ( timeInvl >= tonumber( SyncInterval ) and txnCount >= 1 )  or  txnCount >= tonumber( SyncMaxTxnNo) ) then
		-- update the transaction time
		SetConfigValue("SVST", tostring(currentTxnTime) )
		RCV_SyncNow()
	else
		RCV_goHome ()
	end
end

function RCV_SyncNow()
	ChangeXla("SyncSV")
end

function RCV_retry()
	if( RCV_nfcSess ~= 0 ) then 
		sysnfc_nfc_cancel(RCV_nfcSess)
	end

	DisplayScreenFromRes("NFCProgress", GetCurrencySymbol(),RCV_Amount)
	RCV_nfcSess = sysnfc_init("RCV_offlineReadDetectCard")
end
