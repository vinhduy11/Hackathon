--[[ XLA: SaleSummary
-- Description: Implements the SS feature for the POS XLA bundle. 
-- Date: 2 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

SS_xmsConn  = 0
SS_SodType = 0
array = {}

function SS_OnLoad ()
	xipdbg("Calling DisplayScreenFromRes")
	DispScreen("saleSummaryMenuScreen")
end

function SS_OnMenuBtn1 ()
	xipdbg("In Lua: SS_OnMenuBtn1")
--  SOD for TODAY
	SS_SodType = 0
	XmsRequest_SS ()
end

function SS_OnMenuBtn2 ()
	xipdbg("In Lua: SS_OnMenuBtn2")
--  SOD for YESTERDAY
	SS_SodType = 1
	XmsRequest_SS ()
end

function XmsRequest_SS ()
	xipdbg("In Lua: XmsRequest_SS")
	xipdbg("In Lua: Displaying saleSummaryProgressScreen")
	DisplayScreenFromRes("saleSummaryProgressScreen" )
	cntType = xal_xms_getcontentType()
	xipdbg("In Lua: XmsRequest_SS content Type:".. cntType )

	if( cntType == -1 ) then txnType = "SOD".."|".. "23/2f"
	else txnType = "SOD".."|"..cntType end

	xipdbg("In Lua: XmsRequest_SS txnType:".. txnType )
	SS_xmsConn=xal_xms_init("NULL", txnType, 0, "SS_CB")
	xid = GetDeviceXID()
	xal_xms_add_params( SS_xmsConn, "xid", xid )
	mccMnc = GetMncMcc()
	xal_xms_add_params( SS_xmsConn, "mcc", string.sub(mccMnc, 1, 3) )
	xal_xms_add_params( SS_xmsConn, "mnc", string.sub(mccMnc, 4, -1) )
	exid = GetDeviceExid()
	xal_xms_add_params( SS_xmsConn, "exid", exid  )
	xipdbg("Adding Param bd = " .. SS_SodType)	
	xal_xms_add_params( SS_xmsConn, "bd", tostring(SS_SodType) )
	ret = xal_xms_request(SS_xmsConn, 1)
end

function SS_CB ()
	local xmsSC = 0
	XMSSCData = xal_xms_get_params (SS_xmsConn, "sc")
	SCDetails = mysplit (XMSSCData,"|")
	xmsSC = SCDetails[1]
	xipdbg("In Lua: perso Status" .. xmsSC)
	
	sod = xal_xms_get_params (SS_xmsConn, "sod")
	eod = xal_xms_get_params (SS_xmsConn, "eod")
	ttc = xal_xms_get_params (SS_xmsConn, "ttc")
	tta = xal_xms_get_params (SS_xmsConn, "tta")
	xal_xms_deInit(SS_xmsConn)
	if tonumber (xmsSC)  ==  0 or tonumber (xmsSC)  ==  0100 then
		xipdbg("In Lua: Displaying saleSummarySuccessScreen: SC = " .. xmsSC .. "sod  " .. sod .. "eod  " ..  eod .. "ttc  " .. ttc .. "tta  " ..  tta )
		DisplayScreenFromRes("saleSummarySuccessScreen", sod, eod, ttc, GetSessionValue ("CURR").." "..tta)
	elseif tonumber (xmsSC)  ==  8888 then
		DisplayScreenFromRes("saleSummaryTimeout")
	else
		if string.len(SCDetails[2]) > 20 then
			GetMultipleLines(SCDetails[2])
			DisplayScreenFromRes("saleSummaryFailureScreen", xmsSC, array[1], array[2], array[3], array[4], array[5])
		else
			DisplayScreenFromRes("saleSummaryFailureScreen", xmsSC, SCDetails[2] )
		end
	end
end

function SS_goHome ()
	ChangeXla("HomeScreen")
end

function DispScreen (scrName)
	xipdbg("DispScreen: Calling DisplayScreenFromRes for screen   " .. scrName )
	DisplayScreenFromRes(scrName)
end

function SS_OnCancel()
	if(SS_xmsConn ~= 0) then 
		xal_xms_deInit(SS_xmsConn)
	end
	
	SS_goHome()
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
	while(count < 4) do
		array[count]=" "
		count = count + 1
	end
end
