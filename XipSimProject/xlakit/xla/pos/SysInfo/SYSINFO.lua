--[[ XLA: SysInfo
-- Description: Implements the System Info feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

function SI_OnLoad ()
	xipdbg("Calling DisplayScreenFromRes")
	xid = GetDeviceXID()
	msisdn = GetDeviceMSISDN()
	activeMnc = GetActiveMnc()
	homeMnc = GetIMSINumber()
	network = activeMnc .. "/" .. string.sub(homeMnc, 1, 5)
	signal = xal_xms_getSignalStn()
	batt = xal_xms_getBattStatus()
	gprs = GetGPRSFlag()
	buildDate = GetBuildDate()
	version = GetBuildVersion()
	hwVer = xip_release_hw_ver()
	relVerNo = xip_release_verno()
	hw = hwVer .. "/" .. relVerNo
	authExp = GetKeyExpiryTime()
	DisplayScreenFromRes("systemInfoScreen", xid, msisdn, network, signal, batt, 
						  gprs, buildDate, version, hw, authExp )
end

function SI_goHome ()
	ChangeXla ("PreferencesMenu")
end
