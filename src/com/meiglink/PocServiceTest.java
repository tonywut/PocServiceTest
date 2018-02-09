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
		// String timeServerUrl = "104.225.159.46";
		//String timeServerUrl = "it158.xicp.net";
		// String timeServerUrl = "192.168.1.99";
		String timeServerUrl = "127.0.0.1";
		System.out.println("server:" + timeServerUrl);
		for (int i = 0; i < 10; i++) {
			try {
				NTPUDPClient timeClient = new NTPUDPClient();
				InetAddress timeServerAddress = InetAddress.getByName(timeServerUrl);
				TimeInfo timeInfo = timeClient.getTime(timeServerAddress);
				TimeStamp timeStamp = timeInfo.getMessage().getTransmitTimeStamp();
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
		if (timeOffset == 0) {
			System.out.println("don't get timeoffset");
		}
		System.out.println("timeOffset:" + timeOffset);
	}

	public void start() {
		getTimeOffset();
		try {
			ss = new ServerSocket(8888);
			started = true;
			System.out.println("port is opened, use 8888...");
		} catch (BindException e) {
			System.out.println("port is using...");
			System.out.println("please close some program and retart PocService!");
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
		private ObjectInputStream dis = null;
		private ObjectOutputStream dos = null;
		private boolean bConnected = false;

		public Client(Socket s) {
			this.s = s;
			try {
				dos = new ObjectOutputStream(s.getOutputStream());
				dis = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
				bConnected = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void send(long sendStartTime, long sendEndTime, String str) {
			try {
				System.out.println("start send...");
				long recvStartTime = new Date().getTime() - timeOffset;
				MsgPackage outmsg = new MsgPackage(MsgPackage.MSG_RECVTIME, sendStartTime, sendEndTime, recvStartTime,
						str);
				dos.writeObject(outmsg);
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
					MsgPackage msg = null;
					Object obj = dis.readObject();
					if (obj != null) {
						msg = (MsgPackage) obj;
					}
					if (msg.getMsgType() == MsgPackage.MSG_HEARBEAT) {
						System.out.println("-------heartbeat");
					} else if (msg.getMsgType() == MsgPackage.MSG_SENDTIME) {
						long sendStartTime = msg.getSendStartTime();
						long sendEndTime = new Date().getTime() - timeOffset;
						String str = msg.getStr();
						System.out.println("-----------sendStartTime:" + sendStartTime + " sendEndTime:" + sendEndTime
								+ " " + str);
						for (int i = 0; i < clients.size(); i++) {
							Client c = clients.get(i);
							c.send(sendStartTime, sendEndTime, str);
						}
					}
				}
			} catch (EOFException e) {
				System.out.println("Client closed!");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
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
