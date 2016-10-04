--[[ XLA: SetAuth
-- Description: Implements the SetAuth feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

SA_xmsConn = 0
array = {}

function SA_OnLoad ()
	xipdbg("In Lua: SA_OnLoad")
	DispScreen("setAuthProgressScreen")
	cntType = xal_xms_getcontentType()
	if( cntType == -1 ) then txnType = "SETAUTH".."|".. "7/f"
	else txnType = "SETAUTH".."|"..cntType end
	
	SA_xmsConn=xal_xms_init("NULL", txnType, 0, "SETAUTH_CB")
	ret = xal_xms_request(SA_xmsConn, 1)
end

function SETAUTH_CB ()
	XMSSCData = xal_xms_get_params (SA_xmsConn, "sc")
	SCDetails = mysplit (XMSSCData,"|")
	xmsSC = SCDetails[1]
	xipdbg("In Lua: perso Status" .. xmsSC)

	xal_xms_deInit(SA_xmsConn)
	SA_xmsConn = 0 
	if tonumber (xmsSC)  ==  0 or tonumber (xmsSC)  ==  0100 then
		xipdbg("In Lua: Displaying setAuthSuccessScreen: SC = " .. xmsSC )
		DisplayScreenFromRes("setAuthSuccessScreen")
	elseif tonumber (xmsSC)  ==  8888 then
		DisplayScreenFromRes("setAuthTimeout")
	else
		if string.len(SCDetails[2]) > 20 then
			GetMultipleLines(SCDetails[2])
			DisplayScreenFromRes("setAuthFailureScreen", xmsSC, array[1], array[2], array[3], array[4], array[5])
		else
			DisplayScreenFromRes("setAuthFailureScreen", xmsSC, SCDetails[2] )
		end
	end
end

function SA_goHome ()
	ChangeXla("HomeScreen")
end

function DispScreen (scrName)
	xipdbg("DispScreen: Calling DisplayScreenFromRes for screen   " .. scrName )
	DisplayScreenFromRes(scrName)
end

function SA_OnCancel()
	if(SA_xmsConn ~= 0) then 
		xal_xms_deInit(SA_xmsConn)
	end
	
	SA_goHome()
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
