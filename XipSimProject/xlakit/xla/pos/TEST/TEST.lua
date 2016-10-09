--[[ XLA: SendMoney
-- Description: Implements the SM feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

SM_xmsConn = 0
SM_Amount = nil
SM_PHONE  = nil
SM_MPIN = 0
array = {}

function SM_OnLoad ()
	xipdbg("Calling DisplayScreenFromRes")
	
   DisplayScreenFromRes("PhoneScreen", "")
	--DispScreen("PhoneScreen")
end

function SM_OnPhoneNext (phone)
  xipdbg("In Lua: Phone = " .. phone)
  if(phone ~= nil and string.find(phone, '0') == 1 and (string.len(phone) == 11 or string.len(phone) == 10)) then
    SM_PHONE = phone
    DisplayScreenFromRes("AmountEntryScreen")
  else
    xipdbg("phone error")
    DisplayScreenFromRes("PhoneScreen", "#PHONE_E")
  end
end

function SM_OnAmountNext (amount)
	DisplaySetMaxInputDataLen("0")
	-- xipdbg("In Lua: Amouont = " .. amount)
	SM_Amount	= amount
	if( SM_Amount ~= nil and tonumber( SM_Amount ) > 0 ) then 
		xipdbg("In Lua: Displaying MoneyMPinEntryScreen: amount = " .. SM_Amount .. "phone = " .. SM_PHONE)
		DisplayScreenFromRes("MoneyMPinEntryScreen", " ", "So tien: " .. comma_value(SM_Amount) .. "VND", "SDT: " .. SM_PHONE )
	else
		--SM_goHome ()
		DisplayScreenFromRes("AmountEntryScreen", "#AMT_E")
	end
end

function SM_OnMPINNext (mPIN)
--	xipdbg("Calling XMS Request For SM, Pin = " .. mPIN )
	if( mPIN ~= nil and mPIN:len() == 6 )then
		SM_MPIN=mPIN
		XmsRequest_SM()
	else
		DisplayScreenFromRes("MoneyMPinEntryScreen", "#INCRCT", "So tien: " .. comma_value(SM_Amount) .. "VND", "SDT: " .. SM_PHONE )
	end	
end

function XmsRequest_SM ()
  -- Fix lai
  xipdbg("In Lua: XmsRequest_SM")
  DisplayScreenFromRes("MoneyProgressScreen", comma_value(SM_Amount) .. "VND", SM_PHONE)
  cntType = xal_xms_getcontentType()
  if( cntType == -1 ) then txnType = "TEAM_TEST".."|".. "7/f"
  else txnType = "TEAM_TEST".."|"..cntType end
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
  xal_xms_add_params( SM_xmsConn, "rms", SMM_MerNo )
  xipdbg("Adding Param Mode of com= " .. "2")   
  xal_xms_add_params( SM_xmsConn, "com", "2" )
  xipdbg("Adding Param MPIN = " .. SM_MPIN)  
  xal_xms_add_params( SM_xmsConn, "mp", SM_MPIN )
  xal_xms_add_params( SM_xmsConn, "msgType", "SOAP_TRANSFER" )
  
   xal_xms_add_params( SM_xmsConn, "fromUser", "01696945543" )
   xal_xms_add_params( SM_xmsConn, "fromPass", SM_MPIN )
   xal_xms_add_params( SM_xmsConn, "toUser", SM_PHONE )
   xal_xms_add_params( SM_xmsConn, "amount", SM_Amount )
  ret = xal_xms_request(SM_xmsConn, 1)
end

function SM_CB ()
  XMSSCData = xal_xms_get_params (SM_xmsConn, "sc")
  xipdbg("In Lua: perso Status" .. XMSSCData)
  SCDetails = mysplit (XMSSCData,"|")
  xmsSC = SCDetails[1]
  xipdbg("In Lua: perso Status" .. xmsSC)
  
  phoneSender = xal_xms_get_params (SM_xmsConn, "fromUser")
  originalAmount = xal_xms_get_params (SM_xmsConn, "amount")
  tranId = xal_xms_get_params (SM_xmsConn, "tranId")
  errorDesc = xal_xms_get_params (SM_xmsConn, "errorDesc")
  txnId = xal_xms_get_params (SM_xmsConn, "tranId")
  xal_xms_deInit(SM_xmsConn)
  SM_xmsConn = 0
   xipdbg("In Lua: perso Status" .. errorDesc)
  if tonumber (xmsSC)  ==  0 or tonumber (xmsSC)  ==  0100 then
    if errorDesc ~= "THANH CONG" then
        DisplayScreenFromRes("sendMoneyFailure", xmsSC,errorDesc )
    else
        DisplayScreenFromRes("MoneySuccessScreen", phoneSender, GetCurrencySymbol().." "..SM_Amount, SM_PHONE, txnId)
    end
  elseif tonumber (xmsSC)  ==  8888 then
    DisplayScreenFromRes("sendMoneyTimeout")
  else
    if string.len(SCDetails[2]) > 20 then
      GetMultipleLines(SCDetails[2])
      DisplayScreenFromRes("MoneyFailureScreen", xmsSC, array[1], array[2], array[3], array[4], array[5] )
    else
      DisplayScreenFromRes("MoneyFailureScreen", xmsSC, SCDetails[2] )
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

function comma_value(n) -- credit http://richard.warburton.it
  local left,num,right = string.match(n,'^([^%d]*%d)(%d*)(.-)$')
  return left..(num:reverse():gsub('(%d%d%d)','%1,'):reverse())..right
end