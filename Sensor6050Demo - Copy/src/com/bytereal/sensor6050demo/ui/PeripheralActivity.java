package com.bytereal.sensor6050demo.ui;

import java.util.ArrayList;
import java.util.HashMap;

import com.bytereal.sensor6050demo.BaseApp;
import com.bytereal.sensor6050demo.Constans;
import com.bytereal.sensor6050demo.R;
import com.bytereal.sensor6050demo.logs.MyLog;
import com.bytereal.sensor6050demo.util.StringUtil;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class PeripheralActivity extends Activity implements OnItemClickListener, OnClickListener {
	private final String TAG = "PeripheralActivity";
	private static final long SCAN_PERIOD = 10000;
	private Button mScanButton;
	private ListView mDeviceListView;
	private boolean isScan = false;
	private Handler mHandler = new Handler();
	private BluetoothAdapter mBluetoothAdapter;
	//�����豸����
	private ArrayList<BluetoothDevice> mLeDevices = new ArrayList<BluetoothDevice>();
	private ArrayList<HashMap<String, Object>> listItem; // ListView������Դ������ʾ�ڽ��棬������һ��HashMap���б�
	private SimpleAdapter listItemAdapter; // ListView��������
	private static final int REQUEST_ENABLE_BT = 1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.peripheral);
		BaseApp.getInstance().addActivity(this);

		initView();
		initBlue();
		setupView();
	}

	/**
	 * ����ֻ�����
	 */
	private void initBlue() {
		// TODO Auto-generated method stub

		// ��鵱ǰ�ֻ��Ƿ�֧��ble ����,�����֧���˳�����
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
		}

		// ��ʼ�� Bluetooth adapter, ͨ�������������õ�һ���ο�����������(API����������android4.3�����ϺͰ汾)
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// ����豸���Ƿ�֧������
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
		}
	}
	@Override
	protected void onResume() {
		super.onResume();

		// Ϊ��ȷ���豸��������ʹ��, �����ǰ�����豸û����,�����Ի������û�Ҫ������Ȩ��������

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		//�ܾ���������,�˳�Ӧ��
		if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
			finish();
		}
	}
	/**
	 * �ؼ���ʼ������, ����
	 */
	private void setupView() {
		listItem = new ArrayList<HashMap<String, Object>>();
		listItemAdapter = new SimpleAdapter(this, listItem, R.layout.listview,
				new String[]{"image", "title", "text"},
				new int[]{R.id.ItemImage, R.id.ItemTitle, R.id.ItemText});
		mDeviceListView.setAdapter(listItemAdapter);

		mDeviceListView.setOnItemClickListener(this);
		mScanButton.setOnClickListener(this);
	}


	/**
	 * Ѱ�ҿؼ�
	 */
	private void initView() {
		mScanButton = (Button) findViewById(R.id.bt_scan);
		mDeviceListView = (ListView) findViewById(R.id.lv_device);
	}


	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.bt_scan:
			scanLeDevice(!isScan);
			break;

		default:
			break;
		}

	}

	private void scanLeDevice(final boolean enable) {
		if (enable) {	//true ��ʼɨ��
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					mScanButton.setText(R.string.start_scan);
					isScan = false;
				}
			}, SCAN_PERIOD);	//�ӳ�10s ִ�� -> ɨ��10s ��ֹͣɨ��
			mScanButton.setText(R.string.stop_scan);
			isScan = true;
			deleteItem();
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			mScanButton.setText(R.string.start_scan);
			isScan = false;
		}
	}


	/**
	 * ɨ������ mLeDevices.add
	 * addItem
	 */
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {                	
					int majorid,minorid;
					majorid = scanRecord[25]*256 + scanRecord[26];
					minorid = scanRecord[27]*256 + scanRecord[28];
					addItem(device.getName(),device.getAddress()+" UUID:"+StringUtil.bytesToHex(scanRecord,9,16)+" MajorID:"+majorid+" MinorID:"+minorid+" RSSI:"+rssi);
					mLeDevices.add(device);
					MyLog.i(TAG, device.getName());
				}
			});
		}
	};

	private void addItem(String devname,String address)
	{
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("image", R.drawable.icon);
		map.put("title", devname);
		map.put("text", address);
		listItem.add(map);
		listItemAdapter.notifyDataSetChanged();
	}

	private void deleteItem()
	{
		listItem.clear();
		listItemAdapter.notifyDataSetChanged();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id ) {
		final BluetoothDevice device = mLeDevices.get(position);
		if (device == null) return;
		final Intent intent = new Intent();
		intent.setClass(PeripheralActivity.this, MainActivity.class);
		intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, device.getName());
		intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
		if (isScan) {
			scanLeDevice(false);
		}
		startActivity(intent);

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK ) {			
			Constans.exit_ask(this);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
