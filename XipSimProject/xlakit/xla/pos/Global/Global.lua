--[[ XLA:Global
-- Description: Holds all the global text and screens for the POS XLA bundle
-- Date: 1 Aug 2016
-- Author: Nearex
-- History: Initial version created

--]]
local t={}
local array={} 
Var1 = 0
Var2 = 0
index1 = 0
index2 = 0
index3 = 0
index4 = 0
index5 = 0

function G_OnLoad ()
	xipdbg("Global Lua OnLoad")
end

function mysplit(inputstr, sep)
	if sep == nil then
			sep = "%s"
	end
	i=1
	xipdbg(" Global: Split: Input String val = " .. inputstr)		
	for str in string.gmatch(inputstr, "([^"..sep.."]+)") do
		t[i] = str
		xipdbg(" Global: Individual Split String val = " .. t[i])		
		i = i + 1
	end
	
	Var1 = t[1]
	Var2 = t[2]
	--xipdbg(" Global: val1 = " .. Var1 .. " val2 = " .. Var2)		
end

function GetMultipleLines (buf)
	xipdbg("DisplayMultipleLines: Global:Received : "..buf)
	count = 1	
	result = ""
    for word in buf:gmatch("%w+") do
        if(string.len(result)+string.len(word)+1 > 20) then
            xipdbg("DisplayMultipleLines: Global:chunk : "..result)
			array[count]=result
			count = count + 1
            result = word
        elseif(string.len(result)>0) then
            result = result.." "..word
        else
            result = word
        end
    end
    xipdbg("DisplayMultipleLines: Global:chunk : "..result)
	array[count]=result
	count = count + 1
	while(count <= 5) do
		array[count]=" "
		count = count + 1
	end
	
	index1 = array[1]
	index2 = array[2]
	index3 = array[3]
	index4 = array[4]
	index5 = array[5]
end
