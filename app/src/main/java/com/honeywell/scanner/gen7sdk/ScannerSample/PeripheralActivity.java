
package com.honeywell.scanner.gen7sdk.ScannerSample;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import com.honeywell.scanner.sdk.bt.BTDevice;
import com.honeywell.scanner.sdk.common.DeviceBTConnection;
import com.honeywell.scanner.sdk.common.commonsetting.BarcodeConfiguration;
import com.honeywell.scanner.sdk.common.seychellesetting.DeviceButtonSetting;
import com.honeywell.scanner.sdk.common.seychellesetting.FontSizeSetting;
import com.honeywell.scanner.sdk.common.seychellesetting.DeviceTextColorSetting;
import com.honeywell.scanner.sdk.common.seychellesetting.DeviceTextSetting;
import com.honeywell.scanner.sdk.common.seychellesetting.DeviceLanguageSetting;
import com.honeywell.scanner.sdk.common.commonsetting.ScanningBarcode;
import com.honeywell.scanner.sdk.common.seychellesetting.TextFontSize;
import com.honeywell.scanner.sdk.common.commonsetting.DeviceConfiguration;
import com.honeywell.scanner.btsdk.R;
import android.os.Handler;
import com.honeywell.scanner.sdk.common.*;
import com.honeywell.scanner.sdk.dataparser.BarcodeData;
import com.honeywell.scanner.sdk.dataparser.BtnNotifyData;
import com.honeywell.scanner.sdk.dataparser.MenuData;
import android.os.Message;

import java.util.Timer;
import java.util.TimerTask;

public class PeripheralActivity extends Activity implements ConnectionCallback{

	private static final int MAX_RECEIVED_TEXT_LENGTH = 512;
	private static final String TAG 				  = "PeripheralActivity";

	private BTDevice mPeripheral;
	private TextView 		mMainTitleView;
	private TextView 		mSubTitleView;
	private Button 			mButtonConnect;
	private Button 			mButtonDisconnect;
	private Button 			mButtonScan;
	private Button 			mButtonClear;
    private Button 			mButtonFlashFirmware;
	private TextView		mReceivedText;
	private ScrollView		mReceivedDataScrollView;
    private CheckBox        mcheckBox128K;
	private String          mErrorMessage;
	private String          mText = "";
	private AlertDialog     mConnectingDialog;
	private DeviceBTConnection seyBTConM ;
	private DeviceSetting m_DeviceSetting = new DeviceSetting();
	FlashReturnCodes flashReturnCode;
	private boolean flashProgressState = false;
	BRIReader mBRIReader;
	private Timer mPollTimer = null;
	private TimerTask mPollTimerTask = null;
	private boolean mPollingTags = false;
	private Button mButtonStartRead, mButtonStopRead,mButtonSingleShotRead,mButtonEventRead,mButtonContinuousEventRead,mButtonGetAttributes;
	//******************************************************************************
	// Activity overrides 
	//******************************************************************************

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");

		super.onCreate(savedInstanceState);

		seyBTConM = DeviceConnectionManager.getBTConnectionInstance();

