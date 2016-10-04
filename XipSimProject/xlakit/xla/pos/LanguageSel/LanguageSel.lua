--[[ XLA: LanguageSel
-- Description: Implements the Language Selection feature for the POS XLA bundle. 
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created
--]]


LS_LangId = nil
--language text
--langText = {"ENGLISH", "SPANISH"}

function LS_OnLoad ()
	xipdbg("Calling DisplayScreenFromRes")
	DispScreen("preferencesLanguageMenuScreen")
end

function LS_OnMenuBtn1 ()
	xipdbg("In Lua: LS_OnMenuBtn1")
	LS_LangId = "EN"
	DisplayScreenFromRes("languageSelectionScreen", "#LS1")
end

function LS_OnMenuBtn2 ()
	xipdbg("In Lua: LS_OnMenuBtn2")
	LS_LangId = "SP"
	DisplayScreenFromRes("languageSelectionScreen", "#LS2" )
end

function LS_OnOk ()
	xipdbg("In Lua: LS_OnOk")
	-- update prefernce language Id into flash 
	SetConfigValue("LANG", LS_LangId )
	-- TODO set prefernce language strings 
	LS_goHome ()
end

function LS_goHome ()
	ChangeXla("HomeScreen")
end

function DispScreen (scrName)
	xipdbg("DispScreen: Calling DisplayScreenFromRes for screen   " .. scrName )
	DisplayScreenFromRes(scrName)
end
