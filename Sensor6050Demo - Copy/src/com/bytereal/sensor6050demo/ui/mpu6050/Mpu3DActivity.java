package com.bytereal.sensor6050demo.ui.mpu6050;

import java.util.UUID;

import com.bytereal.sensor6050demo.Constans;
import com.bytereal.sensor6050demo.R;
import com.bytereal.sensor6050demo.logs.MyLog;
import com.bytereal.sensor6050demo.service.BluetoothLeService;


import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.Toast;

public class Mpu3DActivity extends Activity {
	public static final String SERVERID ="SERVERID";
	public static final String CHARAID ="CHARAID";
	private static final String TAG = "Mpu3DActivity";
	GlSurfaceView mGLSurfaceView;
	BluetoothGattCharacteristic characteristic;
	boolean flag = true;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		super.onCreate(savedInstanceState);
		mGLSurfaceView = new GlSurfaceView(this);
		setContentView(mGLSurfaceView);
		Intent intent = getIntent();
		int servidx = intent.getIntExtra(SERVERID, -1);
		String uuidString = intent.getStringExtra(CHARAID);
		MyLog.i(TAG, "servid="+servidx + " uuid="+uuidString);
		UUID uuid = UUID.fromString(uuidString);


		BluetoothGattService  gattService = Constans.gattServiceObject.get(servidx);  
		characteristic = gattService.getCharacteristic(uuid) ;
		if (characteristic == null) {
			Toast.makeText(this, getString(R.string.mpu6050_sensor_fail), 1).show();
			finish();
		}
		Constans.mBluetoothLeService.readCharacteristic(characteristic);
	}

	@Override
	protected void onResume() {
		// Ideally a game should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onResume();
		mGLSurfaceView.onResume();
		registerReceiver(mGattUpdateReceiver, new IntentFilter(BluetoothLeService.ACTION_DATA_AVAILABLE));
	}

	@Override
	protected void onPause() {
		// Ideally a game should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);
		mGLSurfaceView.onPause();
		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		  if (keyCode == KeyEvent.KEYCODE_BACK) {// 当keyCode等于退出事件值时
			  MyLog.i(TAG, "keyback@");
	            finish();
	            return false;
	        } else {
	            return super.onKeyDown(keyCode, event);
	        }
	}
	
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			System.out.println("action = " + action);
			if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
				getFFB6(data);
			}
		}
	};

	private void getFFB6(String data){
	//	Log.i(TAG, "data.len="+data.length());
		int ax = Integer.parseInt(data.substring(0,2),16) + Integer.parseInt(data.substring(2,4),16)*256;
		int ay = Integer.parseInt(data.substring(4,6),16) + Integer.parseInt(data.substring(6,8),16)*256;
		int az = Integer.parseInt(data.substring(8,10),16) + Integer.parseInt(data.substring(10,12),16)*256;
		int gx = Integer.parseInt(data.substring(16,18),16) + Integer.parseInt(data.substring(18,20),16)*256;
		int gy = Integer.parseInt(data.substring(20,22),16) + Integer.parseInt(data.substring(22,24),16)*256;
		int gz = Integer.parseInt(data.substring(24,26),16) + Integer.parseInt(data.substring(26,28),16)*256;
		if(ax >= 0x8000){
			ax = ax - 0x10000;
		}
		if(ay >= 0x8000){
			ay = ay - 0x10000;
		}
		if(az >= 0x8000){
			az = az - 0x10000;
		}
		if(gx >= 0x8000){
			gx = gx - 0x10000;
		}
		if(gy >= 0x8000){
			gy = gy - 0x10000;
		}
		if(gz >= 0x8000){
			gz = gz - 0x10000;
		}
		//http://www.geek-workshop.com/thread-3670-1-1.html
		//======涓?笅涓夎鏄鍔犻?搴﹁繘琛岄噺鍖栵紝寰楀嚭鍗曚綅涓篻鐨勫姞閫熷害鍊?
		double  Ax=ax/16384.00;	///鍗曚綅 g(9.8m/s^2)
		double  Ay=ay/16384.00;
		double  Az=az/16384.00;


		//==========浠ヤ笅涓夎鏄敤鍔犻?搴﹁绠椾笁涓酱鍜屾按骞抽潰鍧愭爣绯讳箣闂寸殑澶硅
		//http://www.geek-workshop.com/forum.php?mod=viewthread&tid=2328&page=1#pid27876[/url]
		float Angel_accX=(float) (Math.atan(Ax/Math.sqrt(Az*Az+Ay*Ay))*180/3.14);
		float Angel_accY=(float) (Math.atan(Ay/Math.sqrt(Ax*Ax+Az*Az))*180/3.14);
		float Angel_accZ=(float) (Math.atan(Az/Math.sqrt(Ax*Ax+Ay*Ay))*180/3.14);
		mGLSurfaceView.onMpu6050Sensor(Angel_accX, Angel_accY, Angel_accZ);

		Constans.mBluetoothLeService.readCharacteristic(characteristic);
		//==========浠ヤ笅涓夎鏄瑙掗?搴﹀仛閲忓寲==========
		double ggx=gx/131.00;
		double ggy=gy/131.00;
		double ggz=gz/131.00;

//		Log.i(TAG, "ax: "+ax+" ay: "+ay+" az: "+az);
//		Log.i(TAG, "Ax: "+Ax+" Ay: "+Ay+" Az: "+Az);
//		Log.i(TAG, "ax: "+ax+" ay: "+ay+" az: "+az);
		//Log.i(TAG, "Ax: "+Ax+" Ay: "+Ay+" Az: "+Az);
//		Log.i(TAG, "angx: "+Angel_accX+" angy: "+Angel_accY+" angz: "+Angel_accZ);
		//Toast.makeText(CharacteristicsActivity.this, "x: "+ax+" y: "+ay+" z: "+az+" gx: "+gx+" gy: "+gy+" gz: "+gz,Toast.LENGTH_SHORT).show();
	}
}
