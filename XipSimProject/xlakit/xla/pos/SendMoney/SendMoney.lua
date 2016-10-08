--[[ XLA: SendMoney
-- Description: Implements the SM feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

SM_xmsConn = 0
SM_Amount = nil
SM_MerNo  = nil
SM_MPIN = 0
array = {}

function SM_OnLoad ()
	xipdbg("Calling DisplayScreenFromRes")
	DispScreen("sendMoneyMsisdnNumberEntryScreen")
end

function SM_OnMerNoNext (merNo)
	SM_MerNo	= merNo
	if( SM_MerNo == nil ) then 
		DisplayScreenFromRes("sendMoneyFailureScreen", " ", " ", "#IMPRDATA" )
	else
		xipdbg("In Lua: mer no = " .. SM_MerNo )
		DisplayScreenFromRes("sendMoneyAmountEntryScreen", SM_MerNo )
	end 
	
	--maxAmt = GetPinlessEndAmount()
	--DisplaySetMaxInputDataLen("99999")
end

function SM_OnAmountNext (amount)
	DisplaySetMaxInputDataLen("0")
	-- xipdbg("In Lua: Amouont = " .. amount)
	SM_Amount	= amount
	if( SM_Amount ~= nil and tonumber( SM_Amount ) > 0 ) then 
		xipdbg("In Lua: Displaying sendMoneyMPinEntryScreen: amount = " .. SM_Amount .. "mer no = " .. SM_MerNo)
		DisplayScreenFromRes("sendMoneyMPinEntryScreen", " ", GetCurrencySymbol().." "..SM_Amount, SM_MerNo )
	else
		SM_goHome ()
	end
end

function SM_OnMPINNext (mPIN)
--	xipdbg("Calling XMS Request For SM, Pin = " .. mPIN )
	if( mPIN ~= nil and mPIN:len() == 4 )then
		SM_MPIN=mPIN
		XmsRequest_SM()
	else
		DisplayScreenFromRes("sendMoneyMPinEntryScreen", "#INCRCT", GetCurrencySymbol().." "..SM_Amount, SM_MerNo )
	end	
end

function XmsRequest_SM ()
	xipdbg("In Lua: XmsRequest_SM")
	DisplayScreenFromRes("sendMoneyProgressScreen", GetCurrencySymbol().." "..SM_Amount, SM_MerNo )
	cntType = xal_xms_getcontentType()
	if( cntType == -1 ) then txnType = "SM".."|".. "7/f"
	else txnType = "SM".."|"..cntType end
	SM_xmsConn=xal_xms_init("NULL", txnType, 0, "SM_CB")
	xid = GetDeviceXID()
	xal_xms_add_params( SM_xmsConn, "xid", xid )
	mccMnc = GetMncMcc()
	xal_xms_add_params( SM_xmsConn, "mcc", string.sub(mccMnc, 1, 3) )
	xal_xms_add_params( SM_xmsConn, "mnc", string.sub(mccMnc, 4, -1) )
	exid = GetDeviceExid()
	xal_xms_add_params( SM_xmsConn, "exid", exid  )
	xipdbg("Adding Param amount = " .. SM_Amount)		
	xal_xms_add_params( SM_xmsConn, "amt", SM_Amount )
	xipdbg("Adding Param merchant number = " .. SM_MerNo)		
	xal_xms_add_params( SM_xmsConn, "rms", SM_MerNo )
	xipdbg("Adding Param Mode of com= " .. "2")		
	xal_xms_add_params( SM_xmsConn, "com", "2" )
	-- xipdbg("Adding Param MPIN = " .. SM_MPIN)	
	xal_xms_add_params( SM_xmsConn, "mp", SM_MPIN )
	ret = xal_xms_request(SM_xmsConn, 1)
end

function SM_CB ()
	XMSSCData = xal_xms_get_params (SM_xmsConn, "sc")
	SCDetails = mysplit (XMSSCData,"|")
	xmsSC = SCDetails[1]
	xipdbg("In Lua: perso Status" .. xmsSC)
	
	txnId = xal_xms_get_params (SM_xmsConn, "txnId")
	
	
	xal_xms_deInit(SM_xmsConn)
	SM_xmsConn = 0
	if tonumber (xmsSC)  ==  0 or tonumber (xmsSC)  ==  0100 then
		xipdbg("In Lua: Displaying sendMoneySuccessScreen: SC = " .. xmsSC .. "txnID  " .. txnId)
		DisplayScreenFromRes("sendMoneySuccessScreen", GetCurrencySymbol().." "..SM_Amount, SM_MerNo, txnId)
	elseif tonumber (xmsSC)  ==  8888 then
		DisplayScreenFromRes("sendMoneyTimeout")
	else
		if string.len(SCDetails[2]) > 20 then
			GetMultipleLines(SCDetails[2])
			DisplayScreenFromRes("sendMoneyFailureScreen", xmsSC, array[1], array[2], array[3], array[4], array[5] )
		else
			DisplayScreenFromRes("sendMoneyFailureScreen", xmsSC, SCDetails[2] )
		end
	end
end

function SM_goHome ()
	ChangeXla("HomeScreen")
end

function DispScreen (scrName)
	xipdbg("DispScreen: Calling DisplayScreenFromRes for screen   " .. scrName )
	DisplayScreenFromRes(scrName)
end

function SM_OnCancel()
	DisplaySetMaxInputDataLen("0")
	if(SM_xmsConn ~= 0) then 
		xal_xms_deInit(SM_xmsConn)
	end
	
	SM_goHome()
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
