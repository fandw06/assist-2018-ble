package com.dawei.assist_2018.beacon;

import android.util.Log;

import com.dawei.assist_2018.DisplayActivity;

import org.altbeacon.beacon.AltBeacon;
import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimedBeaconSimulator implements org.altbeacon.beacon.simulator.BeaconSimulator {
	protected static final String TAG = "TimedBeaconSimulator";
	private List<Beacon> beacons;
	private volatile int pointer = 0;
	private volatile int index = 0;
	private final byte[] ECG = {124,
			124,
			124,
			124,
			123,
			123,
			124,
			122,
			121,
			121,
			120,
			120,
			121,
			120,
			120,
			119,
			116,
			(byte)151,
			(byte)137,
			105,
			118,
			119,
			119,
			120,
			119,
			119,
			119,
			120,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			118,
			118,
			118,
			119,
			119,
			119,
			120,
			120,
			121,
			122,
			121,
			121,
			121,
			121,
			121,
			121,
			121,
			120,
			120,
			120,
			120,
			120,
			120,
			120,
			120,
			120,
			120,
			120,
			120,
			120,
			121,
			120,
			121,
			121,
			122,
			122,
			122,
			122,
			122,
			120,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			116,
			(byte)149,
			(byte)160,
			(byte)151,
			100,
			116,
			118,
			118,
			118,
			118,
			118,
			118,
			117,
			118,
			118,
			117,
			118,
			118,
			117,
			118,
			118,
			117,
			118,
			117,
			117,
			117,
			116,
			116,
			116,
			116,
			117,
			118,
			119,
			119,
			120,
			120,
			120,
			120,
			120,
			120,
			120,
			120,
			120,
			120,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			119,
			120,
			120,
			121,
			122,
			122,
			121,
			121,
			121,
			120,
			119,
			119,
			118,
			118,
			118,
			118,
			116,
			113,
			(byte)157,
			(byte)150,
			97,
			116,
			118,
			118,
			118,
			118,
			117,
			117,
			117,
			117,
			117,
			118,
			117,
			117,
			117,
			117,
			117,
			118,
			118,
			118,
			117,
			117,
			117,
			117,
			117,
			118,
			119,
			119,
			120,
			120,
			120,
			120,
			120,
			120,
			119,
			120,
			119,
			119,
			119,
			119,
			119,
			119,
			118,
			118,
			119,
			118,
			119,
			119,
			119,
			119,
			119,
			119,
			120,
			121,
			121,
			121,
			121,
			121,
			122,
			120,
			118,
			118,
			118,
			118,
			118,
			118,
			116,
			114,
			113,
			(byte)157,
			(byte)148,
			97,
			116,
			118,
			118,
			117,
			117,
			117,
			117,
			118,
			118,
			118,
			118,
			118,
			118,
			118,
			118,
			118,
			118,
			118,
			118,
			119,
			119,
			120,
			120,
			120,
			120,
			121,
			121,
			122,
			122,
			122,
			122,
			122,
			122,
			122,
			123,
			123,
			123,
			123,
			123,
			123,
			123,
			123,
			123,
			124,
			124,
			123,
			124
	};

	/*
	 * Use simulation to test the app.
	 */
	public boolean USE_SIMULATED_BEACONS = false;

	/**
	 *  Creates empty beacons ArrayList.
	 */
	public TimedBeaconSimulator(){
		beacons = new ArrayList<>();
	}
	
	private ScheduledExecutorService scheduleTaskExecutor;

	public void createTimedSimulatedBeacons(){
		if (USE_SIMULATED_BEACONS){
			beacons = new ArrayList<>();
			Beacon beacon = new AltBeacon.Builder().setId1("DF7E1C79-43E9-44FF-886F-1D1F7DA6997A")
					.setId2("1").setId3("2").setRssi(-55).setTxPower(-55).build();
			beacons.add(beacon);

			scheduleTaskExecutor= Executors.newSingleThreadScheduledExecutor();

			// This schedules an beacon to appear every 10 seconds:
			scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
				public void run() {
					try {
						beacons.clear();
						String params[] = buildPacket();
						//Log.d(TAG, params[0] + " " + params[1] + " " + params[2] + " " + params[3]);
						Beacon beacon = new AltBeacon.Builder().setId1(params[0])
								.setId2(params[1]).setId3(params[2]).setRssi(-60).setTxPower(Integer.parseInt(params[3])).build();
						beacons.add(beacon);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, 10, 300, TimeUnit.MILLISECONDS);
		} 
	}

	@Override
	public List<Beacon> getBeacons(){
		return beacons;
	}

	private String[] buildPacket() {
		Log.d(TAG, "Pointer: " + pointer);
		byte c[] = new byte[21];
		for (int i = 0; i< 20; i++) {
			c[i] = DisplayActivity.inverseByte(ECG[pointer]);
			pointer = (pointer + 1) % ECG.length;
		}
		c[20] = (byte)index;
		index++;

		String raw = arrayToHex(c);
		Log.d(TAG, raw);
		String id1 = String.format("%s-%s-%s-%s-%s",
				raw.substring(0, 8),
				raw.substring(8, 12),
				raw.substring(12, 16),
				raw.substring(16, 20),
				raw.substring(20, 32));
		String id2 = String.valueOf(Integer.parseInt(raw.substring(32, 36), 16));
		String id3 = String.valueOf(Integer.parseInt(raw.substring(36, 40), 16));
		String p = String.valueOf(Integer.parseInt(raw.substring(40, 42), 16));
		return new String[]{id1, id2, id3, p};
	}

	// 21Bytes
	private String arrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder();
		for (byte b : a) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}
}