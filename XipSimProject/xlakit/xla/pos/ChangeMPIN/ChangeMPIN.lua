--[[ XLA: ChangeMPIN
-- Description: Implements the Change mPIN feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]


CHNGMPIN_xmsConn    = 0
CHNGMPIN_OldMPIN    = nil
CHNGMPIN_NewMPIN    = nil
CHNGMPIN_Xid		= 0
CHNGMPIN_Exid		= 0
CHNGMPIN_nfcSess    = 0
array = {}
CHNGMPIN_respOtp    = 0

function CHNGMPIN_OnLoad ()
	DisplayScreenFromRes("changeMPinOldMPinEntryScreen", " ")
end

function CHNGMPIN_OnOldMPINNext (mPIN)
	if( mPIN ~= nil and mPIN:len() == 4 )then
		CHNGMPIN_OldMPIN=mPIN
		DisplayScreenFromRes("changeMPinNewMPinEntryScreen", " ")	
	else
		DisplayScreenFromRes("changeMPinOldMPinEntryScreen", "#INCRCT")
	end
end

function CHNGMPIN_OnNewMPINNext (mPIN)	
	if( mPIN ~= nil and mPIN:len() == 4 )then
		CHNGMPIN_NewMPIN=mPIN
		DisplayScreenFromRes("changeMPinReEnterNewMPinEntryScreen", " ")
	else
		DisplayScreenFromRes("changeMPinNewMPinEntryScreen", "#INCRCT")
	end
end

function CHNGMPIN_OnReNewMPINNext (mPIN)
	if( mPIN ~= nil and mPIN:len() == 4  )then
		if( CHNGMPIN_NewMPIN == mPIN  )then
			XmsRequest_CHNGPIN ()
		else
			DispScreen("changeMPinMPinsNotMatchedScreen")
		end 
	else
		DisplayScreenFromRes("changeMPinReEnterNewMPinEntryScreen", "#INCRCT")
	end
end

function XmsRequest_CHNGPIN ()
	xipdbg("In Lua: XmsRequest_CHNGPIN")
	
	DisplayScreenFromRes("changeMPinProgressScreen", "#CHNGPROG" )
	cntType = xal_xms_getcontentType()
	if( cntType == -1 ) then txnType = "TCP".."|".. "7/f"
	else txnType = "TCP_HND".."|"..cntType end
	CHNGMPIN_xmsConn=xal_xms_init("NULL", txnType, 0, "CHNGPIN_CB")
	-- xipdbg("Adding Param old mPIN = " .. CHNGMPIN_OldMPIN)		
	xal_xms_add_params( CHNGMPIN_xmsConn, "mp", CHNGMPIN_OldMPIN )
	
	if( CHNGMPIN_NewMPIN ~= nil ) then 
	--	xipdbg("Adding Param new mPIN = " .. CHNGMPIN_NewMPIN)
		xal_xms_add_params( CHNGMPIN_xmsConn, "nmp", CHNGMPIN_NewMPIN )
	end
	
	mccMnc = GetMncMcc()
	xal_xms_add_params( CHNGMPIN_xmsConn, "mcc", string.sub(mccMnc, 1, 3) )
	xal_xms_add_params( CHNGMPIN_xmsConn, "mnc", string.sub(mccMnc, 4, -1) )
	
	xid = GetDeviceXID()
	xal_xms_add_params( CHNGMPIN_xmsConn, "xid", xid )
	exid = GetDeviceExid()
	xal_xms_add_params( CHNGMPIN_xmsConn, "exid", exid  )
	ret = xal_xms_request(CHNGMPIN_xmsConn, 1)
end

function CHNGPIN_CB ()
	XMSSCData = xal_xms_get_params (CHNGMPIN_xmsConn, "sc")
	SCDetails = mysplit (XMSSCData,"|")
	xmsSC = SCDetails[1]
	xipdbg("In Lua: perso Status" .. xmsSC)
	CHNGMPIN_respOtp = xal_xms_get_params (CHNGMPIN_xmsConn, "otp")
	xipdbg("In Lua:  respOtp = " .. CHNGMPIN_respOtp)

	xal_xms_deInit(CHNGMPIN_xmsConn)
	CHNGMPIN_xmsConn = 0
	if tonumber (xmsSC)  ==  0 or tonumber (xmsSC)  ==  0100 then
		DisplayScreenFromRes("changeMPinSuccessScreen")	
	elseif tonumber (xmsSC)  ==  8888 then
		DisplayScreenFromRes("changeMPinTimeout")
	else		
		if string.len(SCDetails[2]) > 20 then
			GetMultipleLines(SCDetails[2])
			DisplayScreenFromRes("changeMPinFailureScreen", xmsSC, array[1], array[2], array[3], array[4], array[5])
		else
			DisplayScreenFromRes("changeMPinFailureScreen", xmsSC, SCDetails[2] )
		end
	end
end

function CHNGMPIN_goHome ()
	ChangeXla("HomeScreen")
end

function DispScreen (scrName)
	DisplayScreenFromRes(scrName)
end

function CHNGMPIN_OnCancel()
	if(CHNGMPIN_xmsConn ~= 0) then 
		xal_xms_deInit(CHNGMPIN_xmsConn)
	end
	
	CHNGMPIN_goHome()
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
