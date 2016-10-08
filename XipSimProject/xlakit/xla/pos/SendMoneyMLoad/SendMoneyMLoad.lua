--[[ XLA: SendMoney
-- Description: Implements the SM feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

SMM_RET = nil
SMM_xmsConn = 0
SMM_MerNo  = nil
SM_MPIN = 0
SM_Amount = nil

SM_PHONE = nil
array = {}


MS_Screen = nil

function SMM_OnLoad ()
	
	SMM_RET = GetConfigValue ("MLTYPE")	
	if (SMM_RET == "1") then
	 DispScreen("sendMoneyMLoadPhoneEntryScreen")
	elseif (SMM_RET == "2") then
	 DispScreen("tranferMoneyMLoadPhoneEntryScreen")
  end
 
end

function SMM_OnPhoneNext (phone)
  xipdbg("In Lua: Phone = " .. phone)
  xipdbg("In Lua: Phone = " .. string.len(phone))
  if(string.find(phone, '0') == 1 and (string.len(phone) == 11 or string.len(phone) == 10)) then
    SM_PHONE = phone
    SMM_MerNo = phone
    SetConfigValue("PHONE", phone)
    
    if (SMM_RET == "1") then
      DisplayScreenFromRes("MoneyScreen1")
      SetConfigValue("profType", "0" )
      MS_Screen = "0"
    else
      DisplayScreenFromRes("MoneyScreenInputAmount", "#AMTT")
    end
    
   else
    xipdbg("phone error")
    DisplayScreenFromRes("sendMoneyMLoadPhoneEntryScreen", "#INCRCT")
  end
end

function SM_OnMPINNext (mPIN)
--	xipdbg("Calling XMS Request For SM, Pin = " .. mPIN )
	if( mPIN ~= nil and mPIN:len() == 4 )then
		SM_MPIN=mPIN
		XmsRequest_SM()
	else
		DisplayScreenFromRes("sendMoneyMPinEntryScreen", "#INCRCT", GetCurrencySymbol().." "..SM_Amount, SMM_MerNo )
	end	
end




function SMM_goHome ()
	ChangeXla("HomeScreen")
end

function DispScreen (scrName)
	xipdbg("DispScreen: Calling DisplayScreenFromRes for screen   " .. scrName )
	DisplayScreenFromRes(scrName)
end

function SMM_OnCancel()
	DisplaySetMaxInputDataLen("0")
	if(SM_xmsConn ~= 0) then 
		xal_xms_deInit(SM_xmsConn)
	end
	
	SMM_goHome()
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


--###################################
function MS_OnLoad ()
  MS_Screen = GetConfigValue("profType")
  --MS_PHONE = GetConfigValue("PHONE")
  xipdbg("In Lua: profile Type".. MS_Screen )
  if(MS_Screen == -1) then MS_Screen = "0" end 
  
  if (tonumber(MS_Screen) == 0) then
    DisplayScreenFromRes("MoneyScreen1")
  elseif (tonumber(MS_Screen ) == 1) then
    DisplayScreenFromRes("MoneyScreen2")
  end
end

function MS_OnRight()
  if (tonumber(MS_Screen) == 0) then
    DisplayScreenFromRes("MoneyScreen2")
    SetConfigValue("profType", "1" )
    MS_Screen = "1"
  elseif ( tonumber( MS_Screen ) == 1) then
    DisplayScreenFromRes("MoneyScreen1")
    SetConfigValue("profType", "0" )
    MS_Screen = "0"
  end
end

function MS_OnLeft()
  if ( tonumber( MS_Screen ) == 0) then
    DisplayScreenFromRes("MoneyScreen2")
    SetConfigValue("profType", "1" )
    MS_Screen = "1"
  elseif ( tonumber( MS_Screen ) == 1) then
    DisplayScreenFromRes("MoneyScreen1")
    SetConfigValue("profType", "0" )
    MS_Screen = "0"
  end
end

function MS_OnMoney10KSelect()
  xipdbg("amount = 10k")
  MS_OnMoney(10000)
end

function MS_OnMoney20KSelect()
  xipdbg("amount = 20k")
  MS_OnMoney(20000)
end

function MS_OnMoney30KSelect()
  xipdbg("amount = 30k")
  MS_OnMoney(30000)
end

function MS_OnMoney50KSelect()
  xipdbg("amount = 50k")
  MS_OnMoney(50000)
end

function MS_OnMoney100KSelect()
  xipdbg("amount = 100k")
  MS_OnMoney(100000)
end

function MS_OnMoney200KSelect()
  xipdbg("amount = 200k")
  MS_OnMoney(200000)
end

function MS_OnMoney500KSelect()
  xipdbg("amount = 500k")
  MS_OnMoney(500000)
end

function SM_OnCancel()
  DisplaySetMaxInputDataLen("0")
  if(SM_xmsConn ~= 0) then 
    xal_xms_deInit(SM_xmsConn)
  end
  
  SM_goHome()
end
function SM_goHome ()
  ChangeXla("HomeScreen")
end
function MS_OnMoneyANOSelect()
  xipdbg("amount = ANO")
  xipdbg("change screen")
  DisplayScreenFromRes("MoneyScreenInputAmount", "#AMT")
end

function MS_OnMoney(amount)
  xipdbg("amount pass" .. amount)
  xipdbg("Confirm")
  -- check so tien 
  SM_Amount = amount
   DisplayScreenFromRes("sendMoneyMPinEntryScreen", "", "ST: ".. SM_Amount .. "D", "SDT: " .. SMM_MerNo, "#GETPIN_1")
  --maxAmt = GetPinlessEndAmount()
  --DisplaySetMaxInputDataLen("99999")
end

function MS_OnInputNNext(amount)
  xipdbg("amount = " .. amount)
  MS_OnMoney(amount)
end
function MS_OnCancel()
  DisplaySetMaxInputDataLen("0")
  if(SM_xmsConn ~= 0) then 
    xal_xms_deInit(SM_xmsConn)
  end
  if(SMM_RET == "1") then
    DisplayScreenFromRes("MoneyScreen1")
    SetConfigValue("profType", "0" )
    MS_Screen = "0"
  else
    SMM_goHome()
  end
end


function MS_OnMPINNext (mPIN)
  if( mPIN ~= nil and mPIN:len() == 6 )then
    SM_MPIN=mPIN
    XmsRequest_SM()
  else
    if(SMM_RET == "1") then
      DisplayScreenFromRes("sendMoneyMPinEntryScreen", "#INCRCTPIN", "ST: ".. SM_Amount .. "D", "SDT: " .. SMM_MerNo, "#GETPIN_1")
    else
      DisplayScreenFromRes("sendMoneyMPinEntryScreen", "#INCRCTPIN", "ST: ".. SM_Amount .. "D", "SDT: " .. SMM_MerNo, "#GETPIN_2")
    end
  end 
end


function XmsRequest_SM ()
  -- Fix lai
  xipdbg("In Lua: XmsRequest_SM")
  if (SMM_RET == "1") then
   DisplayScreenFromRes("MoneyProgressScreen", "#SMPROG_1", "")
  else
   DisplayScreenFromRes("MoneyProgressScreen", "#SMPROG_2", "#SMPROG_3")
  end
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
  -- xipdbg("Adding Param MPIN = " .. SM_MPIN)  
  xal_xms_add_params( SM_xmsConn, "mp", SM_MPIN )
  xal_xms_add_params( SM_xmsConn, "msgType", "TOPUP_MSG" )
  
   xal_xms_add_params( SM_xmsConn, "user", "01696945543" )
   xal_xms_add_params( SM_xmsConn, "pass", SM_MPIN )
   xal_xms_add_params( SM_xmsConn, "partnerId", SM_PHONE )
   xal_xms_add_params( SM_xmsConn, "originalAmount", SM_Amount )
  ret = xal_xms_request(SM_xmsConn, 1)
end

function SM_CB ()
  XMSSCData = xal_xms_get_params (SM_xmsConn, "sc")
  xipdbg("In Lua: perso Status" .. XMSSCData)
  SCDetails = mysplit (XMSSCData,"|")
  xmsSC = SCDetails[1]
  xipdbg("In Lua: perso Status" .. xmsSC)
  
  phoneSender = xal_xms_get_params (SM_xmsConn, "phoneSender")
  originalAmount = xal_xms_get_params (SM_xmsConn, "originalAmount")
  tranId = xal_xms_get_params (SM_xmsConn, "tranId")
  
  txnId = xal_xms_get_params (SM_xmsConn, "txnId")
  xal_xms_deInit(SM_xmsConn)
  SM_xmsConn = 0
  if tonumber (xmsSC)  ==  0 or tonumber (xmsSC)  ==  0100 then
    xipdbg("In Lua: Displaying sendMoneySuccessScreen: SC = " .. xmsSC .. "txnID  " .. txnId)
    DisplayScreenFromRes("sendMoneySuccessScreen", phoneSender, GetCurrencySymbol().." "..SM_Amount, SMM_MerNo, txnId)
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