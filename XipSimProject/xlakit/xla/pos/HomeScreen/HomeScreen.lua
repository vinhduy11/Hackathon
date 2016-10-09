--[[ XLA:HomeScreens
-- Description: Implements the Home screens for the POS XLA bundle. 
-- Shows HomeScreen1,HomeScreen2 and HomeScreen3, with a LT-RT navigation between them
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

HS_Screen = nil

function HS_OnLoad ()
	HS_Screen = GetConfigValue("profType")
	xipdbg("In Lua: profile Type".. HS_Screen )
	DisplayScreenFromRes("HomeScreen1")
	if( HS_Screen == -1 ) then HS_Screen = "0" end 
	HS_Screen = 2
	if ( tonumber( HS_Screen ) == 0) then
		curSym = GetSessionValue ("CURR")
		DisplayScreenFromRes("HomeScreen1", curSym.." 10.00")
	elseif ( tonumber( HS_Screen ) == 1) then
		DisplayScreenFromRes("HomeScreen2")
	elseif ( tonumber( HS_Screen ) == 2) then
		DisplayScreenFromRes("HomeScreen3")
	end
end

function HS_OnlineRcv ()
	SetSessionValue ("rcvamount", "-10")
	ChangeXla("ReceiveOnline")
end

function HS_OnlineRcvFixed ()
	SetSessionValue ("rcvamount", "10.00")
	ChangeXla("ReceiveOnline")
end

function HS_OfflineRcv ()
	ChangeXla("ReceiveOffline")
end

function HS_OnTopup ()
	ChangeXla("TopUp")
end

function HS_OnPerso ()
	ChangeXla ("MenuHeoDat")
end

function HS_OnCBAL ()
	ChangeXla("CustBalance")
end

function HS_OnBal ()
	ChangeXla ("BalanceEnquiry")
end

function HS_ShowMLoad ()
  ChangeXla("MenuMload")
end

function HS_OnRight()
	if ( tonumber( HS_Screen ) == 0) then
		DisplayScreenFromRes("HomeScreen2")
		SetConfigValue("profType", "1" )
		HS_Screen = "1"
	elseif ( tonumber( HS_Screen ) == 1) then
		DisplayScreenFromRes("HomeScreen3")
		SetConfigValue("profType", "3" )
		HS_Screen = "3"
	elseif ( tonumber( HS_Screen ) == 2) then
		curSym = GetSessionValue ("CURR")
		DisplayScreenFromRes("HomeScreen1")
		SetConfigValue("profType", "0" )
		HS_Screen = "0"
  elseif ( tonumber( HS_Screen ) == 3) then
    DisplayScreenFromRes("HomeScreen4")
    SetConfigValue("profType", "2" )
    HS_Screen = "2"
	end
end

function HS_OnLeft()
	if ( tonumber( HS_Screen ) == 0) then
		DisplayScreenFromRes("HomeScreen3")
		SetConfigValue("profType", "2" )
		HS_Screen = "2"
	elseif ( tonumber( HS_Screen ) == 1) then
		curSym = GetSessionValue ("CURR")
		DisplayScreenFromRes("HomeScreen1")
		SetConfigValue("profType", "3" )
		HS_Screen = "3"
	elseif ( tonumber( HS_Screen ) == 2) then
		DisplayScreenFromRes("HomeScreen2")
		SetConfigValue("profType", "1" )
		HS_Screen = "1"
  elseif ( tonumber( HS_Screen ) == 3) then
    DisplayScreenFromRes("HomeScreen4")
    SetConfigValue("profType", "0" )
    HS_Screen = "0"
	end
end

