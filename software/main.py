#http://stackoverflow.com/questions/5060710/format-of-dev-input-event
#https://www.tutorialspoint.com/python/python_multithreading.htm

#!/usr/bin/python

import threading
import struct
import time
#from time import time
import os, sys




left = 272
right = 273
counter = 0
down = 1
up = 0
last_time=0


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
    "rr" : "o",
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

    #filler so not undefined
    "" : "",
    "llrr" : "[dah]",   
    "lrlr" : "[dah]",
    "rrrl" : "[dit]",
    "rrrr" : "[dah]",

    "lrlrlr" : ".", #full_stop
    "rrllrr" : ",", #comma
    "rrllrr" : "?", #question_mark
}


key_a="lr"
key_b="rlll"
key_c="rlrl"
key_d="rll"
key_e="l"
key_f="llrl"
key_g="rrl"
key_h="llll"
key_i="ll"
key_j="lrrr"
key_k="rlr"
key_l="lrll"
key_m="rr"
key_n="rl"
key_o="rr"
key_p="lrrl"
key_q="rrlr"
key_r="lrl"
key_s="lll"
key_t="r"
key_u="llr"
key_v="lllr"
key_w="lrr"
key_x="rllr"
key_y="rlrr"
key_z="rrll"
key_1="lrrrr"
key_2="llrrr"
key_3="lllrr"
key_4="llllr"
key_5="lllll"
key_6="rllll"
key_7="rrlll"
key_8="rrrll"
key_9="rrrrl"
key_0="rrrrr"

key_full_stop="lrlrlr"
key_comma="rrllrr"
key_question_mark="rrllrr"




print_flag = 0
difference_in_time = 0
code_current = ""
text_input = ""







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
    #code_current=""
    while True:
        time.sleep(1)
        #time.sleep(delay)
        #print "%s: %s" % (threadName, time.ctime(time.time()))
        print "%s time: %f" % (threadName, time.time())
        print "%s difference_in_time: %f" % (threadName, difference_in_time)
	

	#if difference_in_time > 1.0: 
	#    code_current = ""

	print "thread-time: " + text_input
	#print "thread-time: " + key_codes[code_current]
	#print float(time())




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
        print_event(self.name, 5)
        #print "Exiting " + self.name


def print_event(threadName, counter):
    global difference_in_time
    global text_input

    global code_current
    global key_codes
    code_current = ""
    text_input = ""

    #usage: python main.py 21
    #infile_path = "/dev/input/event" + (sys.argv[1] if len(sys.argv) > 1 else "0")

    infile_path = "/dev/input/event17"
    #infile_path = "/dev/input/event21"

    #long int, long int, unsigned short, unsigned short, unsigned int
    FORMAT = 'llHHI'
    EVENT_SIZE = struct.calcsize(FORMAT)

    #open file in binary mode
    in_file = open(infile_path, "rb")

    event = in_file.read(EVENT_SIZE)
	
    last_time=0

    #while 1:
    while event:

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
    #            counter = counter + 1
    #            print counter

		#this_time = float(tv_sec) + float(tv_usec)
		#this_time = float(str(tv_sec) + "." + str(tv_usec))
		this_time = float( str(tv_sec) + "." + str(tv_usec) )
		#print ("seconds %d" % float(tv_sec))
		#print ("useconds %d" % float(tv_usec))
		print ("this time %f" % this_time)
		#print ("this time %d" % this_time)


		if code == left: 
		    if value == down: print "left down"
		    if value == up: print "left up"
		    #print str(tv_sec) + "." + str(tv_usec)
		    code_l_or_r = "l"
    #                print "l"
		if code == right: 
		    if value == down: print "right down"
		    if value == up: print "right up"
		    code_l_or_r = "r"
    #                print "r"


		difference_in_time = this_time - last_time
		print ("difference in time %f" % difference_in_time)
		if difference_in_time > 1.0: 
		#if difference_in_time > 1.0 and code_current != "": 
		    text_input = text_input + key_codes[code_current]
		    print "thread-event: " + text_input 
		    counter = 0
		    code_current = ""
		elif difference_in_time < 0.5:
		    code_current = code_current + code_l_or_r

		print code_current

		print_flag = 0
		last_time = this_time




		#print("Event type %u, code %u, value %u at %d.%d" % \
		#    (type, code, value, tv_sec, tv_usec))


    #    else:
	    # Events with code, type and value == 0 are "separator" events
    #        print("===========================================")

	event = in_file.read(EVENT_SIZE)

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



