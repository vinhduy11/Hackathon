btresult = 0
printerName = ""
resStr1 = ""
resStr2 = ""
resStr3 = ""
resStr4 = ""
resStr5 = ""
PrintBuffer = 0
Btscanflag=0
PrnLen = 0
CurrDate = 0
SampleReceipt=0
printProgress=0
function OnLoad ()
	DisplayScreenFromRes("printerSetupMenu")
	CurrDate = GetConfigValue("dt")
end

function OnCancel ()
	bt_deinit() 
	bt_power()
	ChangeXla("HomeScreen")
end

function PS_OnStartPrintWiz()
	DisplayScreenFromRes("printerSetupStep1")
end
function PS_OnPrintSampleReceipt()
	SampleReceipt=1
	printerName = GetConfigValue ("printName")
	ret = bt_ispaired (printerName)
	if(ret == 1) then
		resStr1 = GetXlaString("TSTPRNPG1")
		DisplayScreenFromRes("printerProgress", resStr1)
		printProgress=1
		bt_init("BT_OnTstPrintInit")
	else
		DisplayScreenFromRes("printerNotConfigured")
	end
end
function OnWizNext()
	Scanlist= {}
	DisplayScreenFromRes ("MenuScan","Scanning devices  ...",Scanlist[0],Scanlist[1],Scanlist[2],Scanlist[3],Scanlist[4],Scanlist[5],Scanlist[6],Scanlist[7],Scanlist[8],Scanlist[9],Scanlist[10],Scanlist[11],"","")
	printProgress=1
	bt_init ("BT_OnInit")
end

function OnOpsCancel()
	bt_cancel()
	OnCancel()
end

function BT_OnInit (btresult)
	if(btresult == "0") then
	Scancnt=0
	Btscanflag=0
	Scanlist= {}
	bt_scan(12,"ONBTSCAN")
--	DisplayScreenFromRes ("MenuScan","Scanning devices",Scanlist[0],Scanlist[1],Scanlist[2],Scanlist[3],Scanlist[4],Scanlist[5],Scanlist[6],Scanlist[7],Scanlist[8],Scanlist[9],Scanlist[10],Scanlist[11],"","")
	else
		OnCancel()
	end
end
function BT_OnDeviceFound(sncount,sndevice,snaddr)
	Scanlist[Scancnt]=sndevice
	Scancnt=Scancnt+1
	DisplayScreenFromRes ("MenuPage","Scanning devices  ...",Scanlist[0],Scanlist[1],Scanlist[2],Scanlist[3],Scanlist[4],Scanlist[5],Scanlist[6],Scanlist[7],Scanlist[8],Scanlist[9],Scanlist[10],Scanlist[11])
end
function ONBTSCAN(resultbuf,recvdata, lenbuf)
	if(Btscanflag~=1)then
	DisplayScreenFromRes ("MenuPage","Scan Completed",Scanlist[0],Scanlist[1],Scanlist[2],Scanlist[3],Scanlist[4],Scanlist[5],Scanlist[6],Scanlist[7],Scanlist[8],Scanlist[9],Scanlist[10],Scanlist[11])
	end
end
function BT_OnPair (btresult)
	if(btresult == "0") then
		DisplayScreenFromRes("printerSetupSuccess")
	else
		resStr1 = GetXlaString("PRNWIZ")
		resStr2 = GetXlaString("PRNSETUPERR")
		PrinterError (resStr1, resStr2)
	end
end
function OnGenericMenu()
	bt_stop_scan()
	Btscanflag=1
	MenuText,MenuKeyIndex = GetMenuKeyIndex()
	z = xipdbg("MenuKeyIndex ----" .. MenuKeyIndex .. "MenuText = " .. MenuText)
	printerName=MenuText
	SetConfigValue("printName",printerName)
	DisplayScreenFromRes("BTPAIRINPUT","Enter Passkey","","Ok","Cancel")
 end
 function BT_OnInputPair(Pairid)
 DisplayScreenFromRes("printerProgress", "Pairing:"..printerName)
 ret = bt_pair (printerName,Pairid,"BT_OnPair")	
 end
function OnSuccessNext()
	DisplayScreenFromRes("printerSetupTestPrintConfirm")
end

function PS_OnPrintTestPage()
	printerName = GetConfigValue ("printName")
	ret = bt_ispaired (printerName)
	if(ret == 1) then
		resStr1 = GetXlaString("TSTPRNPG")
		DisplayScreenFromRes("printerProgress", resStr1)
		printProgress=1
		bt_init("BT_OnTstPrintInit")
	else
		DisplayScreenFromRes("printerNotConfigured")
	end
end

