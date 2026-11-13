
package com.honeywell.scanner.gen7sdk.ScannerSample;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;

import android.widget.TextView;
import com.honeywell.scanner.btsdk.R;
import com.honeywell.scanner.sdk.bt.classicbt.DeviceClassicBTConnection;
import com.honeywell.scanner.sdk.common.*;
import com.honeywell.scanner.sdk.bt.BTConnectionType;
import com.honeywell.scanner.sdk.bt.BTDevice;
import com.honeywell.scanner.sdk.bt.BTDeviceFoundCallBack;
import com.honeywell.scanner.sdk.common.DeviceBTConnection;

import android.os.Message;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ManagerActivity extends Activity implements BTDeviceFoundCallBack
{
	private static final int 		ENABLE_BT_REQUEST_ID = 1;
	private static final int		SCAN_INTERVAL 		 = 8000;
	private static final String  	TAG 				 = "ScannerSample";
	private static final int PERMITIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    private static final int SERVICE_REQUEST_LOCATION = 2;
	private Button 						mScanButton;
	private Button 						mClearAllButton;
    private Button 						mStopScanButton;
	private ProgressBar 				mScanIndicator;
	private ListView 					mPeripheralsListView;
	private ArrayAdapter<BTDevice> mPeripheralList;
	private DeviceBTConnection seyBTConM ;
    private RadioButton radioBtn_BLE,radioBtn_ClassicBt,radioBtn_EZScan;
    private EditText editText_MacAddress;
    private List<BTDevice> m_ScannerList = new CopyOnWriteArrayList<BTDevice>();
	private Context context = null ;

	private final  int msg_start_scan = 1;
	private final  int msg__stop_scan = 2;
	private final  int msg_Peripheral_Found = 3;
	private final  int msg_Peripheral_Update = 4;
	private final  int msg_BLE_status_change = 5;
	private final  int msg_BT_status_change = 6;
	private final  int msg_EZScan_status_change = 7;


	public boolean checkLocationService(final @NonNull Activity activity)
	{
		if (Settings.Secure.getInt(this.getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) == 0) {
			Intent enableLocation = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			activity.startActivityForResult(enableLocation, SERVICE_REQUEST_LOCATION);
		}
		return true;
	}

	//******************************************************************************
	// Activity overrides 
	//******************************************************************************
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		
		super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

		setContentView(R.layout.activity_manager);
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
			return insets;
		});
        context = this.getApplicationContext();

		radioBtn_BLE=(RadioButton)findViewById(R.id.BLE_id);
		radioBtn_ClassicBt=(RadioButton)findViewById(R.id.classic_BT_id);
		radioBtn_EZScan = (RadioButton)findViewById(R.id.EZScan_id);
		editText_MacAddress = (EditText)findViewById(R.id.editTextMacAddress);
        DeviceConnectionManager.createBTConnectionInstance(context, BTConnectionType.BLE);

        seyBTConM = DeviceConnectionManager.getBTConnectionInstance();
		radioBtn_BLE.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
           public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            	if(isChecked){
					DeviceConnectionManager.destroy();
					DeviceConnectionManager.createBTConnectionInstance(context, BTConnectionType.BLE);
					seyBTConM = DeviceConnectionManager.getBTConnectionInstance();
                    Message message = mHandler.obtainMessage();
                    message.what = msg_BLE_status_change;
                    mHandler.sendMessage(message);
            		} }
       });
	       radioBtn_ClassicBt.setOnCheckedChangeListener(new OnCheckedChangeListener() {

	           @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                   if(isChecked) {
					   DeviceConnectionManager.destroy();
                      DeviceConnectionManager.createBTConnectionInstance(context, BTConnectionType.ClassicBT);
                      seyBTConM = DeviceConnectionManager.getBTConnectionInstance();

                       Message message = mHandler.obtainMessage();
                       message.what = msg_BT_status_change;
                       mHandler.sendMessage(message);
                   }
			   }
       });
		radioBtn_EZScan.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
					DeviceConnectionManager.destroy();
					DeviceConnectionManager.createBTConnectionInstance(context, BTConnectionType.ClassicBT);
					seyBTConM = DeviceConnectionManager.getBTConnectionInstance();

					Message message = mHandler.obtainMessage();
					message.what = msg_EZScan_status_change;
					mHandler.sendMessage(message);
				}
			}
		});

		connectViews();
		if(seyBTConM != null) {
            seyBTConM.removeAllPeripherals();
            if (! seyBTConM.isBluetoothEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST_ID);
            }
        }
		// displays a dialog requesting user permission to enable Bluetooth.
        seyBTConM.removeAllPeripherals();
		// initialize peripherals list view
		initializePeripheralsListView();
		updatePeripheralsListView();
		// initialize clearAllButton
		updateClearAllButton();
        // display version number
		displayVersionNumber();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.manager, menu);
		// We do not have any settings here
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " +resultCode );

        switch (requestCode) {
            case ENABLE_BT_REQUEST_ID:
                if (resultCode == Activity.RESULT_CANCELED) {
                    mScanButton.setEnabled(false);
                    return;
                }
                break;

            case SERVICE_REQUEST_LOCATION:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "GPS enabled now on this device");
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
	}
	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");

		DeviceConnectionManager.destroy();

		super.onDestroy();
	}
	private boolean checkAndRequestPermissions() {
		List<String> listPermissionsNeeded = new CopyOnWriteArrayList<>();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
				listPermissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
			}
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
				listPermissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
			}
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
			}
		} else {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
			}
		}

		if (!listPermissionsNeeded.isEmpty()) {
			ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), PERMITIONS_REQUEST_ACCESS_COARSE_LOCATION);
			return false;
		}
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case PERMITIONS_REQUEST_ACCESS_COARSE_LOCATION: {
				boolean allGranted = true;
				if (grantResults.length > 0) {
					for (int result : grantResults) {
						if (result != PackageManager.PERMISSION_GRANTED) {
							allGranted = false;
							break;
						}
					}
				} else {
					allGranted = false;
				}

				if (allGranted) {
					startScanInternal();
				} else {
					Log.d(TAG, "Bluetooth / Location permissions not granted");
				}
				break;
			}
		}
	}
	Handler mHandler = new Handler(){
		@Override

		public void handleMessage(Message msg) {
			switch (msg.what){
				case msg_start_scan:
					radioBtn_ClassicBt.setClickable(false);
					radioBtn_BLE.setClickable(false);
					radioBtn_EZScan.setClickable(false);
					mScanButton.setEnabled(false);
					mClearAllButton.setEnabled(false);
					mScanIndicator.setVisibility(VISIBLE);
					m_ScannerList.clear();
					break;
				case msg__stop_scan:
					radioBtn_ClassicBt.setClickable(true);
					radioBtn_BLE.setClickable(true);
					radioBtn_EZScan.setClickable(true);
					mScanIndicator.setVisibility(GONE);
					mScanButton.setEnabled(true);
                    mStopScanButton.setEnabled(false);
					updateClearAllButton();
					break;
				case msg_Peripheral_Found:
					BTDevice device = (BTDevice)msg.obj;
					m_ScannerList.add(device);
					updatePeripheralsListView();
				break;
                case msg_BLE_status_change:
                	radioBtn_ClassicBt.setChecked(false);
					radioBtn_EZScan.setChecked(false);
					mScanButton.setEnabled(true);
					editText_MacAddress.setEnabled(false);
					mStopScanButton.setEnabled(false);
					editText_MacAddress.getText().clear();
					// clear device list, make sure that user not click bt device not be used in ble instance
					m_ScannerList.clear();
					seyBTConM.removeAllPeripherals();
					  updatePeripheralsListView();

                    break;
                case msg_BT_status_change:
                    radioBtn_BLE.setChecked(false);
                    radioBtn_EZScan.setChecked(false);
					mScanButton.setEnabled(true);
					editText_MacAddress.setEnabled(false);
					mStopScanButton.setEnabled(false);
					editText_MacAddress.getText().clear();
                      // clear device list, make sure that user not click ble device not be used in ClassicBT instance
					m_ScannerList.clear();
					seyBTConM.removeAllPeripherals();
					 updatePeripheralsListView();
                    break;
				case msg_EZScan_status_change:
					radioBtn_BLE.setChecked(false);
					radioBtn_ClassicBt.setChecked(false);
					mScanButton.setEnabled(true);
					mStopScanButton.setEnabled(false);
					editText_MacAddress.setEnabled(true);

					// clear device list, make sure that user not click ble device not be used in ClassicBT instance
					m_ScannerList.clear();
					seyBTConM.removeAllPeripherals();
					updatePeripheralsListView();
					break;
				default:
					break;
			}
		}
	};
	//******************************************************************************
	// UI event handlers 
	//******************************************************************************
	
	public void onScanButtonPressed(View sender) {

		if((radioBtn_EZScan.isChecked()==true)/* && (editText_MacAddress.getText().toString()!= null)*/) {
			DeviceClassicBTConnection.MacAddress = editText_MacAddress.getText().toString();
		}
		else
		{
			DeviceClassicBTConnection.MacAddress = "";
		}
		Log.d(TAG, "onScanButtonPressed");

		if (!checkAndRequestPermissions()) {
			return;
		}

		startScanInternal();
	}

	private void startScanInternal() {
		if (seyBTConM != null) {
			if (m_ScannerList != null) {
				m_ScannerList.clear();
			}
			seyBTConM.removeAllPeripherals();
			updatePeripheralsListView();
			updateClearAllButton();
			mStopScanButton.setEnabled(true);

			seyBTConM.startScanDevice(10000, this);
		}
	}
	public void onClearAllButtonPressed(View sender) {
		Log.d(TAG, "onClearAllButtonPressed");

        if( seyBTConM != null) {
            seyBTConM.removeAllPeripherals();
			if(m_ScannerList != null) {
				m_ScannerList.clear();
			}
            updatePeripheralsListView();
            updateClearAllButton();
        }
	}
	public void onStopScanButtonPressed(View sender) {
		mStopScanButton.setEnabled(false);
		seyBTConM.stopScan();
		updateClearAllButton();
    }
	private void onRemoveButtonPressed(final BTDevice peripheral) {
		Log.d(TAG, "onRemoveButtonPressed "+ peripheral.toString());
        if( seyBTConM != null) {
            seyBTConM.removePeripheral(peripheral);
			m_ScannerList.remove(peripheral);
            updatePeripheralsListView();
            updateClearAllButton();
        }
	}

	private void onPeripheralCellPressed(BTDevice peripheral) {
		Log.d(TAG, "onPeripheralCellPressed "+ peripheral.toString());

		seyBTConM.stopScan();
		Intent intent = new Intent(ManagerActivity.this, PeripheralActivity.class);
		intent.putExtra(ScannerSample.PERIPHERAL_ID_NAME, peripheral.getAddress());
		startActivity(intent);
	}

	//******************************************************************************
	// TIOManagerCallback implementation 
	//******************************************************************************
	
	//@Override
	public void onPeripheralFound(BTDevice peripheral) {
		Log.d(TAG, "onPeripheralDiscovered " + peripheral.toString());

		Message message = mHandler.obtainMessage();
		message.what = msg_Peripheral_Found;
		message.obj = peripheral;
		mHandler.sendMessage(message);
	}
	//@Override
	public void onPeripheralUpdate(BTDevice peripheral) {
		Log.d(TAG, "onPeripheralUpdate " + peripheral.toString());
		Message message = mHandler.obtainMessage();
		message.what = msg_Peripheral_Update;
		mHandler.sendMessage(message);

	}
	//******************************************************************************
	// Internal methods 
	//******************************************************************************
	private void connectViews() {
		Log.d(TAG, "connectViews");
		mScanButton          = (Button) findViewById(R.id.scanButton);
		mClearAllButton      = (Button) findViewById(R.id.clearAllButton);
        mStopScanButton      = (Button) findViewById(R.id.stopButton);
		mScanIndicator		 = (ProgressBar) findViewById(R.id.scanIndicator);
		mPeripheralsListView = (ListView) findViewById(R.id.peripheralsListView);
		mScanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ManagerActivity.this.onScanButtonPressed(view);
			}
		});
	}
	
	private void initializePeripheralsListView() {
			
		// create data adapter for peripherals list view
		mPeripheralList = new ArrayAdapter<BTDevice>(this, 0) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				return createPeripheralCell(position);
			}
			@Override
			public int getCount() {
				if (m_ScannerList != null) {
					return m_ScannerList.size();
				}
				return 0;
			}
		};
		mPeripheralsListView.setAdapter(mPeripheralList);
	}

	private View createPeripheralCell(int position) {
		
		BTDevice peripheral = null;
            if (m_ScannerList != null) {
                if (m_ScannerList.size()  > 0) {
                    peripheral = m_ScannerList.get(position);
                }
        }
		final BTDevice device = peripheral;
	    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View peripheralCell = inflater.inflate(R.layout.peripheral_cell, mPeripheralsListView, false);
	    
	    TextView mainTitle = (TextView) peripheralCell.findViewById(R.id.mainTitle);
		mainTitle.setText(peripheral.getName() + "  " + peripheral.getAddress());
		TextView subTitle = (TextView) peripheralCell.findViewById(R.id.subTitle);
		subTitle.setText("");

		Button removeButton = (Button) peripheralCell.findViewById(R.id.removeButton);
		removeButton.setVisibility(View.INVISIBLE);
		removeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				if(device != null) {
					onRemoveButtonPressed(device);
				}
			}
		});
		peripheralCell.setOnTouchListener(createPeripheralCellGestureDetector(peripheral, removeButton));
		return peripheralCell;
	}
	//@Override
	public void onScanStatus(final int status)
	{
		if(status == 0)// start scan
		{

			Message message = mHandler.obtainMessage();
			message.what = msg_start_scan;
			mHandler.sendMessage(message);
		}
		else if(status == 1)// stop scan
		{
			Message message = mHandler.obtainMessage();
			message.what = msg__stop_scan;
			mHandler.sendMessage(message);
		}
	}

	private void updatePeripheralsListView() {
		// update adapter with currently known peripherals
		mPeripheralList.notifyDataSetChanged();
	}
	private void updateClearAllButton() {
        mClearAllButton.setEnabled(false);
            if (m_ScannerList != null) {
                if (m_ScannerList.size() > 0) {
                    mClearAllButton.setEnabled(true);
                }
            }
	}
	
	private void displayVersionNumber() {
        PackageInfo packageInfo;
        String version = "";
		try {
			packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
	        version = packageInfo.versionName;
		} catch (Exception ex) {
			Log.e(TAG,toString() );
		}
		setTitle(getTitle() + " " + version);
	}

	private STSwipeTapDetector createPeripheralCellGestureDetector(final BTDevice peripheral, final Button removeButton) {
		STSwipeTapDetector detector = new STSwipeTapDetector(this) {

			private boolean mRemoveMode;
			@Override
			public boolean onLeftSwipe() {
				if (this.mRemoveMode)
					return true; // handled

				this.mRemoveMode = true;
				TranslateAnimation animation = new TranslateAnimation(removeButton.getWidth(), 0, 0, 0);
				animation.setDuration(200);
				animation.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {
						removeButton.setVisibility(VISIBLE);
						removeButton.setEnabled(false);
					}
					@Override
					public void onAnimationEnd(Animation animation) {
						removeButton.setEnabled(true);
					}
					@Override
					public void onAnimationRepeat(Animation animation) { }
				});
				removeButton.startAnimation(animation);

				return true;
			}
			@Override
			public boolean onRightSwipe() {
				if (!this.mRemoveMode)
					return true; // handled

				TranslateAnimation animation = new TranslateAnimation(0, removeButton.getWidth(), 0, 0);
				animation.setDuration(200);
				animation.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {
						removeButton.setEnabled(false);
					}
					@Override
					public void onAnimationEnd(Animation animation) {
						removeButton.setVisibility(View.INVISIBLE);
					}
					@Override
					public void onAnimationRepeat(Animation animation) { }
				});
				removeButton.startAnimation(animation);
				this.mRemoveMode = false;
				return true;
			}
			@Override
			public boolean onTap() {
				if (this.mRemoveMode)
					return true; // handled

				ManagerActivity.this.onPeripheralCellPressed(peripheral);

				return true; // handled
			}
		};
		return detector;
	}
}
