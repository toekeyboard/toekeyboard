#http://stackoverflow.com/questions/5060710/format-of-dev-input-event
#https://www.tutorialspoint.com/python/python_multithreading.htm

#!/usr/bin/python

import threading
import struct
import time
from datetime import datetime
#from time import time
import os, sys

#usage: python main.py 21
#$ls -1 /dev/input/event*        //mouse plugged in and out
#infile_path = "/dev/input/event" + (sys.argv[1] if len(sys.argv) > 1 else "0")

infile_path = "/dev/input/event12"      #wired mouse
#infile_path = "/dev/input/event14"     #bluetooth mouse

#debug = 0 #mots
debug = 1 #codes et mots
#debug = 2 #temps, codes et mots

left = 272
right = 273
#counter = 0
down = 1
up = 0
last_time=0


difference_in_time = 0
code_current = ""
text_input = ""



#global difference_in_time
#global text_input

#global this_time
#global last_time

#global code_current
#global key_codes

#global flag_click



# https://en.wikipedia.org/wiki/Morse_code#Letters.2C_numbers.2C_punctuation.2C_prosigns_for_Morse_code_and_non-English_variants
key_codes = {
    "lr" : "a",
    "rlll" : "b",
    "rlrl" : "c",
    "rll" : "d",
    "l" : "e",
    "llrl" : "f",
    "rrl" : "g",
    "llll" : "h",
    "ll" : "i",
    "lrrr" : "j",
    "rlr" : "k",
    "lrll" : "l",
    "rr" : "m",
    "rl" : "n",
    "rrr" : "o",
    "lrrl" : "p",
    "rrlr" : "q",
    "lrl" : "r",
    "lll" : "s",
    "r" : "t",
    "llr" : "u",
    "lllr" : "v",
    "lrr" : "w",
    "rllr" : "x",
    "rlrr" : "y",
    "rrll" : "z",

    "lrrrr" : "1",
    "llrrr" : "2",
    "lllrr" : "3",
    "llllr" : "4",
    "lllll" : "5",

    "rllll" : "6",
    "rrlll" : "7",
    "rrrll" : "8",
    "rrrrl" : "9",
    "rrrrr" : "0",

    "lrlrlr" : ".", #full_stop
    "rlrlrl" : ";", #semicolon

    "rrllrr" : ",", #comma
    "llrrll" : "?", #question_mark

    "rlrlrr" : "!", 
    "lrrlrl" : "@",
    "rllrlr" : "#",

    "lrlrl" : "+",
    "rllllr" : "-",
    "rllrl" : "/",
    "lrrlr" : "*",
    "rlllr" : "=",

    "rlrrl" : "(",
    "rlrrlr" : ")",

    "lrrrrl" : "'",
    "lrlll" : "&",
    "rrrlll" : ":",
    "llrrlr" : "_",
    "lrllrl" : "\"",

    "lllrllr" : "$"
}




class myThread_time (threading.Thread):
    def __init__(self, threadID, name):
        threading.Thread.__init__(self)
        self.threadID = threadID
        self.name = name
	# A flag to notify the thread that it should finish up and exit
    	self.kill_received = False

    def run(self):
    	while not self.kill_received:
	    self.do_something()

    def do_something(self):
	#time.sleep(1)
        #print "Starting " + self.name
        print_time(self.name)
        #print "Exiting " + self.name

def print_time(threadName):
    global last_time

    global code_current
    global text_input
    global last_input
    global key_codes

    code_current = ""
    last_code_current = ""
    text_input = ""
    last_input = ""

    global flag_space
    flag_space = 0


    #long int, long int, unsigned short, unsigned short, unsigned int
    FORMAT = 'llHHI'
    EVENT_SIZE = struct.calcsize(FORMAT)

    #open file in binary mode
    in_file = open(infile_path, "rb")

    event = in_file.read(EVENT_SIZE)


    while True:
	#(tv_sec, tv_usec, type, code, value) = struct.unpack(FORMAT, event)

        #time.sleep(delay)
        #print "%s: %s" % (threadName, time.ctime(time.time()))
 #       print "%s time: %f" % (threadName, time.time())
 #       print "%s difference_in_time: %f" % (threadName, difference_in_time)



	#this_time = float( str(tv_sec) + "." + str(tv_usec) )
	dt= datetime.now()
	#this_sys_time = float( str(dt.second) + "." + str(dt.microsecond) )

	# http://stackoverflow.com/questions/6999726/how-can-i-convert-a-datetime-object-to-milliseconds-since-epoch-unix-time-in-p#11111177
	this_sys_time = float( str(int(dt.strftime("%s"))) + "." + str(dt.microsecond) )
	difference_in_time = this_sys_time - last_time

	if (difference_in_time > 1.0): 
	    if key_codes.has_key(code_current) :
		text_input = text_input + key_codes[code_current]

		last_input = key_codes[code_current]
		last_code_current = code_current
                flag_space = 1
	    code_current = ""

	if( (difference_in_time > 4.0) and (flag_space == 1) ): 
            #mettre espace, apres 4 secondes
            text_input = text_input + " "
            flag_space = 0

        if (debug == 2):
            print ("TIME - last time %f" % last_time)
            print ("TIME - this sys time %f" % this_sys_time)
            print ("TIME - difference in time %f" % difference_in_time)

            print "TIME - last code current: " + last_code_current
            print "TIME - last input: " + last_input 
            print "---- TIME - text: " + text_input 
            print "TIME - code_current: " +  code_current
            print "\n"
        elif (debug == 1):    
	    print code_current
	    print "----" + text_input 
        else:  #(debug == 0)   
	    print text_input 



        time.sleep(1)



