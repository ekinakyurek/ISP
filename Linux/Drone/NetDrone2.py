import os
from wifi import Cell,Scheme
from wireless import Wireless

ssids = [cell.ssid for cell in Cell.all('wlan0')]

connectedClient = False
connectedServer = False
foundClient = False
foundServer = False

clientSSID = "Field"
clientPass = "12345678"
serverSSID = "Base"
serverPass = "12345678"

clientPath = 'src/'

#go to the client to get requests
print "Scanning network area to connect Client to get requests"
while(not foundClient):
    for i in range (0,len(ssids)):
        if (ssids[i] == clientSSID):
            print("In the WiFi field of " + clientSSID)
            foundClient = True
            break
    ssids = [cell.ssid for cell in Cell.all('wlan0')]

print "Connecting to " + clientSSID

wireless = Wireless()

while(not connectedClient):
    if wireless.connect(ssid=clientSSID, password=clientPass):
        print "Connected to " + clientSSID
        connectedClient = True
    else:
        print "Connection error"

if (connectedClient):
        os.chdir(clientPath);
        os.system("java DroneAsServer");




#go to the server
connectedClient = False
foundClient = False

print "Scanning network area to connect Server (Web)"
while(not foundServer):
    for i in range (0,len(ssids)):
        if (ssids[i] == serverSSID):
            print("In the WiFi field of " + serverSSID)
            foundServer = True
            break
    ssids = [cell.ssid for cell in Cell.all('wlan0')]

print "Connecting to " + serverSSID
wireless = Wireless()
while(not connectedServer):
    if wireless.connect(ssid=serverSSID, password=serverPass):
        print "Connected to " + serverSSID
        connectedServer = True
    else:
        print "Connection error"

if (connectedServer):
    os.system("java DroneForServer");

connectedServer = False
foundServer = False

#go to the client to give back requests
print "Scanning network area to connect Client (push back requests)"
while(not foundClient):
    for i in range (0,len(ssids)):
        if (ssids[i] == clientSSID):
            print("In the WiFi field of " + clientSSID)
            foundClient = True
            break
    ssids = [cell.ssid for cell in Cell.all('wlan0')]

print "Connecting to " + clientSSID
wireless = Wireless()
while(not connectedClient):
	if wireless.connect(ssid=clientSSID, password=clientPass):
		print "Connected to " + clientSSID
		connectedClient = True
	else:
		print "Connection error"


if (connectedClient):
	os.system("java DroneAsServer");

print "Connecting to " + serverSSID
wireless = Wireless()
while(not connectedServer):
	if wireless.connect(ssid=serverSSID, password=serverPass):
		print "Connected to " + serverSSID
		connectedServer = True
	else:
		print "Connection error"

print "Successfully completed."

