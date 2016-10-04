--[[ XLA: SVResentTransactions
-- Description: Implements the  SV RT feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

txnName = nil
txnAmount = nil
txnId = nil
txnDate = nil
txnCount = 0
recCount = 0
recCount1 = 0
imgArrowFlag = 0
crdb = nil

function SVRT_OnLoad ()
	xipdbg("Calling SVRT_OnLoad 111")
	txnCount=xal_getRecordCount()
	z = xipdbg("Test: txnCount is ----"..txnCount)

	if( txnCount > 0 ) then
		SVRT_RecentTxnsMenu()
	else
		DisplayScreenFromRes("SVRecentTxnsEmpty")
	end	
end

function SVRT_RecentTxnsMenu ()
	z = xipdbg("Test: txnCount is ----"..txnCount)
	
	z = xipdbg("Test: SVRT_RecentTxnsMenu =" .. recCount )
	recCount1 = ( txnCount - 1 ) - recCount
	z = xipdbg("Test: SVRT_RecentTxnsMenu =" .. recCount1 )
	if( ( recCount1 < txnCount ) and ( txnCount > 0 )  ) then
		z = xipdbg("1Test: SVRT_RecentTxnsMenu =" .. recCount1 )
		rec_id, GetRecordAT,GetRecordAT_len, GetDate=xal_getRecordAt(recCount1)

		z = xipdbg("Test: GetRecordAT record id----".. rec_id)
		z = xipdbg("Test: GetRecordAT DATA----".. GetRecordAT)
		z = xipdbg("Test: GetRecordAT LEN:----".. GetRecordAT_len)
		z = xipdbg("Test: GetRecordAT GetDate:----".. GetDate)
		txnDetails = mysplit(GetRecordAT, "~")
		xipdbg("txnDetails: " .. txnDetails[3])
		if (txnDetails[2] ~= nil) and (txnDetails[3] ~= nil) and (txnDetails[7] ~= nil) and (txnDetails[8] ~= nil) then 
			xipdbg("txnDetails: " .. txnDetails[2].. "  " .. txnDetails[3] .. "  " ..txnDetails[8] .. "  " .. txnDetails[7] )

			txnDate = string.sub(txnDetails[2], 7, 8) .. "-" .. string.sub(txnDetails[2], 5, 6) .. "-" .. string.sub(txnDetails[2], 1, 4) .. " " .. string.sub(txnDetails[2], 9, 10) .. ":" .. string.sub(txnDetails[2], 11, 12)
			txnName = txnDetails[3]
			txnId = txnDetails[8]
			amtStr = GetXlaString("SVAMT")
			txnAmount = amtStr .. GetSessionValue("CURR") .. " " .. txnDetails[7]
			
			if( txnName == "PM" ) then
				--crdb = "Credit"
				--txnStatus = "Success"
				--crdb = GetXlaString("SVResentTransactions:CRDT")
				crdb = "#CRDT"
				--txnStatus = GetXlaString("SVResentTransactions:SUCC")
				txnStatus = "#SUCC"
			elseif( txnName == "TU" ) then
				--crdb = "Debit"
				--txnStatus = "Failure"
				--crdb = GetXlaString("SVResentTransactions:DBT")
				crdb = "#DBT"
				--txnStatus = GetXlaString("SVResentTransactions:FAIL")
				txnStatus = "#FAIL"
			end

			xipdbg("txnAmount: " .. txnAmount.. "  txnDate " .. txnDate .. "txnId  " ..txnId .. "crdb  " .. crdb )

			xipdbg("txnCount: " .. txnCount.. "  recCount " .. recCount )
			if( txnCount == 1 ) then
				DisplayScreenFromRes("SVRecentTxnsScreenNoArrow", txnAmount, txnDate, txnId, crdb, txnStatus)				
			elseif( txnCount > 1 and recCount == 0 ) then
				DisplayScreenFromRes("SVRecentTxnsScreenRightArrow", txnAmount, txnDate, txnId, crdb, txnStatus)				
			elseif( txnCount > 1 and recCount == ( txnCount -1 ) ) then
				DisplayScreenFromRes("SVRecentTxnsScreenLeftArrow", txnAmount, txnDate, txnId, crdb, txnStatus)				
			else
				DisplayScreenFromRes("SVRecentTxnsScreen", txnAmount, txnDate, txnId, crdb, txnStatus)
			end	
		end	
	end	
end

function SVRT_goHome ()
	ChangeXla("HomeScreen")
end

function DispScreen (scrName)
	DisplayScreenFromRes(scrName)
end

function SVRT_OnCancel()
	SVRT_goHome()
end

function mysplit(inputstr, sep)
	if sep == nil then
			sep = "%s"
	end
	local t={} ; i=1
	xipdbg("LFT: Split: Input String val = " .. inputstr)		
	for str in string.gmatch(inputstr, "([^"..sep.."]+)") do
		t[i] = str
		xipdbg("LFT: Individual Split String val = " .. t[i])		
		i = i + 1
	end
	return t
end

function SVRT_Onleft ()
	xipdbg("SVRT_Onright: recCount " .. recCount .. "txnCnt" .. txnCount )
	if( recCount > 0 ) then
		recCount = recCount - 1
	end
	
	SVRT_RecentTxnsMenu ()
end

function SVRT_Onright ()
	xipdbg("SVRT_Onright: recCount " .. recCount .. "txnCnt" .. txnCount )
	if( recCount < ( txnCount - 1 ) ) then
		recCount = recCount + 1
	end	
	
	SVRT_RecentTxnsMenu ()
end

