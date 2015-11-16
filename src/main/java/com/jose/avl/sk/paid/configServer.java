package com.jose.avl.sk.paid;

/**
 * Esta clase implementa una serie de miembros que representan las características
 * de la conexión.
 * 
 * @author José Fernández
 */
public class configServer {
	public String tipo;
	public String ip;
	public int puerto;
	public boolean ack;
	
	/**
	 * Estas variables se crearon para redireccionar los mensajes decodificados
	 * de los equipos Skypatrol hacia una IP y puerto remoto.
	 */
	public String remoteIP;
	public int remotePort;
}
