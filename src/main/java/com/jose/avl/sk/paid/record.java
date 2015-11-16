package com.jose.avl.sk.paid;

import java.util.Date;

/**
 * Esta clase implementa los miembros que representan las partes principales a
 * decodificar en un mensaje enviado por un equipo SkyPatrol TT8750.
 * 
 * @author José Fernández
 */
public class record {
	public long header;
	public long PARM1;
	public String MDMID;
	public String GPIOdata;
	public int GPIOdirection;
	public int ADC1;
	public int ADC2;
	public int InputEventcode;
	public Date GPSDate;
	public int GPSStatus;
	public double GPSlatitude;
	public double GPSLongitude;
	public int GPSVelocity;
	public int GPSHeading;
	public Date GPSTime;
	public int GPSAltitude;
	public int GPSNumSatellites;
	public long GPSOdometer;
	public String RTCData;
	public int Batterylevel;
	public String GPSoverSpeed;
	public String remoteAddress;
	public String remotePort;
	public String description;
	public boolean errorRecord;
	public String errorDescription;
}
