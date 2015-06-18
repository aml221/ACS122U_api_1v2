package com.example;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import com.acs.smartcard.Features;
import com.acs.smartcard.PinProperties;
import com.acs.smartcard.Reader;
import com.acs.smartcard.Reader.OnStateChangeListener;
import com.acs.smartcard.TlvProperties;

public class Reader_part extends Activity{
	
	public RelativeLayout layout1;
    public RelativeLayout layout2;
    
    public int itemCount = 0;

	public byte Uid[] = { (byte) 0x07, (byte) 0xF9, (byte) 0x61, (byte) 0xB1 };
	
	public String APDUcmdType = "";
	

	private static final String TAG = "ACSAndroidActivity";
	
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private static final String[] powerActionStrings = { "Power Down",
            "Cold Reset", "Warm Reset" };

    private static final String[] stateStrings = { "Unknown", "Absent",
            "Present", "Swallowed", "Powered", "Negotiable", "Specific" };

    private UsbManager mManager;
    private Reader mReader;
    private PendingIntent mPermissionIntent;

    private static final int MAX_LINES = 25;

    private Features mFeatures = new Features();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {

                synchronized (this) {

                    UsbDevice device = (UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                        if (device != null) {

                            // Open reader
                            //logMsg("Opening reader: " + device.getDeviceName()+ "...");
                            new OpenTask().execute(device);
                        }

                    } else {

                        //logMsg("Permission denied for device " + device.getDeviceName());

                        // Enable open button
                    }
                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

                synchronized (this) {

                    // Update reader list
                    //mReaderAdapter.clear();
                    for (UsbDevice device : mManager.getDeviceList().values()) {
                        if (mReader.isSupported(device)) {
                           // mReaderAdapter.add(device.getDeviceName());
                        	Log.v(TAG, "device add:" + device.getDeviceName());
                        }
                    }

                    UsbDevice device = (UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (device != null && device.equals(mReader.getDevice())) {

                        // Close reader
                        new CloseTask().execute();
                    }
                }
            }
        }
    };

    private class OpenTask extends AsyncTask<UsbDevice, Void, Exception> {

        @Override
        protected Exception doInBackground(UsbDevice... params) {

            Exception result = null;

            try {

                mReader.open(params[0]);

            } catch (Exception e) {

                result = e;
            }

            return result;
        }

        @Override
        protected void onPostExecute(Exception result) {

            if (result != null) {

                //logMsg(result.toString());

            } else {

                //logMsg("Reader name: " + mReader.getReaderName());

                int numSlots = mReader.getNumSlots();
                //logMsg("Number of slots: " + numSlots);

                // Add slot items
                //mSlotAdapter.clear();
                for (int i = 0; i < numSlots; i++) {
                    //mSlotAdapter.add(Integer.toString(i));
                }

                // Remove all control codes
                mFeatures.clear();
            }
        }
    }

