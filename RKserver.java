package RKserver;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Date;



import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class RKserver {
	HashMap<String, DataOutputStream> clients;
	private ServerSocket ServerSocket = null;
	//String[] IDlist = {"ABC123"};
	static List<String> IDList = RKdatabase.ids;
	String coordinates;
	int RKreceivingStat = 0;
	byte[] videodata;
	
	static String userid = "ABCtest";
	
	
	
	public static void main(String[] args) {
		new RKserver().start();
	}
	
	public RKserver() {
		clients = new HashMap<String, DataOutputStream>();
		Collections.synchronizedMap(clients);
		
		
	}
	
	private void start() {
		int port = 8080;
		Socket socket = null;		
		
		try {
			ServerSocket = new ServerSocket(port);
			System.out.println("server start");
			
			//여기부터 내가 수정 
			
			RKdatabase.calldb();
			
			//여기까지 디비 연결 
		
			while(true) {
				socket = ServerSocket.accept();
				InetAddress ip = socket.getInetAddress();
				System.out.println(ip + " connected");
				new MultiThread(socket).start();
	
			}		
			
			
		}catch(IOException e) {
			System.out.println(e);
		}
	}
	
	class MultiThread extends Thread{
		
		Socket socket = null;		
		String key = null;
		String msg = null;
		
		DataInputStream input;
		DataOutputStream output;
		
		public MultiThread(Socket socket) {
			this.socket = socket;
			
			try {
				input = new DataInputStream(socket.getInputStream());
				output = new DataOutputStream(socket.getOutputStream());
			}catch(IOException e) {
			}
		}
		
		public void run() {
			try {
				
				key = input.readUTF();
				System.out.println("KEY: " + key);
				clients.put(key,  output);
				
				//key 값을 userid에 넣어줌 
				//userid = key;
				//System.out.println("userid:" + userid);
				//여기까지 
				
				while(input != null) {
					try {					
						
						//RidersKeeper
						if (key.equals(IDList.get(0))||key.equals(IDList.get(1))||key.equals(IDList.get(2))) {		
							//이 부분을db값이랑 비교 하게 수정했다. 
							
							//userid랑 좌표 값 db에 넣어주
							RKdatabase.insert(key, coordinates);	
							System.out.println("KEY: " + key);
							//System.out.println("userid:" + userid);
							System.out.println("userid check");
							//여기까지
							
							
							//gps coordinates
							if (RKreceivingStat == 0) {
								coordinates = input.readUTF();
								System.out.println("#####coordinates: " + coordinates);					
							}									
						}
						
						//User APP - MainActivity
						else  {
							String command = input.readUTF();
							
							//check ID
							if (command.equals("checkID")) {
								System.out.println("#####checkID");
								
								String id = input.readUTF();
								boolean b = false;
								for(String s : IDList) {	//user에서 입력한 아이디가 IDList  중 하나인지 체크, 
									if (s.equals(id)) {
										sendMsg(key, "idResult");
										sendMsg(key, "valid");
										sendMsg(key, id);
										b = true;
										break;
									}
								}
								if (b == false){
									sendMsg(key, "idResult");
									sendMsg(key, "invalid");
									sendMsg(key, id);
								}
							}
							//send gps coordinates
							else if (command.equals("gps")) {
								System.out.println("#####gps");
								
								sendMsg(key, "gpsData");
								sendMsg(key, coordinates); 
							}
							//alarm on
							else if (command.equals("alarm")) {
								System.out.println("#####alarm");
								
								String id = input.readUTF();
								sendMsg(id, "alarm");
							}
							//alarm off
							else if (command.equals("alarmOFF")) {
								System.out.println("#####alarmOFF");
								
								String id = input.readUTF();
								sendMsg(id, "alarmOFF");
							}
							//cameraActivity - camera
							else if (command.equals("camera")) {
								System.out.println("#####camera");
								
								RKreceivingStat = 1;								
								String id = input.readUTF();
								
								//////////////////////////////video receiving socket Thread			
								new Thread() {
									public void run() {
										int port = 11111;								
										try {
											ServerSocket sock = new ServerSocket(port);
											Socket socket = sock.accept();
											System.out.println("camera socket connected");
										
											InputStream is = socket.getInputStream();
											final ObjectInputStream ois = new ObjectInputStream(is);
								       								        
											videodata = (byte[])ois.readObject();											
											
									        //ByteArray to video
									        FileOutputStream out = new FileOutputStream("video.mp4");
									        out.write(videodata);
									        out.close();
									        
									        System.out.println("data size : " + videodata.length);
											sendMsg(key, "location");	//여기 
											RKreceivingStat = 0;
											
											//Send to User APP
											new Thread(new Runnable() {
									            @Override
									            public void run() { // TODO Auto-generated method stub
									                try {
									                    Socket sock = new Socket("172.30.1.6", 12345);
									                    OutputStream os = sock.getOutputStream();
									                    ObjectOutputStream oos = new ObjectOutputStream(os);

									                    oos.writeObject(videodata);
									                    oos.close();
									                    os.close();
									                    sock.close();

									                } catch (UnknownHostException e) {
									                    // TODO Auto-generated catch block
									                    e.printStackTrace();
									                } catch (IOException e) {
									                    // TODO Auto-generated catch block
									                    e.printStackTrace();
									                }
									            }
									        }).start();
									        
									        ois.close();
									        is.close();
									        socket.close();
									        sock.close();
										}
										catch(IOException | ClassNotFoundException e) {}
									}
								}.start();

								sendMsg(id, "camera");								
							
							}
							//request GPS data again
							else if (command.equals("location")){
								String id = input.readUTF();
								System.out.println("#####location");
								System.out.println(id);
								
								sendMsg(id, "location");
							}
							else {}
						}
			
					}catch (IOException e) {
						sendMsg(key, "ERROR");
						break;
					}
				}
			}catch (IOException e) {
				System.out.println(e);
			}
		}
		
		
		private void sendMsg(String key, String msg) {
			try {
				OutputStream dos = clients.get(key);
				DataOutputStream output = new DataOutputStream(dos);
				output.writeUTF(msg);
			}catch(IOException e) {
				System.out.println(e);
			}
		}

	}
	

}
