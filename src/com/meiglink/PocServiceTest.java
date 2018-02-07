package com.meiglink;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

public class PocServiceTest {
	boolean started = false;
	ServerSocket ss = null;
	List<Client> clients = new ArrayList<Client>();
	private long timeOffset = 0;

	public static void main(String[] args) {
		new PocServiceTest().start();
	}

	// localtime - NTPservicetime
	public void getTimeOffset() {
		long offsetCount = 0;
		//String timeServerUrl = "104.225.159.46";
		String timeServerUrl = "it158.xicp.net";
		//String timeServerUrl = "192.168.1.99";
		System.out.println("server:" + timeServerUrl);
		for (int i = 0; i < 10; i++) {
			try {
				NTPUDPClient timeClient = new NTPUDPClient();
				InetAddress timeServerAddress = InetAddress
						.getByName(timeServerUrl);
				TimeInfo timeInfo = timeClient.getTime(timeServerAddress);
				TimeStamp timeStamp = timeInfo.getMessage()
						.getTransmitTimeStamp();
				Date date = timeStamp.getDate();
				Date localdate = new Date();
				offsetCount += localdate.getTime() - date.getTime();
				// System.out.println("local time:" + localdate.getTime());
				// System.out.println(date.getTime());
				// System.out.println("offset:"+(localdate.getTime()-date.getTime()));
				// DateFormat dateFormat = new
				// SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
				// System.out.println(dateFormat.format(date));
				// System.out.println(dateFormat.format(localdate));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		timeOffset = offsetCount / 10;
		if(timeOffset == 0) {
			System.out.println("don't get timeoffset");
		}
		System.out.println("timeOffset:"+timeOffset);
	}

	public void start() {
		getTimeOffset();
		try {
			ss = new ServerSocket(8888);
			started = true;
			System.out.println("port is opened, use 8888...");
		} catch (BindException e) {
			System.out.println("port is using...");
			System.out
					.println("please close some program and retart PocService!");
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			while (started) {
				System.out.println("wait client");
				Socket s = ss.accept();
				System.out.println("some one connect");
				Client c = new Client(s);
				System.out.println("a client connected!");
				new Thread(c).start();
				clients.add(c);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				System.out.println("socket close");
				ss.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	class Client implements Runnable {

		private Socket s;
		private DataInputStream dis = null;
		private DataOutputStream dos = null;
		private boolean bConnected = false;

		public Client(Socket s) {
			this.s = s;
			try {
				dis = new DataInputStream(s.getInputStream());
				dos = new DataOutputStream(s.getOutputStream());
				bConnected = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void send(String str) {
			try {
				dos.writeChar('S');
				dos.flush();
				dos.writeUTF(str);
				dos.flush();
			} catch (IOException e) {
				clients.remove(this);
				System.out.println("client exit! remove this from List!");
			}
		}
		
		public void send(long sendStartTime, long sendEndTime, String str) {
			try{
				System.out.println("start send...");
				long recvStartTime = new Date().getTime() - timeOffset;
				dos.writeChar('T');
				dos.flush();
				dos.writeLong(sendStartTime);
				dos.flush();
				dos.writeLong(sendEndTime);
				dos.flush();
				dos.writeLong(recvStartTime);
				dos.flush();
				dos.writeUTF(str);
				dos.flush();
				System.out.println("end send...");
			} catch (IOException e) {
				clients.remove(this);
				System.out.println("client exit! remove this form List!");
			}
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				while (bConnected) {
					char event = dis.readChar();
					if (event == 'H') { // ������
						String str = dis.readUTF();
						System.out.println("------heart beat");
					} else if (event == 'T'){ // ��ʱ���
						long sendStartTime = dis.readLong();
						String str = dis.readUTF();
						long sendEndTime = new Date().getTime() - timeOffset;
						System.out.println("-----------sendStartTime:" + sendStartTime + " sendEndTime:" + sendEndTime +" " + str);
						for (int i = 0; i < clients.size(); i++) {
							Client c = clients.get(i);
							c.send(sendStartTime, sendEndTime, str);
						}
					} else {
						String str = dis.readUTF();
						System.out.println("-----------from serivce:" + str);
						for (int i = 0; i < clients.size(); i++) {
							Client c = clients.get(i);
							c.send(str);
						}
					}
				}
			} catch (EOFException e) {
				System.out.println("Client closed!");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (dis != null)
						dis.close();
					if (dos != null)
						dos.close();
					if (s != null) {
						s.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