    private class CloseTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            mReader.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //mOpenButton.setEnabled(true);
        }

    }

    private class PowerParams {

        public int slotNum;
        public int action;
    }

    private class PowerResult {

        public byte[] atr;
        public Exception e;
    }

    private class PowerTask extends AsyncTask<PowerParams, Void, PowerResult> {

        @Override
        protected PowerResult doInBackground(PowerParams... params) {

            PowerResult result = new PowerResult();

            try {

                result.atr = mReader.power(params[0].slotNum, params[0].action);

            } catch (Exception e) {

                result.e = e;
            }

            return result;
        }

        @Override
        protected void onPostExecute(PowerResult result) {

            if (result.e != null) {

                //logmsg(result.e.toString());

            } else {

                // Show ATR
                if (result.atr != null) {

                    //logmsg("ATR:");
                    //logBuffer(result.atr, result.atr.length);

                } else {

                    //logmsg("ATR: None");
                }
            }
        }
    }

    private class SetProtocolParams {

        public int slotNum;
        public int preferredProtocols;
    }

    private class SetProtocolResult {

        public int activeProtocol;
        public Exception e;
    }

    private class SetProtocolTask extends
            AsyncTask<SetProtocolParams, Void, SetProtocolResult> {

        @Override
        protected SetProtocolResult doInBackground(SetProtocolParams... params) {

            SetProtocolResult result = new SetProtocolResult();

            try {

                result.activeProtocol = mReader.setProtocol(params[0].slotNum,
                        params[0].preferredProtocols);

            } catch (Exception e) {

                result.e = e;
            }

            return result;
        }

        @Override
        protected void onPostExecute(SetProtocolResult result) {

            if (result.e != null) {

                //logmsg(result.e.toString());

            } else {

                String activeProtocolString = "Active Protocol: ";

                switch (result.activeProtocol) {

                case Reader.PROTOCOL_T0:
                    activeProtocolString += "T=0";
                    break;

                case Reader.PROTOCOL_T1:
                    activeProtocolString += "T=1";
                    break;

                default:
                    activeProtocolString += "Unknown";
                    break;
                }

                // Show active protocol
                //logmsg(activeProtocolString);
            }
        }
    }

    private class TransmitParams {

        public int slotNum;
        public int controlCode;
        public String commandString;
        public String commandType;
    }

    private class TransmitProgress {

        public int controlCode;
        public byte[] command;
        public int commandLength;
        public byte[] response;
        public int responseLength;
        public Exception e;
    }

    private class TransmitTask extends
            AsyncTask<TransmitParams, TransmitProgress, Void> {

        @Override
        protected Void doInBackground(TransmitParams... params) {

            TransmitProgress progress = null;

            byte[] command = null;
            byte[] response = null;
            int responseLength = 0;
            int foundIndex = 0;
            int startIndex = 0;

            do {

                // Find carriage return
                foundIndex = params[0].commandString.indexOf('\n', startIndex);
                if (foundIndex >= 0) {
                    command = toByteArray(params[0].commandString.substring(
                            startIndex, foundIndex));
                } else {
                    command = toByteArray(params[0].commandString
                            .substring(startIndex));
                }

                // Set next start index
                startIndex = foundIndex + 1;

                response = new byte[300];
                progress = new TransmitProgress();
                progress.controlCode = params[0].controlCode;
                try {

                    if (params[0].controlCode < 0) {

                        // Transmit APDU
                        responseLength = mReader.transmit(params[0].slotNum,
                                command, command.length, response,
                                response.length);

                    } else {

                        // Transmit control command
                        responseLength = mReader.control(params[0].slotNum,
                                params[0].controlCode, command, command.length,
                                response, response.length);
                    }

                    progress.command = command;
                    progress.commandLength = command.length;
                    progress.response = response;
                    progress.responseLength = responseLength;
                    progress.e = null;

                } catch (Exception e) {

                    progress.command = null;
                    progress.commandLength = 0;
                    progress.response = null;
                    progress.responseLength = 0;
                    progress.e = e;
                }

                publishProgress(progress);

            } while (foundIndex >= 0);

            return null;
        }

        @Override
        protected void onProgressUpdate(TransmitProgress... progress) {

            if (progress[0].e != null) {

                //logmsg(progress[0].e.toString());

            }
            else {
	            	Log.i(TAG,"APDUcmdType:"+APDUcmdType);
	            	TransmitParams params = new TransmitParams();


                if(APDUcmdType.equals("getUID"))
                {
                    Uid = progress[0].response;
                    appConstants.diversify(Uid);
                    Log.i(TAG, "authKeyA:" + appConstants.authKeyA + ", authKeyB:" + appConstants.authKeyB);

                    // store KeyA in reader
                    // Set parameters
                    params.slotNum = appConstants.slotNum;
                    params.controlCode = appConstants.controlCode;
                    //params.commandString = "FF82000006" + appConstants.authKeyA;
                    params.commandString = APDU_build.storeKey(appConstants.KeyA, appConstants.authKeyA);
                    Log.i(TAG, params.commandString);

                    // Transmit control command
                    new TransmitTask().execute(params);
                    APDUcmdType = "storeKeyA";
                }
                else if(APDUcmdType.equals("storeKeyA"))
                {
                    // store KeyA in reader
                    // Set parameters
                    params.slotNum = appConstants.slotNum;
                    params.controlCode = appConstants.controlCode;
                    //params.commandString = "FF82000106" + appConstants.authKeyB;
                    params.commandString = APDU_build.storeKey(appConstants.KeyB, appConstants.authKeyB);
                    Log.i(TAG, params.commandString);

                    // Transmit control command
                    new TransmitTask().execute(params);
                    APDUcmdType = "storeKeyB";
                }
                else if(APDUcmdType.equals("storeKeyB"))
                {
                    // Authenticate using KeyA
                    // Set parameters
                    params.slotNum = appConstants.slotNum;
                    params.controlCode = appConstants.controlCode;
                    params.commandString = "FF8600000501000C6101";
                    params.commandString = APDU_build.authenticate(appConstants.KeyB, APDU_build.block_0C);

                    // Transmit control command
                    new TransmitTask().execute(params);
                    APDUcmdType = "auth";
                }
                else if(APDUcmdType.equals("auth"))
                {
                    // Read Card ID
                    // Set parameters
                    params.slotNum = appConstants.slotNum;
                    params.controlCode = appConstants.controlCode;
                    //params.commandString = "FFB0000C08";
                    params.commandString = APDU_build.readBlock(APDU_build.block_type_binary, APDU_build.block_0C, APDU_build.size_bytes_08);

                    // Transmit control command
                    new TransmitTask().execute(params);
                    APDUcmdType = "readID";
                }
                else if(APDUcmdType.equals("readID"))
                {
                    // Read Card Balance
                    // Set parameters
                    params.slotNum = appConstants.slotNum;
                    params.controlCode = appConstants.controlCode;
                    //params.commandString = "FFB1000D04";
                    params.commandString = APDU_build.readBlock(APDU_build.block_type_value, APDU_build.block_0D, APDU_build.size_bytes_04);

                    // Transmit control command
                    new TransmitTask().execute(params);
                    APDUcmdType = "readBal";
                    appConstants.userID = Arrays.copyOfRange(progress[0].response, 0, progress[0].responseLength-2);
                    Log.i(TAG, "userID:"+appConstants.bytesToHex(appConstants.userID));
                }
                else if(APDUcmdType.equals("readBal"))
                {
                    // Read Card txn counter
                    // Set parameters
                    params.slotNum = appConstants.slotNum;
                    params.controlCode = appConstants.controlCode;
                    //params.commandString = "FFB1000E04";
                    params.commandString = APDU_build.readBlock(APDU_build.block_type_value, APDU_build.block_0E, APDU_build.size_bytes_04);

                    // Transmit control command
                    new TransmitTask().execute(params);
                    APDUcmdType = "readTxnCtr";
                    appConstants.balance_ = Arrays.copyOfRange(progress[0].response, 0, progress[0].responseLength-2);
                    Log.i(TAG, "Bal:" + appConstants.bytesToHex(appConstants.balance_));
                }
                else if(APDUcmdType.equals("readTxnCtr"))
                {
                    APDUcmdType = "deductBal";
                    appConstants.txnCtr_ = Arrays.copyOfRange(progress[0].response, 0, progress[0].responseLength-2);
                    Log.i(TAG, "Txn Ctr:" + appConstants.bytesToHex(appConstants.txnCtr_));
                    appConstants.parseData();
                }
                else if(APDUcmdType.equals("deductBal"))
                {
                    APDUcmdType = "";
                }
            }
        }

		
    }
    
    public void print_(View v)
    {
    	//printData();
    	deductBalance();
    }
    
    public void countItems(View v)
    {
    	itemCount ++;
    }
    
    public void deductBalance()
    {
    	// Set parameters
    	TransmitParams params = new TransmitParams();
        params.slotNum = appConstants.slotNum;
        params.controlCode = appConstants.controlCode;
        //params.commandString = "FFD700" + block_no + "05" + txn_op + itemCount_hex;
        params.commandString = APDU_build.transact(APDU_build.block_0D, APDU_build.txn_op_sale, itemCount);
        APDUcmdType = "";
        new TransmitTask().execute(params);
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_part);
        
        //aeea68
        
        layout1 = (RelativeLayout) findViewById(R.id.layout1);
        layout2 = (RelativeLayout) findViewById(R.id.layout2);
        
        //InitializeReader();
        
        // open printer
        //setupPrinter();
        
        
        // Get USB manager
        mManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // Initialize reader
        mReader = new Reader(mManager);
        mReader.setOnStateChangeListener(new OnStateChangeListener() {

            @Override
            public void onStateChange(int slotNum, int prevState, int currState) {

                if (prevState < Reader.CARD_UNKNOWN
                        || prevState > Reader.CARD_SPECIFIC) {
                    prevState = Reader.CARD_UNKNOWN;
                }

                if (currState < Reader.CARD_UNKNOWN
                        || currState > Reader.CARD_SPECIFIC) {
                    currState = Reader.CARD_UNKNOWN;
                }
                
                if(stateStrings[currState] == "Present")
            	{	
                	runOnUiThread(new Runnable() {
	                    @Override
	                    public void run() {
	                        // This code will always run on the UI thread, therefore is safe to modify UI elements.
	                    	changeLayout(1);
	                    }
                	});
                	
                	itemCount = 0;
                	ToggleButton t1 = (ToggleButton)findViewById(R.id.toggleButton1);
                	ToggleButton t2 = (ToggleButton)findViewById(R.id.toggleButton2);
                	ToggleButton t3 = (ToggleButton)findViewById(R.id.toggleButton3);
                	ToggleButton t4 = (ToggleButton)findViewById(R.id.ToggleButton01);
                	ToggleButton t5 = (ToggleButton)findViewById(R.id.ToggleButton02);
                	ToggleButton t6 = (ToggleButton)findViewById(R.id.ToggleButton03);
                	ToggleButton t7 = (ToggleButton)findViewById(R.id.ToggleButton04);
                	ToggleButton t8 = (ToggleButton)findViewById(R.id.ToggleButton05);
                	ToggleButton t9 = (ToggleButton)findViewById(R.id.ToggleButton06);
                	
                	t1.setChecked(false);
                	t2.setChecked(false);
                	t3.setChecked(false);
                	t4.setChecked(false);
                	t5.setChecked(false);
                	t6.setChecked(false);
                	t7.setChecked(false);
                	t8.setChecked(false);
                	t9.setChecked(false);
                	
                    // Set parameters
                    TransmitParams params = new TransmitParams();
                    params.slotNum = 0;
                    params.controlCode = 3500;
                    params.commandString = "FFCA000000";
                    APDUcmdType = "getUID";
                    new TransmitTask().execute(params);
                    
            	}
                if(stateStrings[prevState] == "Present")
            	{
                	Log.i(TAG, "why not run!?!?!");
                	runOnUiThread(new Runnable() {
	                    @Override
	                    public void run() {
	                        // This code will always run on the UI thread, therefore is safe to modify UI elements.
	                    	changeLayout(2);
	                    }
                	});
            	}

                // Create output string
                final String outputString = "Slot " + slotNum + ": "
                        + stateStrings[prevState] + " -> "
                        + stateStrings[currState];
                Log.v(TAG, "output String:" + outputString);
            }

			private void changeLayout(int i) {
				Log.i(TAG, "Bhosad| i:" + i);
				if(i == 1)
				{
					layout1.setVisibility(View.GONE);
                	layout2.setVisibility(View.VISIBLE);
				}
				else
				{
					layout1.setVisibility(View.VISIBLE);
                	layout2.setVisibility(View.GONE);
				}
				
			}
        });
        
        Button amlInitialize = (Button) findViewById(R.id.button_start);
        amlInitialize.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
            	InitializeReader();
            }
        });
        

        // Register receiver for USB permission
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mReceiver, filter);

        /*
        // Initialize open button
        mOpenButton = (Button) findViewById(R.id.button_start);
        mOpenButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                boolean requested = false;

                // Disable open button
                mOpenButton.setEnabled(false);

                //String deviceName = (String) mReaderSpinner.getSelectedItem();
                String deviceName = "/dev/bus/usb/002/002";

                if (deviceName != null) {

                    // For each device
                    for (UsbDevice device : mManager.getDeviceList().values()) {

                        // If device name is found
                        if (deviceName.equals(device.getDeviceName())) {

                            // Request permission
                            mManager.requestPermission(device,
                                    mPermissionIntent);

                            requested = true;
                            break;
                        }
                    }
                }

                if (!requested) {

                    // Enable open button
                    mOpenButton.setEnabled(true);
                }
            }
        });*/
        /*
        Button amlPrint = (Button) findViewById(R.id.button_printer_open);
        amlPrint.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
            	setupPrinter();
            	printData();
            }
        });
        
        Button amlButton = (Button) findViewById(R.id.button_start);
        amlButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
            	
            	//setupPrinter();
            	
            	boolean requested = false;

                // Disable open button
                //mOpenButton.setEnabled(false);

                //String deviceName = (String) mReaderSpinner.getSelectedItem();
                String deviceName = "/dev/bus/usb/002/002";

                if (deviceName != null) {

                    // For each device
                    for (UsbDevice device : mManager.getDeviceList().values()) {

                        // If device name is found
                        if (deviceName.equals(device.getDeviceName())) {

                            // Request permission
                            mManager.requestPermission(device,
                                    mPermissionIntent);

                            requested = true;
                            break;
                        }
                    }
                }

                if (!requested) {

                    // Enable open button
                    //mOpenButton.setEnabled(true);
                }

                // Get slot number
                //int slotNum = mSlotSpinner.getSelectedItemPosition();

                // Get control code
                int controlCode=3500;

                // Set parameters
                TransmitParams params = new TransmitParams();
                params.slotNum = 0;
                params.controlCode = 3500;
                params.commandString = "FFCA000000";
                APDUcmdType = "getUID";

                // Transmit control command
                //logMsg("Slot " + slotNum+ ": Transmitting control command (Control Code: " + params.controlCode + ")...");
                new TransmitTask().execute(params);
                /*
                // store KeyA in reader 
                // Set parameters
                TransmitParams params = new TransmitParams();
                params.slotNum = 0;
                params.controlCode = 3500;
                //params.commandString = "FF82000006909F44D931FB";
                params.commandString = "FF82000006" + authKeyA;
                Log.i(TAG, params.commandString);
                APDUcmdType = "storeKeyA";

                // Transmit control command
                logMsg("Slot " + slotNum
                        + ": Transmitting control command (Control Code: "
                        + params.controlCode + ")...");
                new TransmitTask().execute(params);
                
                // store KeyB in reader 
                // Set parameters
                params = new TransmitParams();
                params.slotNum = 0;
                params.controlCode = 3500;
                //params.commandString = "FF8200010644D931FB61B1";
                params.commandString = "FF82000106" + authKeyB;
                Log.i(TAG, params.commandString);
                APDUcmdType = "storeKeyB";

                // Transmit control command
                logMsg("Slot " + slotNum
                        + ": Transmitting control command (Control Code: "
                        + params.controlCode + ")...");
                new TransmitTask().execute(params);
                

                // Authenticate using KeyA
                // Set parameters
                params = new TransmitParams();
                params.slotNum = 0;
                params.controlCode = 3500;
                params.commandString = "FF8600000501000C6101";
                APDUcmdType = "auth";

                // Transmit control command
                logMsg("Slot " + slotNum
                        + ": Transmitting control command (Control Code: "
                        + params.controlCode + ")...");
                new TransmitTask().execute(params);

                
                // Read Card ID
                // Set parameters
                params = new TransmitParams();
                params.slotNum = 0;
                params.controlCode = 3500;
                params.commandString = "FFB0000C08";
                APDUcmdType = "readID";

                // Transmit control command
                logMsg("Slot " + slotNum
                        + ": Transmitting control command (Control Code: "
                        + params.controlCode + ")...");
                new TransmitTask().execute(params);

                
                // Read Card Balance
                // Set parameters
                params = new TransmitParams();
                params.slotNum = 0;
                params.controlCode = 3500;
                params.commandString = "FFB1000D04";
                APDUcmdType = "readBal";

                // Transmit control command
                logMsg("Slot " + slotNum
                        + ": Transmitting control command (Control Code: "
                        + params.controlCode + ")...");
                new TransmitTask().execute(params);

                
                // Read Card txn counter
                // Set parameters
                params = new TransmitParams();
                params.slotNum = 0;
                params.controlCode = 3500;
                params.commandString = "FFB1000E04";
                APDUcmdType = "readTxnCtr";

                // Transmit control command
                logMsg("Slot " + slotNum
                        + ": Transmitting control command (Control Code: "
                        + params.controlCode + ")...");
                new TransmitTask().execute(params);
                
            }
        });*/
        
    }
    
    public void closeReader() {

        //close reader
        new CloseTask().execute();
    }
    
    public void InitializeReader() {
    	/*
    	boolean requested = false;
        String deviceName = "/dev/bus/usb/002/00";
        String deviceName2 = "";
        int count = -1;
        if (deviceName != null) {
        	for (int i=0;i<=50;i++)
        	{
        	count++;
        	deviceName2 = deviceName + String.valueOf(count);
            // For each device
            for (UsbDevice device : mManager.getDeviceList().values()) {

                // If device name is found
                if (deviceName.equals(device.getDeviceName())) {

                    // Request permission
                    mManager.requestPermission(device,
                            mPermissionIntent);

                    requested = true;
                    break;
                }
            }
        	}
        }*/

    	//String deviceName = "/dev/bus/usb/002/002";
    	String deviceName = "";

        for (UsbDevice device : mManager.getDeviceList().values()) {
            if (mReader.isSupported(device)) {
            	deviceName = device.getDeviceName();
            }
        }
    	
        if (deviceName != null) {

            // For each device
            for (UsbDevice device : mManager.getDeviceList().values()) {

                // If device name is found
                if (deviceName.equals(device.getDeviceName())) {

                    // Request permission
                    mManager.requestPermission(device,
                            mPermissionIntent);

                    //requested = true;
                    break;
                }
            }
        }
	}

    @Override
    protected void onDestroy() {

        // Close reader
        mReader.close();

        // Unregister receiver
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    /**
     * Logs the message.
     * 
     * @param msg
     *            the message.
     */
    private void logMsg(String msg) {

        DateFormat dateFormat = new SimpleDateFormat("[dd-MM-yyyy HH:mm:ss]: ");
        Date date = new Date();
        //String oldMsg = mResponseTextView.getText().toString();
/*
        mResponseTextView
                .setText(oldMsg + "\n" + dateFormat.format(date) + msg);

        if (mResponseTextView.getLineCount() > MAX_LINES) {
            mResponseTextView.scrollTo(0,
                    (mResponseTextView.getLineCount() - MAX_LINES)
                            * mResponseTextView.getLineHeight());
        }
        */
        Log.v(TAG, msg);
    }

    /**
     * Logs the contents of buffer.
     * 
     * @param buffer
     *            the buffer.
     * @param bufferLength
     *            the buffer length.
     */
    private void logBuffer(byte[] buffer, int bufferLength) {

        String bufferString = "";

        for (int i = 0; i < bufferLength; i++) {

            String hexChar = Integer.toHexString(buffer[i] & 0xFF);
            if (hexChar.length() == 1) {
                hexChar = "0" + hexChar;
            }

            if (i % 16 == 0) {

                if (bufferString != "") {

                    logMsg(bufferString);
                    bufferString = "";
                }
            }

            bufferString += hexChar.toUpperCase() + " ";
        }

        if (bufferString != "") {
            logMsg(bufferString);
        }
    }
        
    private String byteToString(byte[] buffer, int bufferLength) {

        String bufferString = "";

        for (int i = 0; i < bufferLength; i++) {

            String hexChar = Integer.toHexString(buffer[i] & 0xFF);
            if (hexChar.length() == 1) {
                hexChar = "0" + hexChar;
            }

            if (i % 16 == 0) {

                if (bufferString != "") {

                    logMsg(bufferString);
                    bufferString = "";
                }
            }

            bufferString += hexChar.toUpperCase() + " ";
        }
        return bufferString;
    }

    /**
     * Converts the HEX string to byte array.
     * 
     * @param hexString
     *            the HEX string.
     * @return the byte array.
     */
    private byte[] toByteArray(String hexString) {

        int hexStringLength = hexString.length();
        byte[] byteArray = null;
        int count = 0;
        char c;
        int i;

        // Count number of hex characters
        for (i = 0; i < hexStringLength; i++) {

            c = hexString.charAt(i);
            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
                    && c <= 'f') {
                count++;
            }
        }

        byteArray = new byte[(count + 1) / 2];
        boolean first = true;
        int len = 0;
        int value;
        for (i = 0; i < hexStringLength; i++) {

            c = hexString.charAt(i);
            if (c >= '0' && c <= '9') {
                value = c - '0';
            } else if (c >= 'A' && c <= 'F') {
                value = c - 'A' + 10;
            } else if (c >= 'a' && c <= 'f') {
                value = c - 'a' + 10;
            } else {
                value = -1;
            }

            if (value >= 0) {

                if (first) {

                    byteArray[len] = (byte) (value << 4);

                } else {

                    byteArray[len] |= value;
                    len++;
                }

                first = !first;
            }
        }

        return byteArray;
    }

    /**
     * Converts the integer to HEX string.
     * 
     * @param i
     *            the integer.
     * @return the HEX string.
     */
    private String toHexString(int i) {

        String hexString = Integer.toHexString(i);
        if (hexString.length() % 2 != 0) {
            hexString = "0" + hexString;
        }

        return hexString.toUpperCase();
    }

    /**
     * Converts the byte array to HEX string.
     * 
     * @param buffer
     *            the buffer.
     * @return the HEX string.
     */
    private String toHexString(byte[] buffer) {

        String bufferString = "";

        for (int i = 0; i < buffer.length; i++) {

            String hexChar = Integer.toHexString(buffer[i] & 0xFF);
            if (hexChar.length() == 1) {
                hexChar = "0" + hexChar;
            }

            bufferString += hexChar.toUpperCase() + " ";
        }

        return bufferString;
    }
}
