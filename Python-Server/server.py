'''IMPORT SETUP'''
from socket     import *
from _thread    import *
from threading  import * 
from random     import *
from time       import *
from json       import *
from logging    import *
'''SERVER CLASS'''
class Server(object):
    def __init__(cls):
        #GLOBAL VAR 
        cls.SERVER_TIME = time()
        cls.CLIENT_TIME = []
        cls.JOB_LOCK = Lock()
        cls.JOBS = []
        cls.is_active = True

    def clear(cls):
        cls.SERVER_TIME = time.time()
        cls.CLIENT_TIME = []
        cls.JOB_LOCK = Lock()
        cls.JOBS = []

    def callout_client_time(cls, host:str, port:str):
        debug(f'CALLED SERVER.CALLOUT_CLIENT_TIME')
        current_socket = socket(AF_INET, SOCK_STREAM)
        current_socket.connect((host, port))
        debug(f'SERVER.CALLOUT_CLIENT_TIME SERVER CONNECTED TO HOST {host} | PORT {port}')
        current_socket.send("REQUEST_TIME\n".encode())
        received_data = current_socket.recv(1024).decode()
        current_socket.close()
        debug(f'RECEIVED DATA SERVER.CALLOUT_CLIENT_TIME {received_data}')
        cls.CLIENT_TIME.append(float(received_data))
    
    def callout_sync(cls, host:str, port:str) -> bool:
        debug(f'CALLED SERVER.CALLOUT_SYNC')
        current_socket = socket(AF_INET, SOCK_STREAM)
        debug(f'SERVER.CALLOUT_SYNC SERVER CONNECTED TO HOST {host} | PORT {port}')
        current_socket.connect((host, port))
        current_socket.send((str(cls.SERVER_TIME ) + '\n').encode())
        debug(f'SERVER.CALLOUT_SYNC SEND {str(cls.SERVER_TIME)}')
        current_socket.close()

    def receive_client_time(cls, client_socket: socket) -> str:
        return client_socket.recv(1024).decode()
    
    def factory_server_jobs(cls, fun, args: tuple):
        start_new_thread(fun, args)

    def factory_jobs(cls, argument: any, method) ->  bool:
        current_jobs = []
        cls.CLIENT_TIME.clear()
        cls.CLIENT_TIME.append(cls.SERVER_TIME)
            
        for job in cls.JOBS:
            current_jobs.append(Thread(target = method, \
                                args = (job[0], job[1])))
        
        for job in current_jobs:
            job.start()

        for job in current_jobs:
            job.join()

        return False if len(current_jobs) > 0 else True

    def process_data(cls, client_socket: socket) -> str:
        debug(f'CALLED SERVER.PROCESS_DATA')
        hasError: bool = False
        unprocessed_data = cls.receive_client_time(client_socket)
        debug(f'SERVER.PROCESS_DATA CURRENT UNCLEARED RECEIVED DATA { unprocessed_data }')
        cleared_data = unprocessed_data.replace('\n', '').split('|')
        debug(f'SERVER.PROCESS_DATA CURRENT RECEIVED DATA {cleared_data}')
        cls.JOB_LOCK.release()
        client_socket.close()
        
        time_gap = True if (float(cls.SERVER_TIME) \
                    - float(cleared_data[1]) < -1 \
                    or float(cls.SERVER_TIME) \
                    + float(cleared_data[1]) > 1) \
                    else False
        debug(f'SERVER.PROCESS_DATA GAP {time_gap}')
        if time_gap:
            hasError = cls.factory_jobs(any, cls.callout_client_time)
            warn(f'SERVER.PROCESS_DATA HAS ERROR {hasError}')
        if not hasError:
            cls.SERVER_TIME = sum(cls.CLIENT_TIME) / len(cls.CLIENT_TIME)
            debug(f'SERVER.PROCESS_DATA GAP {cls.SERVER_TIME}')
            cls.factory_jobs(cls.SERVER_TIME, cls.callout_sync)

    def clock(cls):
        debug(f'CALLING START SERVER.CLOCK')
        clock_enable : bool = True
        clock_step : float = 0
        clock_gap : float = 0.2

        while clock_enable:
            #debug(f'CURRENT SERVER CLOCK {cls.SERVER_TIME}')
            sleep(clock_gap)
            cls.SERVER_TIME = cls.SERVER_TIME + clock_gap  if clock_step > 5 \
                                            and randint(0, 1) \
                                            else cls.SERVER_TIME + \
                                                clock_step + clock_gap
            clock_step = 0  if clock_step > 5 \
                            and randint(0, 1) \
                            else clock_step + clock_gap

    def start(cls):
        debug(f'CALLING START SERVER.START')
        cls.factory_server_jobs(cls.clock, ())
        host = "127.0.0.1"
        port = 8000

        current_socket = socket(AF_INET, SOCK_STREAM)
        current_socket.bind((host, port))
        current_socket.listen(5) 
        debug(f'INITIAL SETUP')
        debug(f'CURRENT SERVER STATUS {cls.is_active}')
        debug(f'CURRENT HOST {host} | CURRENT PORT {port}')
        debug(f'CURRENT SERVER SOCKET {current_socket}')
        while True:
            debug(f'START SERVER LISTENER')
            client_socket, addr = current_socket.accept()
            debug(f'SERVER SOCKET LOOP CLIENT CONNECTED | SOCKET {client_socket}')
            debug(f'SERVER CURRENT LISTENING TO {addr[0]}:{addr[1]}')
            cls.JOB_LOCK.acquire()
            cls.factory_server_jobs(cls.process_data, (client_socket, ))
            debug(f'CREATED A NEW JOB')
        
        current_socket.close()
        debug(f'STOP SERVER')
        

if __name__ == "__main__":
    '''LOGS SETUP'''
    basicConfig(filename='server.log', filemode='w', encoding='utf-8', level=DEBUG)
    '''SERVER SETUP'''
    server = Server()
    server.JOBS.append(['127.0.0.1',8001])
    server.JOBS.append(['127.0.0.1',8002])
    debug(f'SERVER IS UPPON TO START {ctime()}')
    server.start()
        
