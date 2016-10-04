--[[ XLA: GetConfig
-- Description: Implements the GetConfig feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

GC_xmsConn = 0
array = {}
XMSTRGData = 0

function GC_OnLoad ()
	xipdbg("In Lua: GC_OnLoad")

	DispScreen("getConfigProgressScreen")
	cntType = xal_xms_getcontentType()
	xipdbg("In Lua: GC_OnLoad content Type:".. cntType )
	if( cntType == -1 ) then txnType = "GCON".."|".. "7/f"
	else txnType = "GCON".."|"..cntType end
	xipdbg("In Lua: GC_OnLoad txnType:".. txnType )

	GC_xmsConn=xal_xms_init("NULL", txnType, 0, "CONFIG_CB")

	xid = GetDeviceXID()
	xal_xms_add_params( GC_xmsConn, "xid", xid )
	mccMnc = GetMncMcc()
	xal_xms_add_params( GC_xmsConn, "mcc", string.sub(mccMnc, 1, 3) )
	xal_xms_add_params( GC_xmsConn, "mnc", string.sub(mccMnc, 4, -1) )
	exid = GetDeviceExid()
	xal_xms_add_params( GC_xmsConn, "exid", exid  )

	-- transaction statistics
	tr = GetTotalRequestCount() if( tr == -1 ) then tr = 0  end
	ss = GetSuccessRespCount() if( ss == -1 ) then ss = 0  end
	fl = GetFailureRespCount() if( fl == -1 ) then fl = 0  end
	to = GetTimeoutCount() if( to == -1 ) then to = 0  end
	ce = GetConnectErrorCount() if( ce == -1 ) then ce = 0  end
	uc = GetUserCancelCount() if( uc == -1 ) then uc = 0  end
	stat = tr .. "," ..  ss .. "," .. fl .. ",".. to .. "," .. ce .."," .. uc 

	xal_xms_add_params( GC_xmsConn, "tstat", stat  )
	ret = xal_xms_request(GC_xmsConn, 1)
end

function CONFIG_CB ()
	XMSSCData = xal_xms_get_params (GC_xmsConn, "sc")
	SCDetails = mysplit (XMSSCData,"|")
	xmsSC = SCDetails[1]
	xipdbg("In Lua: perso Status" .. xmsSC)
	
	XMSTRGData = xal_xms_get_params (GC_xmsConn, "trg" )
	xipdbg("In Lua: CONFIG_CB: XMSTRGData = " .. XMSTRGData )
	
	xal_xms_deInit(GC_xmsConn)
	GC_xmsConn = 0
	if tonumber (xmsSC)  ==  0 or tonumber (xmsSC)  ==  0100 then
		xipdbg("In Lua: Displaying getConfigSuccessScreen: SC = " .. xmsSC )
		-- update the profile type to display HS1 for avoid hang issue if category has changed 
		SetConfigValue("profType", "0" )
		DisplayScreenFromRes("getConfigSuccessScreen")
	elseif tonumber (xmsSC)  ==  8888 then
		DisplayScreenFromRes("getConfigTimeout")
	else		
		if string.len(SCDetails[2]) > 20 then
			GetMultipleLines(SCDetails[2])
			DisplayScreenFromRes("getConfigFailureScreen", xmsSC, array[1], array[2], array[3], array[4], array[5])
		else
			DisplayScreenFromRes("getConfigFailureScreen", xmsSC, SCDetails[2] )
		end
	end
end

function GC_goHome ()
	TrigGCon = GetSessionValue ("FetchTrigGCon")
	
	if( tonumber (XMSTRGData)  ==  1 or tonumber( TrigGCon ) == 1 ) then 
		SetSessionValue ("FetchTrigGCon", "0")
		ChangeXla ("FetchTrigger")
	else
		ChangeXla("HomeScreen")
	end
end

function DispScreen (scrName)
	xipdbg("DispScreen: Calling DisplayScreenFromRes for screen   " .. scrName )
	DisplayScreenFromRes(scrName)
end

function GC_OnCancel()
	if(GC_xmsConn ~= 0) then 
		xal_xms_deInit(GC_xmsConn)
	end
	GC_goHome()
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