class myThread_event (threading.Thread):
    def __init__(self, threadID, name):
        threading.Thread.__init__(self)
        self.threadID = threadID
        self.name = name
	# A flag to notify the thread that it should finish up and exit
    	self.kill_received = False

    def run(self):
    	while not self.kill_received:
            self.do_something()

    def do_something(self):
	#time.sleep(1)
        #print "Starting " + self.name
        #print_event(self.name, 5)
        print_event(self.name)
        #print "Exiting " + self.name


#def print_event(threadName, counter):
def print_event(threadName):
    global last_time

    global code_current
    global text_input
    global key_codes
    global flag_click

    code_current = ""
    text_input = ""

    last_time = 0
    flag_click = 0
    flag_code = 0

    #long int, long int, unsigned short, unsigned short, unsigned int
    FORMAT = 'llHHI'
    EVENT_SIZE = struct.calcsize(FORMAT)

    #open file in binary mode
    in_file = open(infile_path, "rb")

	
    #while 1:
    while True:
    #while event:
    	event = in_file.read(EVENT_SIZE)

	(tv_sec, tv_usec, type, code, value) = struct.unpack(FORMAT, event)

#
#	this_time = float( str(tv_sec) + "." + str(tv_usec) )
#	print ("this time %f" % this_time)

#	difference_in_time = this_time - last_time
#	if difference_in_time > 1.0 and code_current != "" and print_flag == 0 :
#	    print key_codes[code_current]
#	    print_flag = 1


	if type != 0 or code != 0 or value != 0:
	    if type != 2 and type !=4:

		#this_time = float(tv_sec) + float(tv_usec)
		#this_time = float(str(tv_sec) + "." + str(tv_usec))
		#print ("seconds %d" % float(tv_sec))
		#print ("useconds %d" % float(tv_usec))


		this_time = float( str(tv_sec) + "." + str(tv_usec) )

		if (code == left): 
		    if (value == down):
			#print "EVENT - left down"
			flag_click = 1
                    elif (value == up):
			#print "EVENT - left up"
			if (flag_click == 1):
			    code_l_or_r = "l"
			    flag_click = 0
			    flag_code = 1

		elif (code == right): 
		    if (value == down):
			flag_click = 1
		    elif (value == up):
			#print "EVENT - right up"
			if (flag_click == 1):
    		    	    code_l_or_r = "r"
			    flag_click = 0
			    flag_code = 1
			

		difference_in_time = this_time - last_time

		if (difference_in_time > 1.0): 
		#if difference_in_time > 1.0 and code_current != "": 
		    #voir si code_current existe?
		    #http://www.tutorialspoint.com/python/python_dictionary.htm
			#dict.has_key(key)
			#Returns true if key in dictionary dict, false otherwise
		    if key_codes.has_key(code_current) :
			text_input = text_input + key_codes[code_current]
		    #counter = 0
		    code_current = ""
		#elif difference_in_time < 0.5:
		else:
    		    if (flag_code == 1):
			code_current = code_current + code_l_or_r
			flag_code = 0

                if (debug == 2):
                    print "++++EVENT - this time %f" % this_time
                    print "++++++++++++++++++++++++++EVENT - code %d" % code
                    print "++++EVENT - difference in time %f" % difference_in_time
                    print "++++EVENT - text: " + text_input 
                    print "++++EVENT - code_current: " +  code_current
                    print "\n"

		last_time = this_time




		#print("Event type %u, code %u, value %u at %d.%d" % \
		#    (type, code, value, tv_sec, tv_usec))


    #    else:
	    # Events with code, type and value == 0 are "separator" events
    #        print("===========================================")

	#event = in_file.read(EVENT_SIZE)

    in_file.close()



def main(args):

    threads = []

    # Create new threads
    thread1 = myThread_time(1, "Thread-time")
    threads.append(thread1)
    thread1.daemon = True
    thread1.start()

    thread2 = myThread_event(2, "Thread-event")
    threads.append(thread2)
    thread2.daemon = True
    thread2.start()


    while len(threads) > 0:
        try:
            # Join all threads using a timeout so it doesn't block
            # Filter out threads which have been joined or are None
            threads = [t.join(1) for t in threads if t is not None and t.isAlive()]
        except KeyboardInterrupt:
            print "Ctrl-c received! Sending kill to threads..."
            for t in threads:
                t.kill_received = True



if __name__ == '__main__':
    main(sys.argv)
    while True:
	time.sleep(1)



