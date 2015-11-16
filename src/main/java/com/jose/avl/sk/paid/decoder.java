package com.jose.avl.sk.paid;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import com.jose.avl.sk.paid.tcpClient.TCPClientListener;


public class decoder implements tcpServer.TCPServerListener, udpServer.UDPServerListener, interpretadorOrdenesConsola.OrdenesDeConsola, TCPClientListener {

	static configServer server;
	static tcpServer servidorTCP;
	static udpServer servidorUDP;
	static tcpClient clienteTCP;
	static interpretadorOrdenesConsola io = new interpretadorOrdenesConsola(new decoder());
	static boolean ondebug = true;
	static BigInteger mensajesRecibidos = new BigInteger("0");
	private static log_file log = new log_file();
	
	public static void main(String[] args) {
	
		inicializar();

	}
	
	private static void inicializar(){
		
		server = ObtenerConfigBD();
		iniciarServidor(server);
		inicializarCliente(server);
	}
	
	private static void decodificar(byte[] b, String id){
		
		mensajesRecibidos = mensajesRecibidos.add(new BigInteger("1"));
		record registro = new record();
		skypatrol d = new skypatrol(b, id, server.tipo);
		registro = d.decodificar_Skypatrol();
		
		SimpleDateFormat formatterFecha = new SimpleDateFormat("dd-MM-yyyy");
		SimpleDateFormat formatterHora = new SimpleDateFormat("HH:mm:ss");
		
		String mensaje = registro.remoteAddress + ":" + registro.remotePort + " ";
		
		if (!(registro.PARM1 == 999)) {
			mensaje = mensaje + registro.MDMID + " Evento: " + registro.PARM1 + " Puertos: " + registro.GPIOdata;
			mensaje = mensaje + " Date: " + formatterFecha.format(registro.GPSDate) + " " + formatterHora.format(registro.GPSTime);
			mensaje = mensaje + " GPSStatus: " + registro.GPSStatus + " Lat: " + registro.GPSlatitude + " Lon: " + registro.GPSLongitude;
			mensaje = mensaje + " Velocidad: " + registro.GPSVelocity + " Altitud: " + registro.GPSAltitude + " Satelites: " + registro.GPSNumSatellites;
		} else {
			mensaje = mensaje + registro.MDMID + " Mensaje de WakeUp";
		}
		
		if ((server.ack) && (server.tipo.toUpperCase().matches("UDP"))){
			byte[] ack = new byte[]{0x00,0x0A,0x01,0x00,0x41,0x43,0x4B};
			enviarData(registro.remoteAddress + ":" + registro.remotePort, ack);
		}
		
		mostrarMensaje("nuevo mensaje desde: " + mensaje);
		log.escribir(mensaje, "mensajes.txt");
		
		
		//Reenvio del mensaje a la IP y puerto
		if (!(registro.PARM1 == 999)) {
			reenviarAServidorRemoto(registro);
		}
	}
	
