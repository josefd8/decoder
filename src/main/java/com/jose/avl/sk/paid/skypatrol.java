package com.jose.avl.sk.paid;

import java.text.SimpleDateFormat;

/**
 * Esta clase implementa los métodos necesarios para la decodificación de tramas
 * de los equipos SkyPatrol TT8750, tanto para protocolos UDP como TCP.
 * 
 * @author José Fernández
 * @version 2.2, 14/01/2014
 */
public class skypatrol {

	private byte[] mensaje;
	private String id;
	private String protocol;

	public skypatrol(byte[] mensaje, String id, String protocol) {
		super();
		this.mensaje = mensaje;
		this.id = id;
		this.protocol = protocol;
	}

	public record decodificar_Skypatrol(){

		int contador = 0;
		record registro = new record();
		int parameter_number = (int) mensaje[1];
		String remote[];
		remote = this.id.split(":");
		registro.remoteAddress = remote[0];
		registro.remotePort = remote[1];
		
		if (this.protocol.matches("TCP")) {
			contador += 2;
			parameter_number = (int) mensaje[3];
		} else {
			parameter_number = (int) mensaje[1];
		}
		
		registro.header = intArrayToLong(this.mensaje,contador,4);
		contador += 4;
		
		switch (parameter_number) {
		
		case 5:	
			
			try {
			registro.description = "Mensaje de posición";
				
			registro.PARM1 = intArrayToLong(this.mensaje, contador, 4);
			contador += 4;
			
			byte[] id = new byte[22];
			for (int i = 0; i < 22; i++) {
				id[i] = mensaje[i + contador];	
			}
			registro.MDMID = new String(id);
			contador += 22;

			registro.GPIOdata = Integer.toBinaryString(this.mensaje[contador] & 0xFF);
			contador += 1;
			
			registro.GPIOdirection = this.mensaje[contador] & 0xFF;
			contador += 1;
			
			registro.ADC1 = (int) intArrayToLong(this.mensaje, contador, 2);
			contador += 2;

			registro.ADC2 = (int) intArrayToLong(this.mensaje, contador, 2);
			contador += 2;
			
			registro.InputEventcode = this.mensaje[contador] & 0xFF;
			contador += 1;
			
			String fecha = String.valueOf(intArrayToLong(this.mensaje, contador, 3));
			fecha = fecha.substring(0, 2) + "-" + fecha.substring(2, 4) + "-20" + fecha.substring(4, 6);
			SimpleDateFormat ft = new SimpleDateFormat ("dd-MM-yyyy");
			registro.GPSDate = ft.parse(fecha);
			
			contador += 3;
			
			registro.GPSStatus = this.mensaje[contador] & 0xFF;
			contador += 1;
			
			double lat = intArrayToLong(this.mensaje, contador, 3);
			if (lat > 8388607) {
				lat = lat - 16777215;
			}
			lat = lat / 100000;
			double parte_entera = (int) lat;
			double resto = (lat - parte_entera);
			resto = resto / 60 * 100;
			registro.GPSlatitude = parte_entera + resto;		
			contador += 3;
			
			
			double lon = intArrayToLong(this.mensaje, contador, 4);
			if (lon > 2147483647) {
				lon = lon - Long.parseLong("4294967295");
			}
			lon = lon / 100000;
			double parte_entera2 = (int) lon;
			double resto2 = (lon - parte_entera2);
			resto2 = resto2 / 60 * 100;
			registro.GPSLongitude = parte_entera2 + resto2;		
			contador += 4;
			
			registro.GPSVelocity = (int) intArrayToLong(this.mensaje, contador, 2);
			contador += 2;
			
			registro.GPSHeading = (int) intArrayToLong(this.mensaje, contador, 2);
			contador += 2;
			
			String hora = String.valueOf(intArrayToLong(this.mensaje, contador, 3));
			hora = hora.substring(0, 2) + ":" + hora.substring(2, 4) + ":" + hora.substring(4, 6);
			SimpleDateFormat ft2 = new SimpleDateFormat ("H:m:s");
			registro.GPSTime = ft2.parse(hora);
			contador += 3;
			
			registro.GPSAltitude = (int) intArrayToLong(this.mensaje, contador, 3);
			contador += 3;
			
			registro.GPSNumSatellites = this.mensaje[contador] & 0xFF;
			contador += 1;
			
			registro.GPSOdometer = intArrayToLong(this.mensaje, contador, 4);
			contador += 4;
			
			String RTC = "";
			for (int i = contador; i < contador + 6; i++) {
				RTC = RTC + (this.mensaje[contador] & 0xFF);
			}
			registro.RTCData = RTC;
			contador += 6;
			
			registro.Batterylevel = this.mensaje[contador] & 0xFF;
			contador += 1;
			
			String GOS = "";
			GOS = GOS + (int) intArrayToLong(this.mensaje, contador, 2);
			GOS = GOS + (int) intArrayToLong(this.mensaje, contador + 2, 2);
			GOS = GOS + (int) intArrayToLong(this.mensaje, contador + 4, 2);
			registro.GPSoverSpeed = GOS;

			registro.errorRecord = false;
			registro.errorDescription = "";
			return registro;
			
			} catch (Exception e) {
				registro.errorRecord = true;
				registro.errorDescription = e.getMessage();
				return registro;
			}

		case 10:
			registro.description = "Mensaje de WakeUp";
			
			int contador2 = 0;
			int idlenght = 0;
			if (this.protocol.matches("TCP")) {
				contador2 = 6;
				idlenght = 20;
			} else {
				contador2 = 14;
				idlenght = 22;
			}
			
			
			byte[] id = new byte[idlenght];
			for (int i = 0; i < idlenght; i++) {
				id[i] = mensaje[i + contador2];	
			}
			registro.MDMID = new String(id);
			registro.PARM1 = 999;
			return registro;
			
			
		default:
			break;
		}
		
		
		return registro;
		
	}

	private long intArrayToLong(byte[] cadena, int posicion, int largo){

		String hex = "";

		for (int i = 0; i <= largo -1; i++) {
			int e = cadena[posicion + i] & 0xFF;
			
			String e2Hex = Integer.toHexString(e);
			
			if (e2Hex.length() == 1) {
				hex = hex + "0" + e2Hex;
			}else{
				hex = hex + e2Hex;
			}
		}

		long valor = Long.parseLong(hex, 16);
		return valor;

	}

}
