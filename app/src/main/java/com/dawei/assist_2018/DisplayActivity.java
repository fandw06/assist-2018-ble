package com.dawei.assist_2018;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.dawei.assist_2018.beacon.TimedBeaconSimulator;
import com.dawei.assist_2018.plot.CalibrateADC;
import com.dawei.assist_2018.plot.CalibrateAccel;
import com.dawei.assist_2018.plot.CalibrateByte;
import com.dawei.assist_2018.plot.CalibrateVol;
import com.dawei.assist_2018.plot.DataPlot;
import com.dawei.assist_2018.plot.PlotConfig;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DisplayActivity extends AppCompatActivity implements BootstrapNotifier, BeaconConsumer {

    private static final String TAG = "DISPLAY_2018";
    private static final int REQUEST_EXTERNAL_STORAGE_RW = 1;
    /** Location permission is required when scan BLE devices.*/
    private static final int REQUEST_LOCATION = 0x03;

    private static final String CUSTOM_LAYOUT = "m:2-3=0815,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
    private static final String APPLE_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";

    private BeaconManager beaconManager;
    private RegionBootstrap regionBootstrap;
    private boolean isScanning = false;

    public DataPlot ecgPlot;

    // Components
    public Button bScan;
    public TextView tInfo;
    public CheckBox cbCloud;
    public CheckBox cbLocal;

    public RadioButton rbCustom;
    public RadioButton rbApple;
    public RadioButton rbSimulation;
    public RadioButton rbBeacon;
    public CheckBox cbCheckIndex;

    public RadioButton rbAmazon;
    public RadioButton rbLab;

    public TextView tDebug;

    // status
    public boolean enabledInfluxDB = false;
    public boolean enabledLocal = false;

    private String serverIP = AMAZON_IP;
    // Amazon cloud IP
    private static final String AMAZON_IP = "34.228.10.232";

    private String dbName = "assist_2018";
    private static int OFFSET = 1;
    public InfluxDB influxDB;

    private static final String DIR = "sap-2018";
    private PrintWriter ecgWriter = null;

    private byte prevIndex = 0;

    private boolean firstTime = true;
    private boolean checkIndex = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            verifyLocationPermissions();
        }
        influxDB = null;
        Log.d(TAG, Charset.defaultCharset().displayName());

        influxDB = InfluxDBFactory.connect("http://" + serverIP +":8086", "root", "root");

        initializeBeacon();
        initializePlot();
        initializeComponents();


        Log.d(TAG, "DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD: " + String.valueOf(BeaconManager.DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD));
        Log.d(TAG, "DEFAULT_BACKGROUND_SCAN_PERIOD: " + String.valueOf(BeaconManager.DEFAULT_BACKGROUND_SCAN_PERIOD));
        Log.d(TAG, "DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD: " + String.valueOf(BeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD));
        Log.d(TAG, "DEFAULT_FOREGROUND_SCAN_PERIOD: " + String.valueOf(BeaconManager.DEFAULT_FOREGROUND_SCAN_PERIOD));
    }

    private void initializePlot() {
        PlotConfig ecgConfig = PlotConfig.builder()
                .setBytesPerSample(1)
                .setName(new String[]{"ecg"})
                .setNumOfSeries(1)
                .setResID(R.id.plot_ecg)
                .setXmlID(new int[]{R.xml.ecg_line_point_formatter})
                .setRedrawFreq(30)
                .setDomainBoundary(new double[]{0, 400})
                .setDomainInc(40.0)
                .setRangeBoundary(new double[]{0.10, 0.40})
                .setRangeInc(0.10)
                .build();
        ecgPlot = new DataPlot(this, ecgConfig, new CalibrateADC());
    }

    private void initializeBeacon() {
        beaconManager = BeaconManager.getInstanceForApplication(getApplicationContext());

        // default Apple layout
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(CUSTOM_LAYOUT));
        beaconManager.setForegroundScanPeriod(50);
        Log.d(TAG, "setting up background monitoring for beacons.");
        Region region = new Region("backgroundRegion",
                null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);
        beaconManager.bind(this);
        beaconManager.setBackgroundMode(false);
    }

    private void initializeComponents() {
        bScan = (Button) this.findViewById(R.id.scan);
        bScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (firstTime) {
                    firstTime = false;
                    cbCheckIndex.setEnabled(false);
                    rbApple.setEnabled(false);
                    rbBeacon.setEnabled(false);
                    rbCustom.setEnabled(false);
                    rbSimulation.setEnabled(false);

                    // custom header
                    if (rbCustom.isChecked())
                        beaconManager.getBeaconParsers().get(0).setBeaconLayout(CUSTOM_LAYOUT);
                    else
                        beaconManager.getBeaconParsers().get(0).setBeaconLayout(APPLE_LAYOUT);
                    if (rbSimulation.isChecked()) {
                        BeaconManager.setBeaconSimulator(new TimedBeaconSimulator());
                        ((TimedBeaconSimulator) BeaconManager.getBeaconSimulator()).USE_SIMULATED_BEACONS = true;
                        ((TimedBeaconSimulator) BeaconManager.getBeaconSimulator()).createTimedSimulatedBeacons();
                    }
                }
                if (isScanning) {
                    isScanning = false;
                    bScan.setText("Scan");
                } else {
                    isScanning = true;
                    bScan.setText("Stop");
                }
            }
        });

        cbCloud = (CheckBox) this.findViewById(R.id.cb_cloud);
        cbCloud.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (!cbCloud.isChecked()) {
                    enabledInfluxDB = false;
                    Log.d(TAG, "disconnected!");
                }
                else {
                    enabledInfluxDB = true;
                    Log.d(TAG, "Connecting Influxdb database...");
                    if (influxDB == null)
                        influxDB = InfluxDBFactory.connect("http://" + serverIP +":8086", "root", "root");
                    Log.d(TAG, "Influxdb database is connected!");
                }
            }
        });
        cbCloud.setChecked(true);

        cbLocal = (CheckBox) this.findViewById(R.id.cb_local);
        cbLocal.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (!cbLocal.isChecked()) {
                    enabledLocal = false;
                }
                else {
                    enabledLocal = true;
                }
            }
        });
        cbLocal.setEnabled(false);

        tInfo = (TextView) this.findViewById(R.id.txt_info);

        rbApple = (RadioButton) this.findViewById(R.id.r_type_apple);
        rbApple.setChecked(false);
        rbCustom = (RadioButton) this.findViewById(R.id.r_type_custom);
        rbCustom.setChecked(true);
        rbSimulation = (RadioButton) this.findViewById(R.id.r_simulation);
        rbSimulation.setChecked(true);
        rbBeacon = (RadioButton) this.findViewById(R.id.r_beacon);
        rbBeacon.setChecked(false);

        tDebug = (TextView) this.findViewById(R.id.txt_debug);
        cbCheckIndex = (CheckBox) this.findViewById(R.id.cb_check_index);
        cbCheckIndex.setChecked(true);
    }

    private void verifyLocationPermissions() {
        if (ContextCompat.checkSelfPermission(DisplayActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(DisplayActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Log.d(TAG, "Request user to grant coarse location permission");
            }
            else {
                ActivityCompat.requestPermissions(DisplayActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_LOCATION);
            }
        }
        if (ContextCompat.checkSelfPermission(DisplayActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(DisplayActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.d(TAG, "Request user to grant write permission");
            }
            else {
                ActivityCompat.requestPermissions(DisplayActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_EXTERNAL_STORAGE_RW);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Location permission is granted.");
                } else {
                    Toast.makeText(getApplicationContext(), "Location permission is denied.", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Location permissions is denied!");
                }
                break;
            }
            case REQUEST_EXTERNAL_STORAGE_RW: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Write and read permissions are granted.");
                } else {
                    Log.d(TAG, "Write and read permissions are denied!");
                }
                break;
            }
            default:
                Log.d(TAG, "Invalid permission request code.");
        }
    }

    public void saveToFile(byte[] accel, byte[] ecg, byte[] vol) {
        long timestamp = System.currentTimeMillis();
        double calAccel[][] = new double[accel.length/3][3];
        double calEcg[] = new double[ecg.length];
        double calVol[] = new double[vol.length];
        long tsAccel[] = new long[accel.length/3];
        long tsEcg[] = new long[ecg.length];
        long tsVol[] = new long[vol.length];

        long iAccel = 40;
        long iEcg = 133;
        long iVol = 400;
        /**
         * Assign sensor value.
         */
        for (int i = 0; i<accel.length/3; i++) {
            calAccel[i] = new double[3];
            calAccel[i][0] = new CalibrateAccel().calibrate(accel[i*3]);
            calAccel[i][1] = new CalibrateAccel().calibrate(accel[i*3 + 1]);
            calAccel[i][2] = new CalibrateAccel().calibrate(accel[i*3 + 2]);

            if (i == 0)
                tsAccel[i] = timestamp;
            else
                tsAccel[i] = tsAccel[i-1] +iAccel;
            Log.d(TAG, "TS: " + tsAccel[i] + " Value: " + calAccel[i][0] + " " + calAccel[i][1] + " " + calAccel[i][2]);
        }
        for (int i = 0; i<ecg.length; i++) {
            calEcg[i] = new CalibrateADC().calibrate(ecg[i]);
            if (i == 0)
                tsEcg[i] = timestamp;
            else
                tsEcg[i] = tsEcg[i-1] +iEcg;
        }
        for (int i = 0; i<vol.length; i++) {
            calVol[i] = new CalibrateADC().calibrate(vol[i]);
            if (i == 0)
                tsVol[i] = timestamp;
            else
                tsVol[i] = tsVol[i-1] +iVol;
        }
        /**
         * Create data points and write to files
         *
         */
        for (int i = 0; i<ecg.length; i++)
            ecgWriter.println(tsEcg[i] + ", " + calEcg[i]);

    }

    private void createWriters() {
        File FILES_DIR = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), DIR);
        if (!FILES_DIR.exists()) {
            boolean created = FILES_DIR.mkdirs();
            if (created)
                Log.d(TAG, "Create a new dir.");
            else
                Log.d(TAG, "Cannot create " + FILES_DIR.toString());
        }
        Log.d(TAG, "Creating local directory...");
        File local = new File(FILES_DIR, Long.toString(System.currentTimeMillis()));
        if (!local.mkdir()) {
            Log.w("LocalDir", "Local directory is not created!");
        }
        Log.d(TAG, "Creating local directory for current session...");
        try {
            ecgWriter = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(
                                    new File(local, "ecg.csv"), true)));
        } catch (Exception e) {
            System.out.println(e);
        }

        Log.d(TAG, "Files are created!");
    }

    private void closeWriters() {
        if (ecgWriter != null)
            ecgWriter.close();
    }

    public void writeInfluxDB(final byte data[]) {
        new Thread(new Runnable() {

            public void run() {
                /**
                 * Create data points and write to InfluxDB
                 *
                 */
                long timestamp = System.currentTimeMillis() * 1000 - 333333 - OFFSET * 1000 * 1000;
                double calEcg[] = new double[data.length];
                long tsEcg[] = new long[data.length];
                // 60Hz
                long iEcg = 16667;
                /**
                 * Assign sensor value.
                 */
                for (int i = 0; i<data.length; i++) {
                    calEcg[i] = new CalibrateADC().calibrate(data[i]);
                    if (i == 0)
                        tsEcg[i] = timestamp;
                    else
                        tsEcg[i] = tsEcg[i-1] +iEcg;
                }

                final BatchPoints batchPoints = BatchPoints
                        .database(dbName)
                        .tag("async", "true")
                        .retentionPolicy("autogen")
                        .consistency(InfluxDB.ConsistencyLevel.ALL)
                        .build();
                Point point[] = new Point[data.length];

                for (int i = 0; i<data.length; i++) {
                    Map<String, Object> fields = new HashMap<>();
                    fields.put("v", calEcg[i]);
                    point[i] = Point.measurement("ECG")
                            .time(tsEcg[i], TimeUnit.MICROSECONDS)
                            .fields(fields)
                            .build();
                    batchPoints.point(point[i]);
                }
                influxDB.write(batchPoints);
            }
        }).start();
    }

    private void setToAmazonIp() {
        this.serverIP = AMAZON_IP;
    }

    @Override
    public void didEnterRegion(Region region) {
        Log.d(TAG, "A beacon entered this region!");
    }

    @Override
    public void didExitRegion(Region region) {
        Log.d(TAG, "A beacon left this region!");
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        Log.d(TAG, "Determine state!");
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                Log.d(TAG, "In region! Beacon size: " + beacons.size() + " mode: " + beaconManager.getBackgroundMode());
                if (beacons.size() > 0 && isScanning) {
                    Log.d(TAG, "scanning!!!");
                    //EditText editText = (EditText)RangingActivity.this.findViewById(R.id.rangingText);
                    Beacon firstBeacon = beacons.iterator().next();

                    byte data[] = new byte[20];
                    int pointer = 0;
                    byte id1[] = firstBeacon.getId1().toByteArray();
                    byte id2[] = firstBeacon.getId2().toByteArray();
                    byte id3[] = firstBeacon.getId3().toByteArray();

                    byte p = (byte)firstBeacon.getTxPower();

                    //Reverse byte.
                    for (byte b : id1)
                        data[pointer++] = inverseByte(b);
                    for (byte b : id2)
                        data[pointer++] = inverseByte(b);
                    for (byte b : id3)
                        data[pointer++] = inverseByte(b);
                    Byte index = inverseByte(p);

                    byte packet[] = new byte[21];
                    for (int i = 0; i< 20; i++)
                        packet[i] = data[i];
                    packet[20] = index;

                    if (p != prevIndex || !cbCheckIndex.isChecked()) {
                        logToDisplay(arrayToHex(packet)) ;
                        ecgPlot.updateDataSeries(data);
                        if (cbCloud.isChecked()) {
                            writeInfluxDB(data);
                        }
                        Log.d(TAG, "index: " + (int)index);
                    }
                    prevIndex = p;
                }
            }

        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }

    private String arrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for (byte b : a) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(false);
    }

    public void logToDisplay(final String line) {
        runOnUiThread(new Runnable() {
            public void run() {
                tDebug.setText(line+"\n");
            }
        });
    }

    public static byte inverseByte(byte x) {
        byte y = 0;
        for(int i = 7; i >= 0; i--){
            y+=((x&1) << i);
            x >>= 1;
        }
        return y;
    }
}