	private static void reenviarAServidorRemoto(record registro){
		
		SimpleDateFormat formatterFecha = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat formatterHora = new SimpleDateFormat("HHmmss");
		long timestamp = System.currentTimeMillis()/1000;
		String status;
		
		if (registro.GPSStatus == 1){
			status = "A";
		} else {
			status = "V";
		}
		
		/**
		 * Los Skypatrol no continen un campo de HDOP en su mensaje, asi que para tener
		 * un numero aproximado utilizo la cantidad de satelites. A mayor cantidad de
		 * satelites mas precision (menor HDOP), y a menos cantidad de satelites menor
		 * la precision (mayor HDOP). Para esto utilizo la formula HDOP = 20/(Numero satelites)^2
		 */
	
		double HDOP;
		if (registro.GPSNumSatellites > 0){
			HDOP = 20/(registro.GPSNumSatellites * registro.GPSNumSatellites);
		} else {
			HDOP = 20;
		}
		
		//Creamos la cadena a enviar
		String mensajeAEnviar = String.valueOf(timestamp) + ",";
		mensajeAEnviar = mensajeAEnviar + String.valueOf(registro.PARM1) + ",";
		mensajeAEnviar = mensajeAEnviar + String.valueOf(registro.MDMID).trim() + ",";
		mensajeAEnviar = mensajeAEnviar + "0" + ",";
		mensajeAEnviar = mensajeAEnviar + String.valueOf(formatterFecha.format(registro.GPSDate)).replace("-", "") + ",";
		mensajeAEnviar = mensajeAEnviar + String.valueOf(formatterHora.format(registro.GPSTime)).replace(":", "") + ",";
		mensajeAEnviar = mensajeAEnviar + status + ",";
		mensajeAEnviar = mensajeAEnviar + String.valueOf(HDOP) + ",";
		mensajeAEnviar = mensajeAEnviar + String.valueOf(registro.GPSlatitude) + ",";
		mensajeAEnviar = mensajeAEnviar + String.valueOf(registro.GPSLongitude) + ",";
		mensajeAEnviar = mensajeAEnviar + String.valueOf(registro.GPSHeading) + ",";
		mensajeAEnviar = mensajeAEnviar + String.valueOf(registro.GPSVelocity) + ",";
		mensajeAEnviar = mensajeAEnviar + String.valueOf(registro.GPSAltitude) + ",";
		mensajeAEnviar = mensajeAEnviar + String.valueOf(registro.Batterylevel);
			
		/**
		 * Para el reenvio a la IP y puerto remoto, nos conectamos, enviamos el mensaje
		 * y luego nos desconectamos
		 */
		try {
			clienteTCP.connect();
			clienteTCP.sendData(mensajeAEnviar);
			clienteTCP.disconnect();
		} catch (NullPointerException e) {
			System.out.println("La direccion del servidor remoto no puede ser nula");
		} catch (IllegalArgumentException e) {
			System.out.println("El valor del puerto remoto no tiene un formato valido");
		} catch (IOException e) {
			System.out.println("Ocurrio un error al iniciar la conexion con el servidor remoto");
		}
		
	}
	
	private static void enviarData(String destino, byte[] ack){
		
		String tipoServidor = server.tipo.toUpperCase();
		
		switch (tipoServidor) {
		
		case "TCP":
			servidorTCP.sendData(destino, ack);
			break;
			
		case "UDP":
			String[] direccion = destino.split(":");
			try {
				servidorUDP.sendData(InetAddress.getByName(direccion[0]), Integer.valueOf(direccion[1]), ack);
			} catch (NumberFormatException e) {
				mostrarMensaje("Problema al enviar acknowledge: " +e.getMessage());
			} catch (UnknownHostException e) {
				mostrarMensaje("Problema al enviar acknowledge: " +e.getMessage());
			}
			break;
			
		}
		
	}
	
	private static void iniciarServidor(configServer server){
		
		String tipoServidor = server.tipo.toUpperCase();
		
		switch (tipoServidor) {
		case "TCP":
			servidorTCP = new tcpServer(new decoder(), "");
			servidorTCP.setIPEscucha(server.ip);
			servidorTCP.setPuertoEscucha(server.puerto);
			try {
				servidorTCP.startTcpServer();
				System.out.println("Iniciado Socket TCP en: " + server.ip + ":" + server.puerto);
			} catch (UnknownHostException e1) {
				System.out.println("Se encontro un problema al abrir el Socket TCP: " + e1.getMessage());
			} catch (IllegalArgumentException e1) {
				System.out.println("Se encontro un problema al abrir el Socket TCP: " + e1.getMessage());
			} catch (IOException e1) {
				System.out.println("Se encontro un problema al abrir el Socket TCP: " + e1.getMessage());
			}
			break;
			
		case "UDP":
			servidorUDP = new udpServer(new decoder());
			servidorUDP.setIPEscucha(server.ip);
			servidorUDP.setPuertoEscucha(server.puerto);
			try {
				servidorUDP.startUDPServer();
				System.out.println("Iniciado Socket UDP en: " + server.ip + ":" + server.puerto);
			} catch (SocketException | UnknownHostException e) {
				System.out.println("Se encontro un problema al abrir el Socket UDP: " + e.getMessage());
			}
			break;

		default:
			System.out.println("La configuracion del tipo de servidor no es correcta");
			break;
		}
	}
	
