package cn.jz.bt_client;

import java.io.IOException;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Connect extends Activity {
	static String MY_UUID = "54ceb2f6-856e-4d17-9e5b-ee5afb474de8";
	static int DELAY = 1000;
	static int TEST_COUNT=Integer.MAX_VALUE;
	BluetoothDevice mBluetoothDevice;
	ConnectTask mTask;
	TextView mCountView,mLogView;
	TextView mState;
	EditText mEditCount,mEditDelay;
	Button mStart,mStop;
	String mac;
	int mOkCount,mFailedCount;
	Handler mHandler;
	final String s1="connecting......",
			s2 ="connect()  OK ",
			s3 = "closed.";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connect);
		mCountView = (TextView) findViewById(R.id.count);
		mLogView = (TextView) findViewById(R.id.log);
		mState = (TextView) findViewById(R.id.state);
		mEditCount = (EditText)findViewById(R.id.test_count);
		mEditDelay = (EditText)findViewById(R.id.delay);
		mStart=(Button)findViewById(R.id.start);
		mStop = (Button)findViewById(R.id.stop);
		mStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				startTest();
			}
		});
		mStop.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				stopTest();
			}
		});
		Intent i = this.getIntent();
		mac = i.getStringExtra("mac");
		mBluetoothDevice = BluetoothAdapter.getDefaultAdapter()
				.getRemoteDevice(mac);
		mHandler = new Handler(){

			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0:
					mState.setText("初始化...Init");
					mCountView.setText(" 0");
					break;
				case 1:
					mOkCount++;
					mState.setText(""+"\n"+s2);
					mCountView.setText(mOkCount+" \n "+mFailedCount);
					break;
				case 9:
					mFailedCount++;
					mCountView.setText(mOkCount+" \n "+mFailedCount);
					mLogView.setText(msg.obj.toString());
					break;
				case 10:
					mState.setText(s1);
					break;
				case 11:
					mState.setText(" "+"\n"+" "+"\n"+s3);
					break;
				case 12:
					resetUI();
					break;
				}
			}
			
		};
	}

	private void resetUI(){
		mStart.setEnabled(true);
		mEditDelay.setEnabled(true);
		mEditCount.setEnabled(true);
		mEditDelay.getEditableText().clear();
		mEditCount.getEditableText().clear();
	}
	private void startTest(){
		try{
			TEST_COUNT = Integer.parseInt(mEditCount.getEditableText().toString());
			DELAY = Integer.parseInt(mEditDelay.getEditableText().toString());
		}catch(Exception e){
//			Toast.makeText(this, e.toString(), 1).show();
		}
		mLogView.setText("");
		mOkCount = mFailedCount = 0;
		mTask = new ConnectTask();
		mTask.start();
		mStart.setEnabled(false);
		mEditDelay.setEnabled(false);
		mEditCount.setEnabled(false);
	}
	private void stopTest(){
		mTask.canceConnect();
	}
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		mTask.canceConnect();
		super.onDestroy();
	}

	class ConnectTask extends Thread {
		private BluetoothSocket mmSocket;
		private boolean runing;
		public ConnectTask() {
			mHandler.sendEmptyMessage(0);
			BluetoothSocket tmp = null;
			try {
				tmp = mBluetoothDevice.createRfcommSocketToServiceRecord(UUID
						.fromString(MY_UUID));
			} catch (IOException e) {
			}
			mmSocket = tmp;
			runing = true;
		}

		public void run(){
			Object lock = new Object();
			while (runing && TEST_COUNT > 0) {
				TEST_COUNT--;
				synchronized (lock) {
					try {
						lock.wait(DELAY);
						try {
							mHandler.sendEmptyMessage(10);
							mmSocket.connect();
							mHandler.sendEmptyMessage(1);
							lock.wait(DELAY);
						} catch (IOException ee) {
							loge("113]"+ee);
							Message msg=mHandler.obtainMessage(9, ee.toString());
							msg.sendToTarget();
							canceConnect();
						}finally{
							try {
							mmSocket.close();
							mmSocket = mBluetoothDevice
									.createRfcommSocketToServiceRecord(UUID
											.fromString(MY_UUID));
							} catch (IOException closeException) {
								loge("124]" + closeException);
								break;
							}
						}
					} catch (InterruptedException e1) {
						loge("129]" + e1);
						break;
					}
				}
			}
			canceConnect();
			mHandler.sendEmptyMessage(12);
			loge("thread over================");
		}

		/** Will cancel the listening socket, and cause the thread to finish */
		public void canceConnect() {
			try {
				runing = false;
				mHandler.sendEmptyMessage(11);
				mmSocket.close();
			} catch (IOException e) {
				loge("135]" + e.toString());
			}
		}
	}

	void loge(String s) {
		Log.e("sw2df", "(Client)" + s);
	}
}