function BT_OnTstPrintInit (btresult)
	xipdbg("In Lua: btresult".. btresult)
	printerName = GetConfigValue ("printName")
	if(btresult == "0") then
		--All OK Call Pair
		if(SampleReceipt==1)then
			bt_connect(printerName, "BT_OnConnect1")
		else
			bt_connect(printerName, "BT_OnConnect")
		end
	else
		--Error printing try again
		--resStr1 = GetXlaString("PRNWIZ")
		resStr2 = GetXlaString("PRNTESTERR")
		PrinterError (resStr1, resStr2)
	end
end
function BT_OnConnect1 (btresult)
	xipdbg("In Lua: btresult".. btresult)
	if(btresult == "0") then
		xipdbg("In Lua: PrintSamplereceipt")
		InitPrintBuff ()
		dateTime= xal_xms_timeStamp()
		Cdate=string.sub(dateTime,7,8)..":"..string.sub(dateTime,5,6)..":"..string.sub(dateTime,1,4).." "..string.sub(dateTime,9,10)..":"..string.sub(dateTime,11,12)..":"..string.sub(dateTime,13,14)
		PrintFromRes ("printerTestPageItem1", "Nearex Pvt Ltd")
		PrintFromRes ("printerTestPageItem1", "184,1st Floor,BMR Complex,")
		PrintFromRes ("printerTestPageItem1", "Hennur Cross,Hennur Main")
		PrintFromRes ("printerTestPageItem1", "Bengalure-560043")
		resStr1 = GetXlaString("DASH")
		PrintFromRes ("printerTestPageItem", resStr1)
		PrintFromRes ("printerTestPageItem1", "RECEIPT")
		PrintFromRes ("printerTestPageItem", resStr1)
		PrintFromRes ("printerTestPageItem", "Date:"..Cdate)
		PrintFromRes ("printerTestPageItem", resStr1)
		PrintFromRes ("printerTestPageItem", "Consumer   :xxxxxx8918")
		PrintFromRes ("printerTestPageItem", "Txn Amount :$10")
		PrintFromRes ("printerTestPageItem", "TxnId      :MP15.1302.C76")
		PrintFromRes ("printerTestPageItem", resStr1)
		PrintFromRes ("printerTestPageItem", "Thank you for shopping with ")
		PrintFromRes ("printerTestPageItem1", "XIPPOS")
		PrintFromRes ("printerTestPageItem", resStr1)
		PrintFromRes ("printerTestPageItem1", "***Please Visit Again***")
		PrintBuffer,PrnLen = GetPrintBuff()
		resStr1 = GetXlaString("RECEIPT")
		bt_send(PrintBuffer, PrnLen, 50, "BT_OnSend")
	else
		resStr2 = GetXlaString("NOPRINTER1")
		PrinterError (resStr1, resStr2)
	end
end
function BT_OnConnect (btresult)
	xipdbg("In Lua: btresult".. btresult)
	if(btresult == "0") then
		xipdbg("In Lua: printerTestPage")
		InitPrintBuff ()
		PrintFromRes ("printerTestPage", CurrDate, "2.0.3.DEV.pos", "1.0.0.0", printerName)
		resStr1 = GetXlaString("DASH")
		PrintFromRes ("printerTestPageItem", resStr1)
		PrintFromRes ("printerTestPageItem", "End of Test Page")
		PrintFromRes ("printerTestPageItem", resStr1)
		PrintBuffer,PrnLen = GetPrintBuff()
		resStr1 = GetXlaString("PRNWIZ")
		bt_send(PrintBuffer, PrnLen, 50, "BT_OnSend")
	else
		--Error printing try again
		--resStr1 = GetXlaString("PRNWIZ")
		resStr2 = GetXlaString("NOPRINTER1")
		PrinterError (resStr1, resStr2)
	end
end

function BT_OnSend (btresult)
	DeInitPrintBuff ()
	if(btresult == "0") then
		--Printed successfully
		resStr2 = GetXlaString("PRNSUCCMSG1")
		resStr3 = GetXlaString("PRNSUCCMSG2")
		DisplayScreenFromRes("successPage", resStr1, resStr2, resStr3)
		
	else
		--Error printing try again
		resStr2 = GetXlaString("PRNTESTERR")
		PrinterError (resStr1, resStr2)
	end
	printProgress=0
	--bt_disconnect ("BT_OnDisconnect")
end

function PrinterError(title, errormsg)
	resStr3 = GetXlaString("WIZMSG1")
	resStr4 = GetXlaString("WIZMSG2")
	resStr5 = GetXlaString("WIZMSG3")
	DisplayScreenFromRes("failurePage", title, errormsg, resStr3, resStr4, resStr5)
	printProgress=0
end

function BT_OnDisconnect (btresult)
	if(printProgress == 1) then
	resStr2 = GetXlaString("PRNTESTERR")
	else
	resStr2 = GetXlaString("PRNTDCON")
	end	
	PrinterError (resStr1, resStr2)
	
end

function OnOk()
	OnCancel()
end