	private static void inicializarCliente(configServer server){
		
		clienteTCP = new tcpClient(new decoder());
		clienteTCP.setIPRemoto(server.remoteIP);
		clienteTCP.setPuertoRemoto(server.remotePort);
	}
		
	private static configServer ObtenerConfigBD(){

		configServer server = new configServer();
		File f = new File("config.xml");

		if (f.exists()) {
			
			try {
				XMLConfiguration config = new XMLConfiguration(f);
				server.tipo= config.getString("server.tipo");
				server.ip = config.getString("server.IP");
				server.puerto = config.getInt("server.puerto");
				server.ack = config.getBoolean("server.ack");
				server.remoteIP = config.getString("server.remoteIP");
				server.remotePort = config.getInt("server.remotePort");
			} catch (ConfigurationException e) {
				System.out.println("Se encontro un error al leer el archivo de configuracion: " + e.getMessage());
			}			
		} else {
			
			String contenido;
			contenido="<config> <server> <tipo>UDP</tipo> <puerto>8080</puerto> <IP>127.0.0.1</IP> <ack>false</ack> <remoteIP>127.0.0.1</remoteIP> <remotePort>80</remotePort> </server> </config>";
			File new_archivo= new File("config.xml");
			FileWriter escribir;
			try {
				System.out.println("No se encontro el archivo de configuracion config.xml. Se creara automaticamente");
				escribir = new FileWriter(new_archivo,true);
				escribir.write(contenido);
				escribir.close();
			} catch (IOException e) {
				System.out.println("Se encontro un problema al crear el archivo de configuracion: " + e.getMessage());
			}	
			System.exit(0);
		}
		return server;
	}

	private static void mostrarMensaje(String mensaje){
		
		if (ondebug) {
			System.out.println(mensaje);
		}
		
	}
	
	private static void reiniciarServidor(){
		
		String tipoServidor = server.tipo.toUpperCase();
		
		switch (tipoServidor) {
		case "TCP":
			try {
				servidorTCP.stopTcpServer();
			} catch (IOException e) {
				System.out.println("Error al cerrar el servidor TCP: " + e.getMessage());
			}
			inicializar();
			
		break;
			
		case "UDP":
			servidorUDP.stopUDPServer();
			inicializar();
		break;
		
		default:
			System.out.println("La configuracion del tipo de servidor no es correcta");
			break;
		}
			
	}
	
	@Override
	public void onMessageReceived(byte[] b, String id) {
		decodificar(b, id);
		
	}

	@Override
	public void onMessageReceived(byte[] b, String id, String identificador,
			Socket remote) {
		decodificar(b, id);
		
	}

	@Override
	public void onClientConnected(String id, String identificador) {
		mostrarMensaje("Cliente " + id + " conectado");
		
	}

	@Override
	public void onClientDisconnected(String id, String identificador) {
		mostrarMensaje("Cliente " + id + " desconectado");
		
	}

	@Override
	public void nuevaOrden(String orden) {
		
		switch (orden) {

		case "reiniciar":
			reiniciarServidor();
			break;

		case "estado":
			if (server.tipo.toUpperCase().matches("TCP")) {
				System.out.println(mensajesRecibidos.toString() + "/" + servidorTCP.getClientsTable());
			} else {
				System.out.println(mensajesRecibidos.toString() + "/" + 0);
			}
			break;

		case "finalizar":
			String tipoServidor = server.tipo.toUpperCase();
			if (tipoServidor.matches("TCP")) {
				servidorTCP.disconnectAllClients();	
			}
			break;

		case "ondebug":
			ondebug = true;
			break;

		case "offdebug":    
			ondebug = false;
			break;	

		case "salir":
			System.exit(0);
			break;
		}
		
	}

	@Override
	public void onMessageReceivedTCPClient(String message) {
		System.out.println("Mensaje desde " + clienteTCP.getIPRemoto() + ":" + clienteTCP.getPuertoRemoto() + ": " + message);
		
	}

	@Override
	public void onErrorOcurredTCPClient(String errorMessage) {
		System.out.println("Error desde " + clienteTCP.getIPRemoto() + ":" + clienteTCP.getPuertoRemoto() + ": " + errorMessage);
		
	}
	
}
