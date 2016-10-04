--[[ XLA: ServerSync
-- Description: Implements the ServerSync feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

SS_xmsConn = 0
array = {}

function SS_OnLoad ()
	xipdbg("In Lua: SS_OnLoad")
	DispScreen("ServerSyncProgressScreen")
--	xal_GetAllXLAs ("NULL", "OnXlaGAPP",1, "GAPP")
    xal_CheckUpdates ("OnXlaGAPP")
end

function SERVERSYNC_CB ()
	xipdbg("In Lua: SERVERSYNC_CB, Res = " .. res)
	XMSSCData = xal_xms_get_params (SS_xmsConn, "sc")
	SCDetails = mysplit (XMSSCData,"|")
	xmsSC = SCDetails[1]
	xipdbg("In Lua: perso Status" .. xmsSC)
	
	xal_xms_deInit(SS_xmsConn)
	SS_xmsConn = 0
	if tonumber (xmsSC)  ==  0 or tonumber (xmsSC)  ==  0100 then
		xipdbg("In Lua: Displaying ServerSyncSuccessScreen: SC = " .. xmsSC )
		DisplayScreenFromRes("ServerSyncSuccessScreen")
	elseif tonumber (xmsSC)  ==  8888 then
		DisplayScreenFromRes("ServerSyncTimeout")
	else
	if SCDetails[2] ~= nil or SCDetails[2] ~= '' then
		if string.len(SCDetails[2]) > 20 then
			GetMultipleLines(SCDetails[2])
			DisplayScreenFromRes("ServerSyncFailureScreen", xmsSC, array[1], array[2], array[3], array[4], array[5])
		else
			DisplayScreenFromRes("ServerSyncFailureScreen", xmsSC, SCDetails[2] )
		end
	else
		err1,err2=GetTxnErrorStr( xmsSC )
		DisplayScreenFromRes("ServerSyncFailureScreen", xmsSC, err1, err2 )
	end
	end
end

function SS_goHome ()
    xipdbg("DispScreen: Canceling the updates ")
    xal_CancelUpdates()
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

function OnXlaGAPP (status, NumXla, NumXlaLen)
	xipdbg("OnXlaGAPP: Download Status =  " .. status .. " XmsStatus = " .. NumXla .. " XmsStatus Len " .. string.len(NumXla))
		
	if (tonumber (status)  == 200) then
        nXlas = tonumber(NumXla)
        xipdbg("OnXlaGAPP: nXlas  = " .. nXlas)
		if nXlas > 0 then 
			DisplayScreenFromRes("GAPPStatusScr","#Global:STATUS","#SYNCSUCC","#SYNCSUCC2"," ", "#NEWAPPS","#NEWAPPS1"," ", " "," "," ")
		else 
			DisplayScreenFromRes("GAPPStatusScr","#Global:STATUS","#SYNCSUCC1","#SYNCSUCC3"," ", "#CONT"," "," ","#Global:OK","SS_goHome","SS_goHome")
		end
	else
			DisplayScreenFromRes("GAPPStatusScrErr","#Global:STATUS", "","#SYNCERR","", "#CONT"," "," ")
	end
end
