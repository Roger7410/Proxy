#!/usr/bin/env python

import urllib
import os
import signal

# bootstrap by downloading bilder.py if not found
if not os.path.exists("bilder.py"):
	print "bootstrapping; downloading bilder.py"
	urllib.urlretrieve(
		"https://raw.githubusercontent.com/parrt/bild/master/src/python/bilder.py",
		"bilder.py")

# assumes bilder.py is in current directory
from bilder import *

def compile():
	javac("src", "out")

def launch_proxy():
	global server_pid
	require(compile)
	p = subprocess.Popen("java -cp out cs601.proxy.ProxyServer", shell=True)
	server_pid = p.pid
	print "Server PID ", server_pid

def test(url):
	print "TEST "+url
	rmdir("/tmp/wo")
	rmdir("/tmp/w")
	errcode = wget(url, trgdir="/tmp/wo", level=1)
	errcode = wget(url, trgdir="/tmp/w",  level=1, proxy="http://localhost:8080")
	output = diff("/tmp/wo", "/tmp/w", recursive=True)
	if output is not None and len(output.strip())>0:
		print output
	else:
		print "URL looks the same with and without the proxy"

def tests():
	launch_proxy()
	try:
		test("http://www.cs.usfca.edu/index.html")
		test("http://www.github.com")
	finally:
		print "Killing Server PID ", server_pid
		os.kill(server_pid, signal.SIGABRT)

def all():
	tests()

processargs(globals())

