--[[ XLA:HOME
-- Description: Implements the Home screen for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]

function H_OnLoad()
	xipdbg("In Lua: changexla to DPIN")
	
	-- set the syncup value
	SetSessionValue("SyncUp", "0" )
	ChangeXla ("DPIN")
end

