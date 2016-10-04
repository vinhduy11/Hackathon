--[[ XLA:SyncSV
-- Description: Implements the Stored Value Sync feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

SYNCSV_XmsConn = 0
SYNCSV_WakeupType = 0
RecIds ={}
RetryCount = 0

SYNCSV_CHUNKEnb = false
SYNCSV_RecCHUNK = 5
SYNCSV_TotlRecCnt = 0
SYNCSV_TotlRecSync = 0
SYNCSV_RecSend = 0
totalAmt = 0
SYNCTotalAmt = 0
array = {}

function SYNCSV_OnLoad ()
	-- check for wake sync Type
	SYNCSV_WakeupType = GetSessionValue("SyncUp")
	xipdbg("SYNCSV_OnLoad: SYNCSV_WakeupType On ----"..SYNCSV_WakeupType)
	
	SYNCSV_TotlRecCnt=xal_getRecordCount()
	xipdbg("SYNCSV_OnLoad: Total RecCount is ----"..SYNCSV_TotlRecCnt)	
	SendSyncRespCB()
end

function SendSyncRespCB()
	-- check for no records
	if( SYNCSV_TotlRecCnt == 0 ) then
		if( SYNCSV_WakeupType == "1" ) then
			ChangeXla ("Home")
		else
			DisplayScreenFromRes("syncFailureScreen",  "", "#EMTTXN" )
		end
	elseif( SYNCSV_TotlRecCnt >  SYNCSV_TotlRecSync ) then
		-- check for chunk enable
		if(  SYNCSV_CHUNKEnb == true ) then
			SYNCSV_RecSend  = SYNCSV_TotlRecCnt
		elseif( (SYNCSV_TotlRecCnt - SYNCSV_TotlRecSync) < SYNCSV_RecCHUNK ) then
			SYNCSV_RecSend = SYNCSV_TotlRecCnt - SYNCSV_TotlRecSync
		else
			SYNCSV_RecSend = SYNCSV_RecCHUNK
		end
		xipdbg("SYNCSV_OnLoad: Total RecCount is ----"..SYNCSV_TotlRecCnt)	
		xipdbg("SendSyncRespCB: Total sync Recs ----"..SYNCSV_TotlRecSync)
		xipdbg("SendSyncRespCB: send Recs ----"..SYNCSV_RecSend)
		SYNCSV_TotlRecSync = SYNCSV_TotlRecSync + SYNCSV_RecSend 
		XmsRequest_SYNCSV ()
	else
		if( SYNCSV_WakeupType == "1" ) then
			ChangeXla ("Home")
		else
			DisplayScreenFromRes("syncSuccessScreen", SYNCSV_TotlRecSync, GetCurrencySymbol().." "..SYNCTotalAmt )
		end
	end
end

function XmsRequest_SYNCSV ()
	xipdbg("In Lua: XmsRequest_SYNCSV" .. SYNCSV_RecSend)
	recData = nil
	RecLen = 0

	for i=0, SYNCSV_RecSend-1 do
		xipdbg("Test: RT_RecentTxnsMenu =" .. i )
		rec_id, GetRecordAT,GetRecordAT_len, GetDate=xal_getRecordAt(i)

		xipdbg("Test: GetRecordAT record id----".. rec_id)
		xipdbg("Test: GetRecordAT DATA----".. GetRecordAT)
		xipdbg("Test: GetRecordAT LEN:----".. GetRecordAT_len)
		xipdbg("Test: GetRecordAT GetDate:----".. GetDate)
		if( recData == nil ) then
			recData = GetRecordAT .. ","
			RecLen = GetRecordAT_len + 1
		else
			if( i < SYNCSV_RecSend-1 ) then 
				recData = recData .. GetRecordAT .. ","
				RecLen = RecLen + GetRecordAT_len + 1
			else	
				recData = recData .. GetRecordAT 
				RecLen = RecLen + GetRecordAT_len
			end
		end
		xipdbg("Test: GetRecordAT recData = ".. recData)
		xipdbg("Test: GetRecordAT RecLen = ".. RecLen)
		RecIds[i+1]=rec_id 
		txnDetails = mysplit(GetRecordAT, "~")
		if (txnDetails[3] ~= nil) and (txnDetails[7] ~= nil) then 
			xipdbg("txnDetails2: " .. txnDetails[3].. "txnDetails6: " .. txnDetails[7] )
			if( txnDetails[3] == "PM" ) then
				if( totalAmt == 0 ) then
					xipdbg("1st time recAmt Parsed as [" .. txnDetails[7] .. "]" )
					Sync_TotalBal = sysnfc_svEpurseConvAmtDec2Int(txnDetails[7])
				else
					xipdbg("2nd onwards recAmt Parsed as [" .. txnDetails[7] .. "]" )
					Sync_TotalBal = sysnfc_svEpurseConvAmtDec2Int(totalAmt) + sysnfc_svEpurseConvAmtDec2Int(txnDetails[7])
				end	
				totalAmt = sysnfc_svEpurseConvAmtInt2Dec(Sync_TotalBal)
			end	
		end
	end	

	xipdbg("Final total Amount to be synched [" .. totalAmt .. "]" .. "SYNCSV_RecSend " .. SYNCSV_RecSend )
	xipdbg("XmsRequest_SYNCSV: RecLen ----"..RecLen)
	xipdbg("XmsRequest_SYNCSV: recData ----"..recData)
	
	for i=1, SYNCSV_RecSend do
		xipdbg("XmsRequest_SYNCSV: RecIds  ----"..RecIds[i])
	end

	DisplayScreenFromRes("syncProgressScreen",  SYNCSV_RecSend, GetCurrencySymbol().." "..totalAmt )
	cntType = xal_xms_getcontentType()
	xipdbg("In Lua: XmsRequest_SYNCSV content Type:".. cntType )
	if( cntType == -1 ) then txnType = "SYNCUP".."|".. "23/2f"
	else txnType = "SYNCUP".."|"..cntType end
	xipdbg("In Lua: XmsRequest_SYNCSV txnType:".. txnType )
	res=xal_xms_init("NULL", txnType, 0, "SYNCSV_CB")
	xipdbg("res = " .. res)
	SYNCSV_XmsConn = res	
	exid = GetDeviceExid()
	xal_xms_add_params( SYNCSV_XmsConn, "exid", exid  )
	
	xal_xms_add_params( SYNCSV_XmsConn, "data", recData )
	xal_xms_add_params( SYNCSV_XmsConn, "zd", "0" )
	ret = xal_xms_request(SYNCSV_XmsConn, 1)
end

function SYNCSV_CB ()
	xipdbg("In Lua: SYNCSV_CB, Res = " .. res)
	XMSSCData = xal_xms_get_params (SYNCSV_XmsConn, "sc")
	SCDetails = mysplit (XMSSCData,"|")
	xmsSC = SCDetails[1]
	xipdbg("In Lua: SYNCSV_CB, xmsSC = " .. xmsSC)

	xal_xms_deInit(SYNCSV_XmsConn)
	SYNCSV_XmsConn = 0
	if tonumber (xmsSC)  ==  0 or tonumber (xmsSC)  ==  0100  then
		-- delete the Records with array .
		ret = xal_deleteMultipleRecords( RecIds, SYNCSV_RecSend)

		-- update pending txns count and amount
		if( ret == 0 ) then 
			xipdbg("After deleting the records [" .. totalAmt .. "]" .. "SYNCSV_RecSend " .. SYNCSV_RecSend )
			if( tonumber(totalAmt) > 0 ) then
				-- SV Total amount
				SVCurAmt = GetConfigValue("sta")
				xipdbg("In Lua: StoreValueRecord: SVCurAmt = " .. SVCurAmt )
				if( SVCurAmt ~= -1 ) then
					SVTotalAmt = sysnfc_svEpurseConvAmtDec2Int(SVCurAmt) - sysnfc_svEpurseConvAmtDec2Int(totalAmt)
					remAmt = sysnfc_svEpurseConvAmtInt2Dec(SVTotalAmt)
					xipdbg("In Lua: StoreValueRecord: remAmt = " .. remAmt )
					SetConfigValue("sta", remAmt )
				end
				SYNCTotalAmt = SYNCTotalAmt + totalAmt
			end
		
			-- SV Total count
			SVCurTxns = GetConfigValue("stt")
			xipdbg("In Lua: StoreValueRecord: SVCurTxns = " .. SVCurTxns )
			if( SVCurTxns ~= -1 ) then
				SVTotalTxns = tonumber( SVCurTxns ) - SYNCSV_RecSend
				xipdbg("In Lua: StoreValueRecord: TotalTxns = " .. SVTotalTxns )
				SetConfigValue("stt", tostring(SVTotalTxns) )
			end	
		end
		
		-- Store last sync Time Stamp (YYYYMMDDHHMMSS)
		TS = xal_xms_timeStamp()
		SetConfigValue("lss", TS )
		
		-- reset the retry count 
		RetryCount = 0
		
		totalAmt = 0
		-- try for next lot of records
		SendSyncRespCB()
	elseif tonumber (xmsSC)  ==  8888 then
		-- retry the record send 3 times 
		RetryCount = RetryCount + 1
		if( RetryCount > 3) then 
			if( SYNCSV_WakeupType == "1" ) then
				ChangeXla ("Home")
			else
				DisplayScreenFromRes("syncTimeout") 
			end
		else
			totalAmt = 0
			XmsRequest_SYNCSV ()
		end
	else		
		-- retry the record send 3 times 
		--RetryCount = RetryCount + 1
		--if( RetryCount > 3) then 
			if( SYNCSV_WakeupType == "1" ) then
				ChangeXla ("Home")
			else
				if string.len(SCDetails[2]) > 20 then
					GetMultipleLines(SCDetails[2])
					DisplayScreenFromRes("syncFailureScreen", xmsSC, array[1], array[2], array[3], array[4], array[5])
				else
					DisplayScreenFromRes("syncFailureScreen", xmsSC, SCDetails[2] )
				end
			end
		--else
		--	XmsRequest_SYNCSV ()
		--end
	end
end

function SYNCSV_goHome ()

	if( SYNCSV_WakeupType == "1" ) then
		ChangeXla ("Home")
	else
		ChangeXla("HomeScreen")
	end
end

function DispScreen (scrName)
	DisplayScreenFromRes(scrName)
end

function SYNCSV_OnCancel()
	if(SYNCSV_XmsConn ~= 0) then 
		xipdbg("In Lua: SYNCSV_OnCancel, xal_xms_deInit " )
		xal_xms_deInit(SYNCSV_XmsConn)
	end
	
	SYNCSV_goHome()
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


