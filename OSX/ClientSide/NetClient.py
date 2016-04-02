import os
from wifi import Cell,Scheme
from wireless import Wireless
import time

ssids = [cell.ssid for cell in Cell.all('wlan0')]
foundwifi = False #set it to false when need to download files from web
connectedwifi = False #set it to false when need to download files from web
networkname = "Field"
passnetwork = "12345678"

clientPath = 'src/'

print "Scanning network area .."
while(not foundwifi):
	for i in range (0,len(ssids)):
		if (ssids[i] == networkname):
			print("In the WiFi field of " + networkname)
			foundwifi = True
			break
	ssids = [cell.ssid for cell in Cell.all('wlan0')]

print "Connecting to " + networkname
wireless = Wireless()
while(not connectedwifi):
	if wireless.connect(ssid=networkname, password=passnetwork):
		print "Connected to " + networkname
		connectedwifi = True
	else:
		print "Connection error"

if (connectedwifi):
	os.chdir(clientPath);
	os.system("javac Client.java");
	os.system("java Client");

for x in range(0,5):
	time.sleep(1)
	print "sleeping for " + str(x+1) + " seconds"
	
if (connectedwifi):
	os.chdir(clientPath);
	#os.system("javac Client2.java");
	os.system("java Client");

