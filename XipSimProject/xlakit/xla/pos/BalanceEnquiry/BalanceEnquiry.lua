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
array = {}
BE_IsuId	= 0
BE_MSISDN = 0
BE_Name	= 0

function BE_OnLoad ()
	DisplayScreenFromRes("balanceMPinEntryScreen", "#BEMPIN", " ")
end

function BE_OnMPINOk(mPIN)
--	xipdbg("Calling XMS Request For BE, Pin = " .. mPIN )
	if( mPIN ~= nil and mPIN:len() == 6 ) then
		BE_MPIN = mPIN
		XmsRequest_BE()
	else
		DisplayScreenFromRes("balanceMPinEntryScreen", "#BEMPIN", "#BEINCRCT")
	end
end

function XmsRequest_BE ()
	xipdbg("In Lua: XmsRequest_BE")
	DisplayScreenFromRes("balanceProgress")
	
	cntType = xal_xms_getcontentType()
	xipdbg("In Lua: XmsRequest_BE content Type:".. cntType )
	
	if( cntType == -1 ) then txnType = "TEAM_TEST".."|".. "7/f"
	else txnType = "TEAM_TEST".."|"..cntType end
	BE_xmsConn=xal_xms_init("NULL", txnType, 0, "BE_CB")
	mccMnc = GetMncMcc()
	xal_xms_add_params( BE_xmsConn, "mcc", string.sub(mccMnc, 1, 3) )
	xal_xms_add_params( BE_xmsConn, "mnc", string.sub(mccMnc, 4, -1) )

	xid = GetDeviceXID()
	xal_xms_add_params( BE_xmsConn, "xid", xid )
	exid = GetDeviceExid()
	xal_xms_add_params( BE_xmsConn, "exid", exid  )
	xal_xms_add_params( BE_xmsConn, "mp", BE_MPIN )
	
	  xal_xms_add_params( BE_xmsConn, "msgType", "MOMO_BALANCE" )
	  msisdn = GetDeviceMSISDN()
	   xal_xms_add_params( BE_xmsConn, "user",msisdn )
   xal_xms_add_params( BE_xmsConn, "pass", BE_MPIN )
   
	ret = xal_xms_request(BE_xmsConn, 1)
end

function BE_CB ()
	XMSSCData = xal_xms_get_params (BE_xmsConn, "sc")
	SCDetails = mysplit (XMSSCData,"|")
	xmsSC = SCDetails[1]
	xipdbg("In Lua: perso Status" .. xmsSC)

	uBalVal = xal_xms_get_params (BE_xmsConn, "cbal")
	xal_xms_deInit(BE_xmsConn)
	xipdbg("In Lua: Displaying balanceSuccessScreen: SC = " .. xmsSC .. "Balance = " .. uBalVal)

	if tonumber (xmsSC)  ==  0 then
		DisplayScreenFromRes("balanceSuccessScreen", GetSessionValue ("CURR").." "..uBalVal)
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
end

function BE_goHome ()
	ChangeXla("HomeScreen")
end

function DispScreen (scrName)
	DisplayScreenFromRes(scrName)
end

function BE_OnCancel()
	if(BE_nfcSess ~= 0) then 
		sysnfc_nfc_cancel(BE_nfcSess)
	end

	if(BE_xmsConn ~= 0) then 
		xal_xms_deInit(BE_xmsConn)
	end

	BE_goHome()
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
	while(count < 4) do
		array[count]=" "
		count = count + 1
	end
end
