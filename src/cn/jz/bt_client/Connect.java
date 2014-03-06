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
	static final int INIT = 0;		//初始化socket
	static final int SUCCESS = 1;	//连接成功
	static final int FAILED = 9;	//连接失败了
	static final int CONNECTING = 10;//正在连接
	static final int CLOSE = 11;	//socket关闭
	static final int OVER = 12; //线程结束
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
			s2 ="connect()  OK ",s22="connect() failed",
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
		mHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case INIT:
					mState.setText("初始化...Init");
					mCountView.setText(" 0");
					break;
				case SUCCESS:
					mOkCount++;
					mState.setText(s1 + "\n" + s2);
					mCountView.setText(mOkCount + " \n " + mFailedCount);
					break;
				case FAILED:
					mFailedCount++;
					mCountView.setText(mOkCount + " \n " + mFailedCount);
					mLogView.setText(msg.arg1 + "}" + msg.obj.toString());
					break;
				case CONNECTING:
					mState.setText(s1);
					break;
				case CLOSE:
					if(msg.arg1==0)
						mState.setText(s1 + "\n" +s22 + "\n" + s3);	
					else
						mState.setText(s1 + "\n" + s2 + "\n" + s3);
					break;
				case OVER:
					resetUI();
					Toast.makeText(Connect.this, "Connect Thread over", 1).show();
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
		if(mTask!=null)
		mTask.canceConnect();
		super.onDestroy();
	}

	class ConnectTask extends Thread {
		private volatile boolean runing;
		public ConnectTask() {
			runing = true;
		}

		public void run(){
			BluetoothSocket socket = null;
			
			while (runing && TEST_COUNT > 0) {
				TEST_COUNT--;
				socket = null;
				
				synchronized (this) {
					//create local connsoket.
					try {	
						
                        try {					
						   wait(DELAY);
                        } catch (InterruptedException e) {
                        	loge("InterruptedException");
                        	display(154,e.toString());
                        	break;
                        }
						
						socket = mBluetoothDevice
								.createRfcommSocketToServiceRecord(UUID
										.fromString(MY_UUID));
					  
					    	
						} catch (IOException ee) {
							loge("164]" + ee);
							display(164,ee.toString());
							break;
					   }
					//connect remote device.
					int success = 1;
						try {
							mHandler.removeMessages(CONNECTING);
							mHandler.sendEmptyMessage(CONNECTING);
							socket.connect();
							mHandler.removeMessages(SUCCESS);
							mHandler.sendEmptyMessage(SUCCESS);							
						} catch (IOException ee) {
							loge("176]"+ee);
							display(176,ee.toString());
							success = 0;
							break;
						}finally{							
							closeConnect(socket, success);
						}
					}
				}
			
			mHandler.sendEmptyMessage(OVER);
			loge("thread over================");
		}

		/** Will cancel the listening socket, and cause the thread to finish */
		public void closeConnect(BluetoothSocket socket, int success) {
			try {
				Message msg=mHandler.obtainMessage(CLOSE, success, 0);
				msg.sendToTarget();
				socket.close();	
			} catch (IOException e) {
				loge("196]" + e.toString());
				display(196,e.toString());
			}
		}
		private void display(int type, String error) {
			Message msg=mHandler.obtainMessage(FAILED, type,0, error);
			msg.sendToTarget();
		}
		public void canceConnect() {
		    runing = false;
	   }
	}

   
	void loge(String s) {
		Log.e("sw2df", "(Client)" + s);
	}
}
