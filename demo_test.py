import socket

def send_message(temp):
   MESSAGE = '{\n  "temp": '+str(temp)+'\n}'
   sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
   sock.sendto(MESSAGE, (UDP_IP, UDP_PORT))

UDP_IP = "127.0.0.1"
UDP_PORT = 5911

for x in range(40, 150):
  send_message(x)

