import socket, json

def print_graph(temp):
   tick_str = ""
   for tick in range(0, temp-40):
      tick_str += "-"
   print "temp: " + str(temp) + "   " + tick_str


UDP_IP = "127.0.0.1"
UDP_PORT = 5911

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((UDP_IP, UDP_PORT))

while True:
   data, addr = sock.recvfrom(1024)
   rec = json.loads(data)
   print_graph(rec['temp'])