		seyBTConM.addConnectionListener(this);
		setContentView(R.layout.activity_peripheral);
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
			return insets;
		});
		// Show the Up button in the action bar.
		setupActionBar();
		connectViews();
		connectPeripheral();
		updateUIState();
		displayVersionNumber();
        displaySDKVersion();
		m_DeviceSetting = loadSetting();

	}
	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		getMenuInflater().inflate( R.menu.peripheral , menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_setting) {
			if (seyBTConM.getConnectionState() == ConnectStatus.STATE_CONNECTED) {
				Log.d(TAG, "onSettingButtonPressed");
				Intent intent = new Intent(PeripheralActivity.this, SettingActivity.class);
				startActivity(intent);
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
	//	seyBTConM.addConnectionListener(this);
		super.onResume();

		if(	seyBTConM.getConnectionState() == ConnectStatus.STATE_CONNECTED)
		{
			m_DeviceSetting = 	loadSetting();
			configureDevice();
		}
	}
	
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		seyBTConM.disconnectDevice();
		seyBTConM.removeConnectionListener(this);
		super.onDestroy();
	}

	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what){
				case 1:
					if((mConnectingDialog != null) && mConnectingDialog.isShowing())
					{
						mConnectingDialog.dismiss();
					};

					updateUIState();
					break;
				default:
					break;
			}
		}
	};
	//******************************************************************************
	// UI event handlers 
	//******************************************************************************
	
	public void onConnectButtonPressed(View sender) {
		Log.d(TAG, "onConnectButtonPressed");

		try {
            mButtonConnect.setEnabled(false);
			showConnectionMessage();

			seyBTConM.connectDevice(mPeripheral);
		}
		catch (Exception ex) {
			Log.e(TAG,"! Connect peripheral failed, "+ ex.toString());
		}
		updateUIState();
	}

	public void onReadEventButtonPressed(View sender) {
		try {
			if (mBRIReader.startReadingTags(BRIReader.TagReportOptions.EVENT)) {
				displayMessage("EVENT read started.");
				mButtonStopRead.setEnabled(true);
			} else {
				displayMessage("Failed to start EVENT read.");
			}

		}
		catch(Exception ex)
		{
			Log.e(TAG,"Reading tag as an event, "+ ex.toString());
		}
	}

	public void onContinuousReadEventButtonPressed(View sender) {

		try {
			if (mBRIReader.startReadingTags(BRIReader.TagReportOptions.EVENTALL)) {
				displayMessage("Continuous EVENTALL read started.");
				mButtonStopRead.setEnabled(true);
			} else {
				displayMessage("Failed to start continuous EVENTALL read.");
			}
		}
		catch(Exception ex)
		{
			Log.e(TAG,"Reading tag as an continuous event, "+ ex.toString());
		}
	}

	public void onGetAttributesButtonPressed(View sender) {
		try {

			String strAttributes = "FIELDSEP: " + mBRIReader.attributes.getFieldSeparator() + "\r\n";
			//strAttributes = strAttributes + "FIELDSTRENGTH:" + mBRIReader.attributes.getFieldStrength().toString() + "\r\n";
			strAttributes = strAttributes + "QUERYTARGET: " + mBRIReader.attributes.getQueryTarget() + "\r\n";

			ReaderAttributes.AntennaPair[] antpair =  mBRIReader.attributes.getAntennas() ;

			strAttributes = strAttributes + "ANTS: ";

			for (int i = 0; i < antpair.length; i++)
			{
				strAttributes = strAttributes + antpair[i].toString();
			}

			strAttributes = strAttributes + "\r\n";

			int[] fieldstrength =  mBRIReader.attributes.getFieldStrengthDB() ;

			strAttributes = strAttributes + "FIELDSTRENGTH DB: ";

			for (int i = 0; i < fieldstrength.length; i++)
			{
				strAttributes = strAttributes + fieldstrength[i] + ",";
			}

			strAttributes = strAttributes.substring(0, strAttributes.length() - 1);

			strAttributes = strAttributes + "\r\n";

			strAttributes = strAttributes + "IDREPORT: " + mBRIReader.attributes.getIDREPORT() + "\r\n";


			if(mBRIReader.attributes.isNoTagReport()) {
				strAttributes = strAttributes + "NOTAGRPT: ON" +"\r\n";
			}
			else
			{
				strAttributes = strAttributes + "NOTAGRPT: OFF" +"\r\n";
			}

			if(mBRIReader.attributes.isDenseReaderMode()) {
				strAttributes = strAttributes + "DENSEREADERMODE: ON" +"\r\n";
			}
			else
			{
				strAttributes = strAttributes + "DENSEREADERMODE: OFF" +"\r\n";
			}

			strAttributes = strAttributes + "TAGTYPE: " + mBRIReader.attributes.getTAGTYPE() + "\r\n";

			strAttributes = strAttributes + "CHKSUM : " + mBRIReader.attributes.getCheckSum() + "\r\n";

			strAttributes = strAttributes + "TIMEOUTMODE : " + mBRIReader.attributes.getTimeoutMode() + "\r\n";

			strAttributes = strAttributes + "TTY : " + mBRIReader.attributes.getTTY() + "\r\n";

			strAttributes = strAttributes + "ECHO : " + mBRIReader.attributes.getECHO() + "\r\n";

			strAttributes = strAttributes + "XONXOFF : " + mBRIReader.attributes.getXonXoff() + "\r\n";

			strAttributes = strAttributes + "RDTRIES : " + mBRIReader.attributes.getRDTRIES() + "\r\n";

			strAttributes = strAttributes + "IDTRIES : " + mBRIReader.attributes.getIDTRIES() + "\r\n";

			strAttributes = strAttributes + "ANTTRIES : " + mBRIReader.attributes.getANTTRIES() + "\r\n";

			strAttributes = strAttributes + "WRTRIES : " + mBRIReader.attributes.getWRTRIES() + "\r\n";

			strAttributes = strAttributes + "LOCKTRIES : " + mBRIReader.attributes.getLOCKTRIES() + "\r\n";

			strAttributes = strAttributes + "SELTRIES : " + mBRIReader.attributes.getSELTRIES() + "\r\n";

			strAttributes = strAttributes + "UNSELTRIES : " + mBRIReader.attributes.getUNSELTRIES() + "\r\n";

			strAttributes = strAttributes + "INITTRIES : " + mBRIReader.attributes.getINITTRIES() + "\r\n";

			strAttributes = strAttributes + "IDTIMEOUT : " + mBRIReader.attributes.getIDTimeout() + "\r\n";

			strAttributes = strAttributes + "RPTTIMEOUT : " + mBRIReader.attributes.getRPTTimeout() + "\r\n";

			strAttributes = strAttributes + "ANTTIMEOUT : " + mBRIReader.attributes.getANTTimeout() + "\r\n";

			strAttributes = strAttributes + "INITIALQ : " + mBRIReader.attributes.getINITIALQ() + "\r\n";

			strAttributes = strAttributes + "QUERYSEL : " + mBRIReader.attributes.getQuerySel() + "\r\n";

			strAttributes = strAttributes + "SESSION : " + mBRIReader.attributes.getSession() + "\r\n";

			strAttributes = strAttributes + "EPCC1G2PARAMETERS : " + mBRIReader.attributes.getEPCC1G2PARAMETERS() + "\r\n";

			strAttributes = strAttributes + "SCHEDULEOPT : " + mBRIReader.attributes.getScheduleOpt() + "\r\n";

			strAttributes = strAttributes + "BAUD : " + mBRIReader.attributes.getBaud() + "\r\n";

			strAttributes = strAttributes + "BROADCASTSYNC : " + mBRIReader.attributes.getBroadcastSync() + "\r\n";

			strAttributes = strAttributes + "UTCTIME : " + mBRIReader.attributes.getUtcTime() + "\r\n";

			displayMessage(strAttributes);
		}
		catch(Exception ex)
		{
			Log.e(TAG,"Reading tag as an event, "+ ex.toString());
		}
	}

	public void onDisconnectButtonPressed(View sender) {
		Log.d(TAG, "onDisconnectButtonPressed");
		try {
			mButtonDisconnect.setEnabled(false);
			mButtonFlashFirmware.setEnabled(false);
			mButtonScan.setEnabled(false);
			mButtonClear.setEnabled(false);
			mButtonStartRead.setEnabled(false);
			mButtonStopRead.setEnabled(false);
			mButtonEventRead.setEnabled(false);
			mButtonContinuousEventRead.setEnabled(false);
			mButtonSingleShotRead.setEnabled(false);
			mButtonGetAttributes.setEnabled(false);
			seyBTConM.disconnectDevice();
		}
		catch ( Exception ex){
			Log.e(TAG,"! Disconnect peripheral failed, "+ ex.toString());
		}
	}

	public void onScanButtonPressed(View sender) {
		Log.d(TAG, "onSendButtonPressed");

		try {
			ScanningBarcode scanning = new ScanningBarcode();
			seyBTConM.setConfiguration(scanning);
		} catch (Exception ex) {
			Log.e(TAG,ex.toString() );
		}
	}
	private void displayMessage(final String msg)
	{
		//mEditTextInfo.append(msg);
		//mEditTextInfo.append(NEW_LINE);
		updateText(msg);
		updateText("\n");
	}

	private void displayMessageFromThread(final String msg)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run()
			{
				displayMessage(msg);
			}
		});
	}
	/**
	 * Works for strings of up to 5 characters
	 */
	public boolean containsChar(String s, char search) {

		try {
			if (s.charAt(0) == 'Z') return true;
			if (s.charAt(1) == '2') return true;

			if (s.charAt(3) == 'H') return true;

		} catch (IndexOutOfBoundsException e) {
			// this should never happen...
			return false;
		}
		return false;
	}
	private void displayTags(final Tag[] tags)
	{
		for (int i=0; i<tags.length; i++)
		{
			String strTag = tags[i].toString();

			if(containsChar(strTag,'Z') && containsChar(strTag,'2') && containsChar(strTag,'H')) {
				strTag = strTag.substring(4);
				displayMessage("Tag Key: " + strTag);
			}
			else
			displayMessage("Tag Key: " + tags[i].toString());
		}
	}

	private void displayTagsFromThread(final Tag[] tags)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run()
			{
				displayTags(tags);
			}
		});
	}
	public void onStopReadButtonClicked (View view)
	{
		if (mPollingTags)
		{
			mPollingTags = false; // Cause the poll timer to stop.
		}
		if (mBRIReader != null)
		{
			try
			{
				mBRIReader.stopReadingTags();
				mButtonStopRead.setEnabled(false);
				mButtonStartRead.setEnabled(true);
				displayMessage("read stopped.");
			}
			catch (Exception e)
			{
				displayMessage("Exception: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	public void onSingleShotReadButtonPressed(View sender) {
		Log.d(TAG, "onSingleShotReadButtonPressed");

		try {

			//Get singleshot
			//if(mBRIReader.startReadingTags(BRIReader.TagReportOptions.EVENT))
			if(mBRIReader.read())
			{
				displayTags(mBRIReader.tags);
			}
			else
			{
				displayMessage("Failed to read tags.");
			}

		} catch (Exception ex) {
			Log.e(TAG,ex.toString() );
		}
	}
	/**
	 * Create a timer and timer task to poll tags. A beep is played
	 * if the poll returns at least 1 tag.
	 */
	private void PollTagsWithTimer()
	{
		mPollTimer = new Timer();
		mPollTimerTask = new TimerTask()
		{
			public void run()
			{
				if (mPollingTags)
				{
					try
					{
						if (mBRIReader.pollTags())
						{
							displayTagsFromThread(mBRIReader.tags);
						}
						else
						{
							displayMessage("Failed to read tags.");
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
				else
				{
					if (mPollTimer != null)
					{
						// Stop the timer
						mPollTimer.cancel();
						mPollTimer = null;
					}
				}
			}
		};

		// Schedule the timer. The timer task runs every 100 ms.
		mPollTimer.schedule(mPollTimerTask, 0, 100);
	}
	public void onContinuousPollReadButtonPressed(View sender) {
		Log.d(TAG, "onContinuousPollReadButtonPressed");

		try {
			//mBRIReader.startReadingTags(BRIReader.TagReportOptions.POLL);
			if (mBRIReader.startReadingTags(BRIReader.TagReportOptions.POLL)) {
				displayMessage("Continuous POLL read started.");
				mButtonStartRead.setEnabled(false);
				mButtonStopRead.setEnabled(true);
				mPollingTags = true;
				PollTagsWithTimer();
			}
			else
			{
				displayMessage("Failed to start continuous POLL read.");
			}

		} catch (Exception ex) {
			Log.e(TAG,ex.toString() );
		}
	}

	public void onClearButtonPressed(View sender) {
		mReceivedText.setText("");
	}

	@Override
	public void onBackPressed() {
		if(flashProgressState == false)
			super.onBackPressed();
		else
			updateText("\nBack navigation disabled during firmware flash procedure.");
	}

	class Task implements Runnable {
		@Override
		public void run() {
			int sleepTime = 0;
			if(mcheckBox128K.isChecked())
				sleepTime = 15000;
			else
				sleepTime = 30000;

			while(flashProgressState == true)
			{
				updateText("\nFlash Progress = " + seyBTConM.getFirmwareDownlaodPercent());
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class AsyncFlashOperation extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... params) {
			flashReturnCode = seyBTConM.flashFirmwareSeychelles(Environment.getExternalStorageDirectory().getPath() + "/Firmware.smoc");
			return "Executed";
		}

		@Override
		protected void onPreExecute() {
			Log.d("HWFLASHLOG", "flashFirmwareSeychelles - Scanner Sample - Start");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					flashProgressState = true;
					mButtonConnect.setEnabled(false);
					mButtonDisconnect.setEnabled(false);
					mButtonScan.setEnabled(false);
					mButtonFlashFirmware.setEnabled(false);
					mButtonClear.setEnabled(false);
					mcheckBox128K.setEnabled(false);
					updateText("\nFirmware flash initiated. \nFile transfer will be faster with 128K buffer size.");
					if(mcheckBox128K.isChecked())
						seyBTConM.setFwTransferBufferSize(128);
					else
						seyBTConM.setFwTransferBufferSize(1);
					new Thread(new Task()).start();
				}
			});
		}

		@Override
		protected void onPostExecute(String result) {
			Log.d("HWFLASHLOG", "flashFirmwareSeychelles - Scanner Sample - End");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					flashProgressState = false;
					mButtonConnect.setEnabled(true);
					mButtonDisconnect.setEnabled(false);
					mButtonFlashFirmware.setEnabled(false);
					mButtonScan.setEnabled(false);
					mButtonClear.setEnabled(false);
					mcheckBox128K.setEnabled(false);
					if(flashReturnCode == FlashReturnCodes.FINISH_SUCCESSFUL)
						updateText("\nFirmware transfer success. \nScanner flashes the firmware and reboot. \nReconnect the scanner.");

					else
						updateText("\nFirmware transfer failed. \nError code:" + flashReturnCode + ".\nReconnect the scanner.");
					seyBTConM.disconnectDevice();
				}
			});
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			updateText("\nFirmware flash progress: " + seyBTConM.getFirmwareDownlaodPercent());
		}
	}

    public void onFlashFirmwareButtonPressed(View view) {

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
		}

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
			AsyncFlashOperation asyncFlashOperation = new AsyncFlashOperation();
			asyncFlashOperation.execute("");
		}
		else
		{
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
			updateText("Application requires storage permission. Allow when prompted and retry Flash Firmware.");
		}
    }
	//******************************************************************************
	// TIOPeripheralCallback implementation 
	//******************************************************************************
    @Override
    public void onConnectionStatus(final int status, final String Message)
    {
        if(status == ConnectStatus.STATE_CONNECTED)
        {
            Log.d(TAG, "onConnected");

            Message message = mHandler.obtainMessage();
            message.what = 1;
            mHandler.sendMessage(message);
            configureDevice();
            try {
				 mBRIReader = new BRIReader(seyBTConM);
			}
            catch(BasicReaderException e)
			{
				e.printStackTrace();
			}

        }
        else if(status == ConnectStatus.STATE_CONNECTED_FAILED)
        {
            Log.d(TAG, "onConnectFailed " + Message);

            if((mConnectingDialog != null) && mConnectingDialog.isShowing()) { mConnectingDialog.dismiss(); };

            mErrorMessage = Message;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    updateUIState();
                }
            };
            runOnUiThread( runnable );
            if (mErrorMessage.length() > 0) {
                updateText("Failed to connect with error message: " + mErrorMessage +"\r\n"); }
        }
        else if(status == ConnectStatus.STATE_DISCONNECTED)
        {
            Log.d(TAG, "onDisconnected" + Message);

            if((mConnectingDialog != null) && mConnectingDialog.isShowing()) { mConnectingDialog.dismiss(); };
            mErrorMessage = Message;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    updateUIState();
                }
            };
            runOnUiThread(runnable);
            updateText("\nScanner disconnected" + "\r\n");
        }
    }
    @Override
  public  void  onBarcodeDataReceived(final BarcodeData barcodeData)
    {
        Log.d(TAG, "onBarcodeDataReceived ");

        try {
            String  strBarcode = new String(BarcodeData.mRealBarCode);
            String  str_Code_Id = new String(String.valueOf(barcodeData.mCodeId));
			String  str_AimId = new String(String.valueOf(barcodeData.mAimID));
			str_AimId = "]" + str_AimId;
            updateText("\r\nCode Id: " + str_Code_Id + "\r\n" + "AIM Id: " + str_AimId +"\r\n" + "Barcode Result: " + strBarcode + "\r\n");

            /*
            //TODO - display the barcode on scanner display and calculate time taken
			long startTime = 0, elapsedTime = 0;
			startTime = System.currentTimeMillis();

			DeviceTextSetting text = new DeviceTextSetting(strBarcode, m_DeviceSetting.m_TextLineType);
			seyBTConM.setConfiguration(text);

			elapsedTime = ((System.currentTimeMillis() - startTime));
			updateText("Scanner display updated with the Barcode Result: " + strBarcode + " in " + elapsedTime + " milli seconds." + "\r\n");
			*/

        } catch (Exception ex) {}

    }
    @Override
    public void  OnButtonPressedReceived(final BtnNotifyData btnData)
    {
       Log.d(TAG, "OnButtonPressedReceived ");
        try {
            String str = "";
            if(btnData.mBtnNofication == BtnNotifyData.BTN_LEFTBUTTON_PRESSED) {
                str = new String("Left button pressed!" + "\r\n");
            }
            else if(btnData.mBtnNofication == BtnNotifyData.BTN_RIGHTBUTTON_PRESSED)
            {
				str = new String("Right button pressed!" + "\r\n");
            }
            else if(btnData.mBtnNofication == BtnNotifyData.BTN_BOTHBUTTON_PRESSED)
            {
				str = new String("Both button pressed!" + "\r\n");
            }
            updateText(str);

        } catch (Exception ex) {}

    }

	@Override
	public void onDataTransmitted(final int status, final int bytesTransferred) {
		Log.d(TAG,"onDataTransmitted" );
	}
	@Override
	public void onUnProcessedDataReceived(byte[] data) {
		Log.d(TAG,"onUnProcessedDataReceived" );
	}
	@Override
	public void  onGetScannerInfo(final DeviceInfo scannerInfo){
		Log.d(TAG,"onGetScannerInfo " + DeviceInfo.mStrName);
        String str_serial_num = "Serial Number: " + DeviceInfo.mStrSerialNum + "\r\n";
        String str_app_version = "App Version: " + DeviceInfo.mStrAppVersion + "\r\n";
        String str_app_data = "App Data: " + DeviceInfo.mStrAppDate + "\r\n";
        String str_boot_version = "Boot Version: " + DeviceInfo.mStrBootVersion + "\r\n";
        String str_bsp_version = "BSP Version: " + DeviceInfo.mStrBSPVersion + "\r\n";
        String str = "\r\nScanner Connected.\r\n" + str_serial_num + str_app_version + str_app_data + str_boot_version + str_bsp_version;
        updateText(str);
	}
	@Override
	public void  onMenuCommandReceived( MenuData menuData )
	{
		MenuData data = menuData;
		int type = data.mDataType;
	    // here, check the menu command response are valid
	}
	//******************************************************************************
	// Internal methods 
	//******************************************************************************
	
	private void connectViews() {
		Log.d(TAG, "connectViews");
		
		mMainTitleView = (TextView) findViewById(R.id.mainTitle);
		mSubTitleView = (TextView) findViewById(R.id.subTitle);
		mButtonConnect = (Button) findViewById(R.id.connectButton);
		mButtonDisconnect = (Button) findViewById(R.id.disconnectButton);
		mButtonScan = (Button) findViewById(R.id.ScanButton);
		mButtonClear = (Button) findViewById(R.id.clearButton);
		mReceivedText = (TextView) findViewById(R.id.receivedDataTextView);
		mReceivedDataScrollView = (ScrollView) findViewById(R.id.receivedDataScrollView);
        mcheckBox128K = (CheckBox) findViewById(R.id.checkBoxBufferSize);
        mButtonFlashFirmware = (Button) findViewById(R.id.btnFlashFirmware);
		mButtonStartRead = findViewById(R.id.ContinuousPollButton);
		mButtonStopRead = findViewById(R.id.buttonStopRead);
		mButtonSingleShotRead = findViewById(R.id.SingleShotReadButton);
		mButtonEventRead = findViewById(R.id.btnReadEvent);
		mButtonContinuousEventRead = findViewById(R.id.btnContinuousReadEvent);
		mButtonGetAttributes = findViewById(R.id.btnGetAttributes);
		mButtonScan.setEnabled(false);
		mButtonClear.setEnabled(false);

		mReceivedText.setText("");
		mReceivedText.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//mButtonClear.setEnabled( mReceivedText.length() > 0);
			}
		});
	}
	
	private void connectPeripheral() {
		// extract peripheral id (address) from intent
		Intent intent = getIntent();
		String address = intent.getStringExtra(ScannerSample.PERIPHERAL_ID_NAME);
		if(address != null) {
			mPeripheral = seyBTConM.findPeripheral(address);
		}
		if ( mPeripheral != null) {
			// register callback
			try {
				// display peripheral properties
				mMainTitleView.setText(mPeripheral.getName() + "  " + mPeripheral.getAddress());
				mSubTitleView.setText( "" );
			}
			catch (Exception ex ){
				Log.e(TAG,"! Connect to peripheral failed, "+ ex.toString());
			}
		}
	}
	private void updateUIState() {
		Log.d(TAG, "updateUIState");
		
		boolean isConnected = false;
		boolean isConnecting = false;
		isConnected  =  seyBTConM.getConnectionState() == ConnectStatus.STATE_CONNECTED;
		isConnecting =  seyBTConM.getConnectionState() == ConnectStatus.STATE_CONNECTING;

		int visibility = isConnected ? View.VISIBLE : View.INVISIBLE;

		mButtonConnect.setEnabled(!isConnected && !isConnecting);
		mButtonDisconnect.setEnabled(isConnected);
        mButtonFlashFirmware.setEnabled(isConnected);
		mButtonScan.setEnabled(isConnected);
		mButtonClear.setEnabled(isConnected);
		mcheckBox128K.setEnabled(isConnected);
		mButtonStartRead.setEnabled(isConnected);
		mButtonStopRead.setEnabled(isConnected);
		mButtonEventRead.setEnabled(isConnected);
		mButtonContinuousEventRead.setEnabled(isConnected);
		mButtonSingleShotRead.setEnabled(isConnected);
		mButtonGetAttributes.setEnabled(isConnected);
	}
	private void updateText(final String str) {
		Log.d(TAG, "updateText");

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				mReceivedText.append( str );

				// limit view's text length to MAX_RECEIVED_TEXT_LENGTH
				if (mReceivedText.length() > PeripheralActivity.MAX_RECEIVED_TEXT_LENGTH + 3) {
					String text = mReceivedText.getText().toString();
					text = "..." +  text.substring(text.length() - (PeripheralActivity.MAX_RECEIVED_TEXT_LENGTH + 3));
					mReceivedText.setText(text);
				}

				// scroll text view to bottom
				mReceivedDataScrollView.post(new Runnable() {
					@Override
					public void run() {
						mReceivedDataScrollView.scrollTo(0, mReceivedText.getBottom());
					}
				});
			}
		};
		runOnUiThread(runnable);
	}
	private void displaySDKVersion()
    {
       String str =  SdkVersion.getVersion();
       updateText("SDK Version : " + str + "\r\n");
    }
	private void displayVersionNumber() {
        PackageInfo packageInfo;
        String version = "";
		try {
			packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
	        version = packageInfo.versionName;
		} catch (Exception ex) {
			Log.e(TAG,ex.toString());
		}
		setTitle(getTitle() + " " + version);
	}
	private DeviceSetting loadSetting()
	{
		SharedPreferences app_preferences =
				PreferenceManager.getDefaultSharedPreferences(this);

		// line type
		m_DeviceSetting.m_TextLineType = DeviceTextSetting.TextLineType.UpLine;
		m_DeviceSetting.m_FontSizeLineType = FontSizeSetting.TextLineType.UpLine;
		String str_text_line = app_preferences.getString("list_text_line_preference","Bottom Line");
	    if(str_text_line.equals("Bottom Line"))
		{
			m_DeviceSetting.m_TextLineType = DeviceTextSetting.TextLineType.BottomLine;
			m_DeviceSetting.m_FontSizeLineType = FontSizeSetting.TextLineType.BottomLine;
		}
		// display text
		String str_text_display = app_preferences.getString("edittext_display_preference","Sample App");
		m_DeviceSetting.m_text = str_text_display;

		String m_TextFontSize = app_preferences.getString("list_Font_size_preference","Small");
		switch (m_TextFontSize) {
			case "Small":
				m_DeviceSetting.m_TextFontSize = TextFontSize.Small;
				break;
			case "Medium":
				m_DeviceSetting.m_TextFontSize = TextFontSize.Medium;
				break;
			case "Large":
				m_DeviceSetting.m_TextFontSize = TextFontSize.Large;
				break;
			case "Extra Large":
				m_DeviceSetting.m_TextFontSize = TextFontSize.ExtraLarge;
				break;
			case "Small Bold":
				m_DeviceSetting.m_TextFontSize = TextFontSize.SmallBold;
				break;
			case "Medium Bold":
				m_DeviceSetting.m_TextFontSize = TextFontSize.MediumBold;
				break;
			case "Large Bold":
				m_DeviceSetting.m_TextFontSize = TextFontSize.LargeBold;
				break;
			case "Extra Large Bold":
				m_DeviceSetting.m_TextFontSize = TextFontSize.ExtraLargeBold;
				break;
		}

		String str_language = app_preferences.getString("list_language_preference","English");
		m_DeviceSetting.m_LanguageOption = DeviceLanguageSetting.LanguageOptions.loEnglish;
		if(str_language.equals("Cyrillic"))
		{
			m_DeviceSetting.m_LanguageOption = DeviceLanguageSetting.LanguageOptions.loCyrillic;
		}

		String str_text_color = app_preferences.getString("list_text_color_preference","DefaultColor");
		switch (str_text_color) {
			case "Background Color":
				m_DeviceSetting.m_TextColor = DeviceTextColorSetting.TextColors.DefaultColor;
				break;
			case "Red":
				m_DeviceSetting.m_TextColor = DeviceTextColorSetting.TextColors.Red;
				break;
			case "Green":
				m_DeviceSetting.m_TextColor = DeviceTextColorSetting.TextColors.Green;
				break;
			case "Blue":
				m_DeviceSetting.m_TextColor = DeviceTextColorSetting.TextColors.Blue;
				break;
		}

		String str_text_color_type = app_preferences.getString("list_text_color_type_preference","Background Color");
		switch (str_text_color_type) {
			case "Background Color":
				m_DeviceSetting.m_TextColorType = DeviceTextColorSetting.TextColorType.BgColor;
				break;
			case "Foreground Color UpLine":
				m_DeviceSetting.m_TextColorType = DeviceTextColorSetting.TextColorType.FgColorUpLine;
				break;
			case "Foreground Color Bottom":
				m_DeviceSetting.m_TextColorType = DeviceTextColorSetting.TextColorType.FgColorBottomLine;
				break;

		}

		m_DeviceSetting.m_bENA13_Enable = app_preferences.getBoolean("ENA13_checkbox_preference",false);
		m_DeviceSetting.m_bQR_Enable = app_preferences.getBoolean("QR_checkbox_preference",false);
		m_DeviceSetting.m_EnableButtonPress= app_preferences.getBoolean("button_press_checkbox_preference",false);
		return m_DeviceSetting ;
	}
	private void configureDevice()
	{
		DeviceLanguageSetting language = new DeviceLanguageSetting(m_DeviceSetting.m_LanguageOption);
		seyBTConM.setConfiguration(language);

		DeviceTextSetting text = new DeviceTextSetting(m_DeviceSetting.m_text, m_DeviceSetting.m_TextLineType);
		seyBTConM.setConfiguration(text);


		FontSizeSetting fontSetting = new FontSizeSetting(m_DeviceSetting.m_TextFontSize, m_DeviceSetting.m_FontSizeLineType);
		seyBTConM.setConfiguration(fontSetting);

		DeviceTextColorSetting text_setting = new DeviceTextColorSetting();

		text_setting.setTextColor(m_DeviceSetting.m_TextColorType, m_DeviceSetting.m_TextColor);
		seyBTConM.setConfiguration(text_setting);

		String command = "";
		if(m_DeviceSetting.m_bENA13_Enable)
		{
			command = "E13ENA1";
		}
		else
		{
			command = "E13ENA0";
		}

		if(m_DeviceSetting.m_bQR_Enable)
		{
			command += ";";
			command += "QRCENA1";
		}
		else
		{
			command += ";";
			command += "QRCENA0";
		}
		BarcodeConfiguration barcode_Configuration = new BarcodeConfiguration(command);
		seyBTConM.setConfiguration(barcode_Configuration);

		DeviceButtonSetting bc = new DeviceButtonSetting(m_DeviceSetting.m_EnableButtonPress);
		seyBTConM.setConfiguration(bc);
	}

	void showConnectionMessage() {
		final AlertDialog.Builder dialog;
		if (mConnectingDialog == null) {
			// Create dialog
			dialog = new AlertDialog.Builder(this)
					.setMessage("Connecting...")
					.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							//if ( mConnection != null ) {
								try {

									seyBTConM.cancelConnect();
								}
								catch ( Exception ex) {
									Log.e(TAG,ex.toString() );
								}
							}
					});
			mConnectingDialog = dialog.create();

			// Disable click event outside a dialog
			Window window = mConnectingDialog.getWindow();
			window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
					WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
			window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		}
		// Show dialog
		mConnectingDialog.show();
	}


}
