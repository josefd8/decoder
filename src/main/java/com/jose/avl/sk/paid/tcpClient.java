package com.jose.avl.sk.paid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;


/**
 * Esta clase implementa los metodos necesarios para establecer conexiones TCP
 * en modo cliente. Se debe especificar una IP y puerto del servidor remoto con el
 * cual se va a establecer la conexion. Mantiene un hilo de escucha para los mensajes
 * que pueda recibir del servidor remoto.
 * 
 * @author José Fernández
 * @version 1.0, 01/04/2015
 */
public class tcpClient implements Runnable {
	
	private String IPRemoto;
	private int PuertoRemoto;
	private Socket socket;
	private PrintWriter toServer;
	private BufferedReader fromServer;
	static private int BUFFER_SIZE = 1024;
	private List<TCPClientListener> listeners = new ArrayList<TCPClientListener>();	
	
	public tcpClient(TCPClientListener escuhante) {
		addTCPClientListener(escuhante);
	}

	public String getIPRemoto() {
		return IPRemoto;
	}
	
	public void setIPRemoto(String iPRemoto) {
		this.IPRemoto = iPRemoto;
	}
	
	public int getPuertoRemoto() {
		return PuertoRemoto;
	}
	
	public void setPuertoRemoto(int puertoRemoto) {
		this.PuertoRemoto = puertoRemoto;
	}
	
	/**
	 * Inicia la conexion con el servidor remoto
	 * @throws IOException if an I/O error occurs when creating the socket.
	 * @throws IllegalArgumentException  if the port parameter is outside the specified range of valid port values, which is between 0 and 65535, inclusive.
	 * @throws NullPointerException if address is null.
	 */
	public void connect() throws IOException, NullPointerException, IllegalArgumentException {
		
		int serverPort = this.PuertoRemoto; 
		InetAddress host = InetAddress.getByName(this.IPRemoto);
		
		Socket socket = new Socket(host,serverPort);
		this.socket = socket;
		this.toServer = new PrintWriter(this.socket.getOutputStream(),true);
		this.fromServer = new BufferedReader( new InputStreamReader(this.socket.getInputStream()));
		
		Thread thread1 = new Thread(this);
		thread1.start();
		
	}
	
	/**
	 * Desconecta al cliente del servidor remoto
	 * @throws IOException if an I/O error occurs when closing this socket.
	 */
	public void disconnect() throws IOException{
		this.toServer.close();
		this.fromServer.close();
		this.socket.close();
	}
	
	/**
	 * Envia un mensaje al servidor remoto
	 * @param message mensaje a enviar en formato string
	 * @throws IOException if an I/O error occurs when creating the output stream or if the socket is not connected.
	 */
	public void sendData(String message) {
		try {
			this.toServer.println(message);
		} catch (Exception e) {
			System.out.println("tcplient.sendData: No se pudo enviar data a " + this.IPRemoto + ":" + this.PuertoRemoto);
		}
			
	}

	@Override
	public void run() {
		
		try {
			while (!this.socket.isClosed()) {
				char[] cbuf = new char[BUFFER_SIZE];
				char[] output;
				
				int chars = this.fromServer.read(cbuf, 0, BUFFER_SIZE);
				
				if (chars >= 0) {
					output = Arrays.copyOf(cbuf, chars);
					String message = new String(output);
					this.onMessageReceivedTCPClient(message);
					System.out.println("aqui");
				} else {
					break;
				}
				
			}
		} catch (IOException e) {
			this.onErrorOcurredTCPClient(e.getMessage());
		}
		
	}
	
	public static interface TCPClientListener extends EventListener{
		void onMessageReceivedTCPClient(String message);
		void onErrorOcurredTCPClient(String errorMessage);
	}
	
	public void addTCPClientListener(TCPClientListener listener){
		listeners.add(listener);
	}
	
	protected synchronized void onMessageReceivedTCPClient( String message){
		for (int i=0;i<listeners.size();i++) {
			listeners.get(i).onMessageReceivedTCPClient(message);	    
		}
	}

	protected synchronized void onErrorOcurredTCPClient( String errorMessage){
		for (int i=0;i<listeners.size();i++) {
			listeners.get(i).onErrorOcurredTCPClient(errorMessage);	    
		}
	}
	
}
