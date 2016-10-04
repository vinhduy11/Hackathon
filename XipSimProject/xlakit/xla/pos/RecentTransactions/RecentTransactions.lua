--[[ XLA: ResentTransactions
-- Description: Implements the RT feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

RT_xmsConn = 0
RT_gMPIN = 0
txnId = {}
txnDate = {}
ConsumerId = {}
txnType = {} 
txnAmount = {}
lftTxn = {}
gTxnDateStr = {}
txnTypeDetails = {}
txnStatus = {}
RT_Type = 0
array = {}

function RT_OnLoad ()
	DisplayScreenFromRes("RecentTxnsMPinEntryScreen", "#GETMPIN", " ")
end

function RT_OnMPINNext (mPIN)
	if( mPIN ~= nil  and mPIN:len() == 4 )then		
		RT_MPIN=mPIN
		XmsRequest_RT()
	else
		DisplayScreenFromRes("RecentTxnsMPinEntryScreen", "#GETMPIN", "#INCRCT" )
	end
end

function XmsRequest_RT ()
	DispScreen("RecentTxnsProgress")

	cntType = xal_xms_getcontentType()
	xipdbg("In Lua: XmsRequest_RT content Type:".. cntType )
	
	if( cntType == -1 ) then txnCType = "LFT".."|".. "7/f"
	else txnCType = "LFT".."|"..cntType end
	RT_xmsConn=xal_xms_init("NULL", txnCType, 0, "RT_CB")
	
	xid = GetDeviceXID()
	xal_xms_add_params( RT_xmsConn, "xid", xid )
	mccMnc = GetMncMcc()
	xal_xms_add_params( RT_xmsConn, "mcc", string.sub(mccMnc, 1, 3) )
	xal_xms_add_params( RT_xmsConn, "mnc", string.sub(mccMnc, 4, -1) )
	exid = GetDeviceExid()
	xal_xms_add_params( RT_xmsConn, "exid", exid  )
	if( RT_Type == "1" or RT_Type == "2" ) then
		xal_xms_add_params( RT_xmsConn, "mp", RT_MPIN )
	end
	ret = xal_xms_request(RT_xmsConn, 1)
end

function RT_CB ()
	XMSSCData = xal_xms_get_params (RT_xmsConn, "sc")
	SCDetails = mysplit (XMSSCData,"|")
	xmsSC = SCDetails[1]
	xipdbg("In Lua: perso Status" .. xmsSC)
	
	if tonumber (xmsSC)  ==  0 then
--		xip_led_viberator_enable( )
		txnCount = xal_xms_GetTxnCount(xmsConn)	
		for i=0,txnCount-1 do
			txnId[i], txnDate[i], ConsumerId[i], txnType[i], txnAmount[i] = xal_xms_GetTxnDetails(xmsConn, i)
			xipdbg("TestXMS: " .. txnId[i].. "  " .. txnDate[i] .. "  " ..ConsumerId[i] .. "  " .. txnType[i] .. "  " .. txnAmount[i])
			lTxnDateStr = NIL
			lTxnDateStr = xipGetDateStr (txnDate[i])
			gTxnDateStr[i] = xipGetDateTimeStr (txnDate[i])
			lftTxn[i] = lTxnDateStr .. "   "  ..GetCurrencySymbol().." "..txnAmount[i]
			xipdbg("In Lua: Displaying last5TxnsMenuScreen: lftTxn[" .. i .. "] = " .. lftTxn[i])
		end 

		xipdbg("In Lua: Displaying last5TxnsMenuScreen: LFT")
		DisplayScreenFromRes("RecentTxnsMenuScreen", lftTxn[0], lftTxn[1], lftTxn[2], lftTxn[3], lftTxn[4])
		
	elseif tonumber (xmsSC)  ==  8888 then
		DisplayScreenFromRes("RecentTxnsTimeout")
	else		
		if string.len(SCDetails[2]) > 20 then
			GetMultipleLines(SCDetails[2])
			DisplayScreenFromRes("RecentTxnsFailureScreen", xmsSC, array[1], array[2], array[3], array[4], array[5])
		else
			DisplayScreenFromRes("RecentTxnsFailureScreen", xmsSC, SCDetails[2] )
		end
	end
	xal_xms_deInit(RT_xmsConn)
	RT_xmsConn = 0
end

function RT_OnMenuBtn1 ()
	DisplayLFTDetails (0)
end

function RT_OnMenuBtn2 ()
	DisplayLFTDetails (1)
end

function RT_OnMenuBtn3 ()
	DisplayLFTDetails (2)
end

function RT_OnMenuBtn4 ()
	DisplayLFTDetails (3)
end

function RT_OnMenuBtn5 ()
	DisplayLFTDetails (4)
end

function OnLFTMenuOk ()
	xipdbg("OnLFTMenuOk")
end 

function RT_OnBack ()
	if( RT_Type == "1" ) then
		DisplayScreenFromRes("RecentTxnsMenuScreen", lftTxn[0], lftTxn[1], lftTxn[2], lftTxn[3], lftTxn[4])	
	elseif( RT_Type == "3" ) then
		RT_goHome ()
	end
end

function RT_goHome ()
	ChangeXla("HomeScreen")
end

function RT_OnCancel()
	if(RT_xmsConn ~= 0) then 
		xal_xms_deInit(RT_xmsConn)
	end

	RT_goHome()
end

function DispScreen (scrName)
	DisplayScreenFromRes(scrName)
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

function isempty(s)
  return s == nil or s == ''
end

function xipGetDateStr (origDate)
	dtDetails = mysplit (origDate,"|")
	localTxnDateStr = dtDetails[1]
	
	if( dtDetails[1] == "NA" ) then 
		localTxnDateStr = dtDetails[2]
	end 
	
	return localTxnDateStr
end

function xipGetDateTimeStr (origDate)
	dtDetails = mysplit (origDate,"|")

	if (dtDetails[3] ~= nil ) or (dtDetails[3]  ~= '') then 
		timDetails = mysplit(dtDetails[3], ":")
	end
	
	localTxnDateStr = dtDetails[2] .. " "  .. timDetails[1] .. ":" .. timDetails[2]

	return localTxnDateStr
end

function DisplayLFTDetails (i)
	xipdbg("In Lua: Displaying Transaction detail for  " .. i )

	if( string.sub(txnType[i], 1, 1) == "S" ) then
		--txnStatus = "Success"
		--txnStatus =GetXlaString("ResentTransactions:SUCC")
		txnStatus ="#SUCC"
	elseif( string.sub(txnType[i], 1, 1) == "F" ) then
		--txnStatus = "Fail"
		--txnStatus =GetXlaString("ResentTransactions:FAIL")
		txnStatus ="#FAIL"
	else
		txnStatus = "NA"
	end
	
	if( string.sub(txnType[i], 3, -1) == "C" ) then
		--typeCrDr = "Credit"
		--typeCrDr =GetXlaString("ResentTransactions:CRDT")
		typeCrDr = "#CRDT"
	elseif( string.sub(txnType[i], 3, -1) == "D" ) then
		--typeCrDr = "Debit"
		--typeCrDr =GetXlaString("ResentTransactions:DBT")
		typeCrDr = "#DBT"
	else
		typeCrDr = "NA"
	end

	DisplayScreenFromRes("RecentTxnsScreen", lftTxn[i], gTxnDateStr[i], ConsumerId[i], txnId[i], typeCrDr, txnStatus, "#BACK", " ", "RT_OnBack", " ")
 end

function GetMultipleLines (buf)
	xipdbg("DisplayMultipleLines:Line Received : "..buf)
	count = 1	
	result = ""
    for word in buf:gmatch("%w+") do
        if(string.len(result)+string.len(word)+1 > 20) then
            xipdbg("DisplayMultipleLines:Line chunk : "..result)
			array[count]=result
			count = count + 1
            result = word
        elseif(string.len(result)>0) then
            result = result.." "..word
        else
            result = word
        end
    end
    xipdbg("DisplayMultipleLines:Line chunk : "..result)
	array[count]=result
	count = count + 1
	while(count <= 5) do
		array[count]=" "
		count = count + 1
	end
end
