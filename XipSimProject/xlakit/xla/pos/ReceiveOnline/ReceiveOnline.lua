--[[ XLA:ReceiveOnline
-- Description: Implements the Receive feature for the POS XLA bundle. 
-- Asks for MPIN if amount > pinlessStartAmt or amount < pinlessEndAmt
-- Date: 22 Feb 2016
-- Author: Nearex
-- History: Initial version created
--]]

RCV_XmsConn = 0
RCV_Amount = 0
RCV_MPIN 	= 0
RCV_MSISDN = 0
PL_Start= 0
PL_End = 0
array = {}

RCV_Xid = 0
RCV_nfcSess = 0

function RCV_OnLoad ()
	ret = GetSessionValue ("rcvamount")
	if (ret == "-10") then
		DisplayScreenFromRes("receiveAmountEntryScreen")
	else
		RCV_OnAmountNext (ret)
	end
	maxAmt = GetDeviceMaxAmount()
	DisplaySetMaxInputDataLen(maxAmt)
end

function RCV_OnAmountNext (amount)
	DisplaySetMaxInputDataLen("0")
	if( amount ~= nil and tonumber( amount ) > 0 ) then
		RCV_Amount = amount	
		DisplayScreenFromRes("NFCProgress", GetCurrencySymbol(),RCV_Amount)
--		xipdbg("In Lua: RCV_OnAmountNext " .. RCV_Amount)
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
	
	if (status == "true") then
		sysnfc_readTagData(RCV_nfcSess, 1, 2,"RCV_OnNFCReadData")
	else
		DisplayScreenFromRes("receiveFailureScreen", " ", " ", "#TAPTO")
	end
end

function RCV_OnNFCReadData (status, tagdata, tagdatalen)
--	xipdbg("In Lua: OnNFCReadData " .. status )
--	xipdbg("Got data [" .. tagdata .. "] with len of " .. tagdatalen)
	local i = 1
	for seg in string.gmatch(tagdata, "([^|]*)") do
		xipdbg("Parsed as [" .. seg .. "]" )
		if(i == 5) then RCV_MSISDN = seg
		elseif( i == 7 ) then RCV_IsuId = seg
		elseif( i == 9 ) then RCV_Exid = seg
		elseif( i == 11 ) then RCV_Xid	= seg
		else
		end
		i = i + 1	
	end 

	if( RCV_Xid ~= 0 and RCV_Exid ~= 0 and RCV_IsuId ~= 0 and RCV_MSISDN ~= 0)then 
		PL_Start = GetPinlessStartAmount()
		PL_End = GetPinlessEndAmount()
		if ( tonumber(RCV_Amount) > tonumber( PL_Start )  and tonumber( RCV_Amount ) <= tonumber( PL_End ) ) then
			DisplayScreenFromRes("receiveMPinEntryScreen", " ", GetCurrencySymbol().." "..RCV_Amount, RCV_MSISDN)
		else
			xipdbg("In Lua: XmsRequest_RCV " )
			XmsRequest_RCV()
		end
	else
		DisplayScreenFromRes("receiveFailureScreen", " ", " ", "#INVALID1", "#INVALID2")
	end
end

function RCV_OnMPINOk( mPIN )
	if( mPIN ~= nil and mPIN:len() == 4 )then  
		RCV_MPIN = mPIN
		XmsRequest_RCV()
	else
		DisplayScreenFromRes("receiveMPinEntryScreen", "#INCRCT", GetCurrencySymbol().." "..RCV_Amount, RCV_MSISDN)
	end	
end

function XmsRequest_RCV ()
--	xipdbg("In Lua: XmsRequest_RCV")
	DisplayScreenFromRes("receiveProgressScreen",  GetCurrencySymbol().." "..RCV_Amount, RCV_MSISDN)
	cntType = xal_xms_getcontentType()
--	xipdbg("In Lua: XmsRequest_RCV content Type:".. cntType )
	if( cntType == -1 ) then 
		txnType = "PM".."|".. "7/f"
	else
		txnType = "PM".."|"..cntType
	end
--	xipdbg("In Lua: XmsRequest_RCV txnType:".. txnType )
	
	RCV_XmsConn=xal_xms_init("NULL", txnType, 0, "RCV_CB")

	if ( tonumber(RCV_Amount) > tonumber( PL_Start )  and tonumber( RCV_Amount ) <= tonumber( PL_End ) ) then
		xal_xms_add_params( RCV_XmsConn, "mp", RCV_MPIN )
	end
	mccMnc = GetMncMcc()
	xal_xms_add_params( RCV_XmsConn, "mcc", string.sub(mccMnc, 1, 3) );
	xal_xms_add_params( RCV_XmsConn, "mnc", string.sub(mccMnc, 4, -1) );
--	xipdbg("Adding Param Amount = " .. RCV_Amount)		
	xal_xms_add_params( RCV_XmsConn, "amt", RCV_Amount )
--	xipdbg("Adding Param IsuId = " .. RCV_IsuId)		
	xal_xms_add_params( RCV_XmsConn, "isuid", RCV_IsuId )
--	xipdbg("Adding Param xid = " .. RCV_Xid)		
	xal_xms_add_params( RCV_XmsConn, "xid", RCV_Xid )
	xal_xms_add_params( RCV_XmsConn, "ltrx", RCV_Xid );
--	xipdbg("Adding Param exid = " .. RCV_Exid)		
	xal_xms_add_params( RCV_XmsConn, "exid", RCV_Exid )
	ret = xal_xms_request(RCV_XmsConn, 1)
end

function RCV_CB ()
	XMSSCData = xal_xms_get_params (RCV_XmsConn, "sc")
	SCDetails = mysplit (XMSSCData,"|")
	xmsSC = SCDetails[1]
--	xipdbg("In Lua: perso Status" .. xmsSC)
	
	txnId = xal_xms_get_params (RCV_XmsConn, "txnId")
	xal_xms_deInit(RCV_XmsConn)
	RCV_XmsConn = 0
--	xipdbg("In Lua: Displaying receiveSuccessScreen: SC = " .. xmsSC .. "txnID  " .. txnId)
	if tonumber (xmsSC)  ==  0 or tonumber (xmsSC)  ==  0100 then
		DisplayScreenFromRes("receiveSuccessScreen", GetCurrencySymbol().." "..RCV_Amount, RCV_MSISDN, txnId )
	elseif tonumber (xmsSC)  ==  8888 then
		DisplayScreenFromRes("receiveFailureScreen", "#Global:TXNTO", " ", " ", " ", "#Global:TXNFAIL", " ", " ")
	else		
		if string.len(SCDetails[2]) > 20 then
			GetMultipleLines(SCDetails[2])
			DisplayScreenFromRes("receiveFailureScreen", xmsSC, array[1], array[2], array[3], array[4], array[5])
		else
			DisplayScreenFromRes("receiveFailureScreen", xmsSC, SCDetails[2] )
		end
	end
end


function RCV_goHome ()
	ChangeXla("HomeScreen")
end

function RCV_OnCancel()
	DisplaySetMaxInputDataLen("0")
	
	if(RCV_XmsConn ~= 0) then 
		xal_xms_deInit(RCV_XmsConn)
	end
	
	RCV_goHome()
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
