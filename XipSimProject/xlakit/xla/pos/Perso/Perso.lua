--[[ XLA: Perso
-- Description: Implements the Perso tag feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

Perso_XmsConn 	= 0
Perso_NickName  = nil
Perso_Msisdn	= nil
Perso_MPIN		= 0
Perso_XID		= 0
Perso_Ispin  	= nil
Perso_OTP		= 0
Perso_nfcSess 	= 0
array = {}
Perso_MccMnc	= 0
Perso_Isuid 	= 0
Perso_msm 		= 0
Perso_Exid		= 0
Perso_Write     = 0
Perso_data      = 0
Pesro_respOtp   = 0

function Perso_OnLoad ()
	Perso_NickName = "Cliente"
	DisplayScreenFromRes("NFCProgress", " ", " ")
	Perso_nfcSess = sysnfc_init("Perso_OnNFCReadDetectData")
	if( Perso_nfcSess == -1 ) then 
		DisplayScreenFromRes("persoFailureScreen", "#Global:STATUS", " ", "#NFCFAIL", "#TRYAGAIN")
	end 
end

function  Perso_OnNFCReadDetectData (status, tagdata, tagdatalen)
	Perso_XID = tagdata
	Perso_nfcSess = 0
--	xipdbg("In Lua: Perso_OnNFCReadDetectData Perso_XID " .. Perso_XID)
--	xipdbg("In Lua: Perso_OnNFCReadDetectData " .. status)
	if (status == "true") then
		DisplayScreenFromRes("nickNameMSEntryScreen", "#GETNICK", "2", "15", "1", "Perso_NickNext")
	else
		DisplayScreenFromRes("persoFailureScreen", "#Global:STATUS", " ", " ", "#TAPTO")
	end
end

function Perso_NickNext( nickName )
	Perso_NickName = nickName
	if( Perso_NickName ~= nil ) then 
		DisplayScreenFromRes("nickNameMSEntryScreen", "#GETMSISDN", "3", "10", "0", "Perso_MsisdnNext")
	else
		DisplayScreenFromRes("persoFailureScreen", "#Global:STATUS", " ", " ", "#IMPRDATAENR")
	end 
end

function Perso_MsisdnNext( msisdn )
	Perso_Msisdn = msisdn
	if( Perso_Msisdn ~= nil ) then 
		Perso_Ispin = GetCustomerPinFlag()
		if( Perso_Ispin == -1) then
			Perso_Ispin = "N"
		end
		
		if( Perso_Ispin == "B") or ( Perso_Ispin == "M" ) then
			DisplayScreenFromRes("persoMPinOTPEntryScreen","#GETPIN", " ", "Perso_OnMPINNext")
		else
			XmsRequest_PersoTag()
		end
	else
		DisplayScreenFromRes("persoFailureScreen", "#Global:STATUS", " ", " ", "#IMPRDATAENR")
	end 
end

function Perso_OnMPINNext( mpin)
	if( mpin ~= nil and mpin:len() == 4 )then  
		Perso_MPIN = mpin
		XmsRequest_PersoTag()
	else
		DisplayScreenFromRes("persoMPinOTPEntryScreen", "#GETPIN","#INCRCT", "Perso_OnMPINNext" )
	end	
end

function XmsRequest_PersoTag ()
	DisplayScreenFromRes("persoProgressScreen", "#PERSOPROG")
	cntType = xal_xms_getcontentType()
--	xipdbg("In Lua: XmsRequest_PersoTag content Type:".. cntType )
	if( cntType == -1 ) then txnType = "PROV".."|".. "7/f"
	else txnType = "PROV".."|"..cntType end
	xipdbg("In Lua: XmsRequest_PersoTag txnType:".. txnType )
	Perso_XmsConn=xal_xms_init("NULL", txnType, 0, "PersoTag_CB")
	mccMnc = GetMncMcc()
	xal_xms_add_params( Perso_XmsConn, "mcc", string.sub(mccMnc, 1, 3) )
	xal_xms_add_params( Perso_XmsConn, "mnc", string.sub(mccMnc, 4, -1) )
	xal_xms_add_params( Perso_XmsConn, "xid", Perso_XID )
	xal_xms_add_params( Perso_XmsConn, "name", Perso_NickName )
	xal_xms_add_params( Perso_XmsConn, "ms", Perso_Msisdn )
	xal_xms_add_params( Perso_XmsConn, "ams", Perso_Msisdn )
	if( Perso_Ispin == "B") or (Perso_Ispin == "M" ) then
		xal_xms_add_params( Perso_XmsConn, "mpin", Perso_MPIN )
	end	
	xal_xms_add_params( Perso_XmsConn, "ispin", Perso_Ispin )
	ret = xal_xms_request(Perso_XmsConn, 1)
end

function PersoTag_CB ()
	XMSSCData = xal_xms_get_params (Perso_XmsConn, "sc")
	
	SCDetails = mysplit (XMSSCData,"|")
	xmsSC = SCDetails[1]
--	xipdbg("In Lua: perso Status" .. xmsSC)
	
	-- get tag perso data 
	mcc   = xal_xms_get_params (Perso_XmsConn, "mcc")
	mnc   = xal_xms_get_params (Perso_XmsConn, "mnc")
	name  =	xal_xms_get_params (Perso_XmsConn, "name")
	msm   = xal_xms_get_params (Perso_XmsConn, "msm")
	isuid =	xal_xms_get_params (Perso_XmsConn, "isuid")
	exid  =	xal_xms_get_params (Perso_XmsConn, "exid")
	Pesro_respOtp = xal_xms_get_params (Perso_XmsConn, "otp")
	Perso_data = mcc..mnc.."|"..name.."|"..msm.."|"..isuid.."|"..exid.."\r\n"
--	xipdbg("In Lua: perso data" .. Perso_data)
	
	xal_xms_deInit(Perso_XmsConn)
	Perso_XmsConn = 0
	if ( tonumber(xmsSC)  ==  0 or tonumber(xmsSC)  ==  0100 ) then
		if( Perso_Ispin == "B") or ( Perso_Ispin == "O" ) then
			DisplayScreenFromRes("persoMPinOTPEntryScreen", "#GETOTP"," ", "Perso_OnOTPNext" )
		else
			DisplayScreenFromRes("NFCProgress", "#NFCPRGS_1", "#NFCPRGS_2")
			Perso_nfcSess = sysnfc_init("Perso_OnNFCWriteDetectData")
		end
	elseif tonumber (xmsSC)  ==  8888 then
		DisplayScreenFromRes("persoFailureScreen", "#Global:TXNTO", "", "", "#Global:TXNFAIL")
	else	
		if string.len(SCDetails[2]) > 20 then
			GetMultipleLines(SCDetails[2])
			DisplayScreenFromRes("persoFailureScreen", "#Global:STATUS", xmsSC, array[1], array[2], array[3], array[4], array[5])
		else
			DisplayScreenFromRes("persoFailureScreen", "#Global:STATUS", xmsSC, SCDetails[2] )
		end
	end
end

function Perso_OnNFCWriteDetectData (status, tagdata, tagdatalen)
	xid = tagdata
--	xipdbg("In Lua: Perso_OnNFCWriteDetectData xid" .. xid)
--	xipdbg("In Lua: Perso_OnNFCWriteDetectData " .. status)
--	xipdbg("Got data [" .. tagdata .. "] with len of " .. tagdatalen)
	
	if (status == "true") then
		if( Perso_XID == xid ) then 
			-- persodata = xal_xms_get_params (Perso_XmsConn, "persotag")
			sysnfc_writeTagData(Perso_nfcSess, 1, 2, Perso_data, "Perso_OnNFCWriteData")
		else
			DispScreen("persoMultiTagFailureScreen")
		end
	else
		DisplayScreenFromRes("persoFailureScreen", "#Global:STATUS", " ", " ", "#TAPTO")
	end
end

function Perso_OnTagFailOK ()
	if(Perso_Write == 0 ) then 
		DisplayScreenFromRes("NFCProgress", "#NFCPRGS_1", "#NFCPRGS_2")
		Perso_nfcSess = sysnfc_init("Perso_OnNFCWriteDetectData")
	else
		DisplayScreenFromRes("NFCProgress", "#NFCPRGS_1", "#NFCRPGRS" )
		Perso_nfcSess = sysnfc_init("Perso_OnNFCReadDetectValData")
	end
end

function Perso_OnOTPNext( otp )
	if( otp ~= nil and otp:len() == 4  and Pesro_respOtp == otp )then 
		Perso_OTP = otp
		DisplayScreenFromRes("NFCProgress", "#NFCPRGS_1", "#NFCPRGS_2")
		Perso_nfcSess = sysnfc_init("Perso_OnNFCWriteDetectData")
	else
		DisplayScreenFromRes("oTPEntryScreen", "#INCROTP" )
		DisplayScreenFromRes("persoMPinOTPEntryScreen", "#GETOTP","#INCROTP", "Perso_OnOTPNext" )
	end	
end

function Perso_OnNFCWriteData (persoStatus)
	Perso_nfcSess = 0
	if (persoStatus == "true") then
		DisplayScreenFromRes("NFCProgress", "#NFCPRGS_1", "#NFCRPGRS" )
		Perso_Write = 1
		Perso_nfcSess = sysnfc_init("Perso_OnNFCReadDetectValData")
	else
		DisplayScreenFromRes("persoFailureScreen", "#Global:STATUS", " ", " ", "#TAPTO")
	end
end

function Perso_OnNFCReadDetectValData(status, tagdata, tagdatalen)
	xid = tagdata
--	xipdbg("In Lua: Perso_OnNFCReadDetectValData xid " .. xid)
--	xipdbg("In Lua: Perso_OnNFCReadDetectValData " .. status)
	if (status == "true") then
		if( Perso_XID == xid ) then 
			sysnfc_readTagData(Perso_nfcSess, 1, 2,"Perso_OnNFCReadValData")
		else
			DispScreen("persoMultiTagFailureScreen")
		end
	else
		DisplayScreenFromRes("persoFailureScreen", "#Global:STATUS", " ", " ", "#TAPTO" )
	end
end

function  Perso_OnNFCReadValData (status, tagdata, tagdatalen)
	local i = 1
--	xipdbg("In Lua: Perso_OnNFCReadValData " .. status)
--	xipdbg("Got data [" .. tagdata .. "] with len of " .. tagdatalen)
	Perso_nfcSess = 0
	if (status == "true") then
		for seg in string.gmatch(tagdata, "([^|]*)") do
			xipdbg("Parsed as [" .. seg .. "]" )
			if( i == 1 ) then Perso_MccMnc = seg
			elseif( i == 3) then Perso_NickName = seg
			elseif( i == 5 ) then Perso_msm = seg
			elseif( i == 7) then Perso_Isuid = seg
			elseif( i == 9) then Perso_Exid = seg
			elseif( i == 11) then xid = seg
			else
			end 
			i = i + 1	
		end
		
		if( Perso_XID == xid and Perso_Exid ~= 0 and Perso_Isuid ~= 0 and Perso_NickName ~= 0 and Perso_MccMnc ~= 0 and Perso_msm ~= 0)then 
			XmsRequest_ValTag ()
		else
			DispScreen("persoMultiTagFailureScreen")
		end
		
	else
		DisplayScreenFromRes("persoFailureScreen", "#Global:STATUS", " ", " ", "#TAPTO")
	end
end

function XmsRequest_ValTag ()
--	xipdbg("In Lua: XmsRequest_ValTag")
	DisplayScreenFromRes("persoProgressScreen", "#VALPROG")
	cntType = xal_xms_getcontentType()
--	xipdbg("In Lua: XmsRequest_ValTag content Type:".. cntType )
	if( cntType == -1 ) then txnType = "VAL".."|".. "7/f"
	else txnType = "VAL".."|"..cntType end
--	xipdbg("In Lua: XmsRequest_ValTag txnType:".. txnType )
	Perso_XmsConn=xal_xms_init("NULL", txnType, 0, "ValTag_CB")
	xal_xms_add_params( Perso_XmsConn, "mcc", string.sub(Perso_MccMnc, 1, 3) )
	xal_xms_add_params( Perso_XmsConn, "mnc", string.sub(Perso_MccMnc, 4, -1) )
	xal_xms_add_params( Perso_XmsConn, "name", Perso_NickName )
	xal_xms_add_params( Perso_XmsConn, "m4", string.sub(Perso_msm, string.len( Perso_msm ) - 3, -1) )
	xal_xms_add_params( Perso_XmsConn, "pv", "0" )
	xal_xms_add_params( Perso_XmsConn, "isuid", Perso_Isuid )
	xal_xms_add_params( Perso_XmsConn, "exid", Perso_Exid )
	xal_xms_add_params( Perso_XmsConn, "xid", Perso_XID )
	xal_xms_add_params( Perso_XmsConn, "ams", Perso_Msisdn )
	xal_xms_add_params( Perso_XmsConn, "ispin", Perso_Ispin )
	if( Perso_Ispin == "B") or ( Perso_Ispin == "M" ) then
		xal_xms_add_params( Perso_XmsConn, "mpin", Perso_MPIN )
	end
	if( Perso_Ispin == "B") or ( Perso_Ispin == "O" ) then
		xal_xms_add_params( Perso_XmsConn, "otp", Perso_OTP )
	end
	ret = xal_xms_request(Perso_XmsConn, 1)
end


function ValTag_CB ()
	XMSSCData = xal_xms_get_params (Perso_XmsConn, "sc")
	SCDetails = mysplit (XMSSCData,"|")
	xmsSC = SCDetails[1]
--	xipdbg("In Lua: perso Status" .. xmsSC)
	xal_xms_deInit(Perso_XmsConn)
	Perso_XmsConn = 0
	if tonumber (xmsSC)  ==  0 or tonumber (xmsSC)  ==  0100 then
		DisplayScreenFromRes("persoSuccessScreen" )
	elseif tonumber (xmsSC)  ==  8888 then
		DisplayScreenFromRes("persoFailureScreen", "#Global:TXNTO", "", "", "#Global:TXNFAIL")
	else
		if string.len(SCDetails[2]) > 20 then
			GetMultipleLines(SCDetails[2])
			DisplayScreenFromRes("persoFailureScreen", "#Global:STATUS", xmsSC, array[1], array[2], array[3], array[4], array[5])
		else
			DisplayScreenFromRes("persoFailureScreen", "#Global:STATUS", xmsSC, SCDetails[2] )
		end
	end
end

function Perso_goHome ()
	ChangeXla("HomeScreen")
end

function DispScreen (scrName)
	DisplayScreenFromRes(scrName)
end

function Perso_OnCancel()
	if( Perso_nfcSess ~= 0 ) then 
		sysnfc_nfc_cancel(Perso_nfcSess)
	end
	
	if(Perso_XmsConn ~= 0) then 
		xal_xms_deInit(Perso_XmsConn)
	end
	
	Perso_goHome()
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
