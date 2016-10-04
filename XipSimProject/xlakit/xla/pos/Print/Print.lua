--[[ XLA:Global
-- Description: Handles common BT and Print functionality
-- Date: 9 SEP 2015
-- Author: SG
-- History: Initial version created

--]]

PT_XmsConn = 0
prtBuf = nil
prtFlag = 0
prtLen = 0
PT_XLAName = 0
PT_XLACallback = 0
printerName = "DL58"
resStr1 = ""
resStr2 = ""
resStr3 = ""
resStr4 = ""

function PT_OnInstall ()
 	xipdbg("OnInstall invoked")
end

function PT_OnLoad ()
	xipdbg("Global Lua OnLoad")
end

function PT_BTPrintBuff (xlaname, callback, flag, prtBufDecode, prtLenDecode )
	PT_XLAName = xlaname
	PT_XLACallback = callback
	prtFlag = flag
	prtBuf = prtBufDecode
	prtLen = prtLenDecode
	
	xipdbg("PT_BTPrintBuff:prtFlag : "..prtFlag)
	--prtBuf,prtLen = xal_xms_get_params_large_data (PT_XmsConn, "prt")
	--prtFlag,prtLen,prtBuf = GetXlaVarVal(xlaname, 3, flag, length, buffer) ;
	
	xipdbg("PT_BTPrintBuff:prtLen : "..prtLen)
	xipdbg("PT_BTPrintBuff:prtBuf : "..prtBuf)
	xipdbg("PT_BTPrintBuff:prtBufLen : "..prtBuf:len() )
	
	ret = bt_ispaired (printerName)
	xipdbg("PT_BTPrintBuff:bt is paried ret : "..ret)
	if (ret == 1) then
		DisplayScreenFromRes("Print:printerProgress")
		--check if buffer is available to print or needs to be retrieved from xms
		if (prtLen ~= 0) then
			--initiate bt and print; buffer is in prtBuf
			bt_init("Print:OnBTInit")
		else
			--initiate a xms request
			xipdbg("PT_BTPrintBuff:Call GPB ")
			XmsRequest_GPB()
		end
	else
		resStr1 = GetXlaString("Print:NOPRINTER")
		resStr2 = GetXlaString("Print:NOPRNMSG1")
		resStr3 = GetXlaString("Print:NOPRNMSG2")
		resStr4 = GetXlaString("Print:NOPRNMSG3")
		DisplayScreenFromRes("Print:failurePage", resStr1, resStr2, resStr3, resStr4)
	end
	
	--
	xipdbg("PT_BTPrintBuff:PT_XLAName : "..PT_XLAName)
end

function OnBTInit(result)
	xipdbg("Init Result "..result)
	if (result == "0") then
		bt_connect(printerName, "Print:OnBTConnect")
	else
		--show failure screen
		resStr1 = GetXlaString("Print:PRNERR")
		resStr2 = GetXlaString("Print:PRNERRMSG1")
		DisplayScreenFromRes("Print:failurePage", resStr1, resStr2)
	end
end

function OnBTConnect(result)
	if (result == "0") then
		xipdbg("RCV_btPrint:prtFlag : "..prtFlag)
		xipdbg("RCV_btPrint:prtLen : "..prtLen)
		bt_send(prtBuf, prtLen, 50, "Print:OnSend")
	else
		--show failure screen
		resStr1 = GetXlaString("Print:PRNNOTFOUND")
		resStr2 = GetXlaString("Print:PRNMSG1")
		resStr3 = GetXlaString("Print:PRNMSG2")
		resStr4 = GetXlaString("Print:PRNMSG3")
		DisplayScreenFromRes("Print:failurePage", resStr1, resStr2, resStr3, resStr4)
	end	
end

function OnSend(result)
	if (result == "0") then
		--success screen
		DisplayScreenFromRes("Print:successPage")
	else
		--failure screen
		resStr1 = GetXlaString("Print:PRNERR")
		resStr2 = GetXlaString("Print:PRNMSG1")
		resStr3 = GetXlaString("Print:PRNMSG2")
		resStr4 = GetXlaString("Print:PRNMSG3")
		DisplayScreenFromRes("Print:failurePage", resStr1, resStr2, resStr3, resStr4)
	end
end

function Print_OnReprint()
	bt_send(prtBuf, prtLen, 50, "Print:OnSend")
end

function OnDisconnect(result)
end

function XmsRequest_GPB ()
	xipdbg("In Lua: XmsRequest_RCV")
	res=xal_xms_init("NULL", "GPB|7/f", 0, "Print:GPB_CB")
	xipdbg("res = " .. res)
	PT_XmsConn = res
	
	xid = GetConfigValue("imei")
	xal_xms_add_params( PT_XmsConn, "xid", xid )
	mccMnc = GetSessionValue("MNC")
	xal_xms_add_params( PT_XmsConn, "mcc", string.sub(mccMnc, 1, 3) )
	xal_xms_add_params( PT_XmsConn, "mnc", string.sub(mccMnc, 4, -1) )
	exid = GetConfigValue("exid")
	xal_xms_add_params( PT_XmsConn, "exid", exid  )
	ret = xal_xms_request(PT_XmsConn, 1)
end

function GPB_CB()
	XMSSCData = xal_xms_get_params (PT_XmsConn, "sc")
	SCDetails = mysplit (XMSSCData,"|")
	xmsSC = SCDetails[1]
	xipdbg("In Lua: GPB_CB Status" .. xmsSC)
	
	if tonumber (xmsSC)  ==  0 or tonumber (xmsSC)  ==  0100 then
		--prtBuf,prtLen = xal_xms_get_params_large_data (PT_XmsConn, "prt")
		prtBufEncode = xal_xms_get_params (STU_XmsConn, "prt")
		prtBuf, prtLen = xip_sec_base64_decode( prtBufEncode )
		xipdbg("In Lua: GPB_CB PrnLen" .. prtLen)
		xipdbg("PT_BTPrintBuff:prtBufLen : "..prtBuf:len() )
		xal_xms_deInit(PT_XmsConn)
		if( PrnLen ~= 0 )  then 
			xipdbg("In Lua: GPB_CB PrintBuffer : " .. prtBuf)
			bt_init("Print:OnBTInit")
		else
			--Show Failure screen
			resStr1 = GetXlaString("Print:PRNERR")
			resStr2 = GetXlaString("Print:PRNERRMSG1")
			DisplayScreenFromRes("Print:failurePage", resStr1, resStr2)
			--DisplayScreenFromRes("Print:printFailureScreen", "Failed to connect.")
			xip_buzzer_viberator_sound( 1000 )
		end
	else
		--Show Failure Screen
		xal_xms_deInit(PT_XmsConn)
		resStr1 = GetXlaString("Print:PRNERR")
		resStr2 = GetXlaString("Print:PRNERRMSG1")
		DisplayScreenFromRes("Print:failurePage", resStr1, resStr2)
		--DisplayScreenFromRes("Print:printFailureScreen", "Failed to connect.")
		xipdbg("Printer not ready")
		xip_buzzer_viberator_sound( 1000 )
	end
	
	
end

function Print_OnOK()
	xipdbg("Print Lua Print_OnOK")
	--if (RCV_XmsConn ~= 0) then
	--	xal_xms_deInit(RCV_XmsConn)
	--end
	bt_deinit() ;
	ExecuteXlaFunction (PT_XLAName, PT_XLACallback, 1, 1, 1)
end

function Print_OnCancel()
	bt_cancel()
	Print_OnOK()
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

