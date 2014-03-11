package cn.jz.bt_client;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
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
	static int DEFAULT_DELAY = 1;
	static int DELAY = DEFAULT_DELAY, DELAY_TO = -1;
	static int TEST_COUNT=Integer.MAX_VALUE;
	BluetoothDevice mBluetoothDevice;
	ConnectTask mTask;
	TextView mCountView,mLogView;
	TextView mState;
	EditText mEditCount,mEditDelayFrom,mEditDelayTo;
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
		mEditDelayFrom = (EditText)findViewById(R.id.delay_from);
		mEditDelayTo = (EditText)findViewById(R.id.delay_to);
		mStart=(Button)findViewById(R.id.start);
		mStop = (Button)findViewById(R.id.stop);
		mStop.setEnabled(false);
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
					if(0==msg.arg1){
						mLogView.setText( msg.obj.toString());
						break;
					}
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
		TEST_COUNT = Integer.MAX_VALUE;
		DELAY = DEFAULT_DELAY;
		DELAY_TO = -1;
		mStop.setEnabled(false);
		mStart.setEnabled(true);
		mEditDelayFrom.setEnabled(true);
		mEditDelayTo.setEnabled(true);
		mEditCount.setEnabled(true);
		mEditDelayFrom.getEditableText().clear();
		mEditDelayTo.getEditableText().clear();
		mEditCount.getEditableText().clear();
	}
	private void startTest(){
		try{
			TEST_COUNT = Integer.parseInt(mEditCount.getEditableText().toString());
		}catch(Exception e){
		}
		try{
			DELAY = Integer.parseInt(mEditDelayFrom.getEditableText().toString());
		}catch(Exception e){
		}
		try{
			DELAY_TO = Integer.parseInt(mEditDelayTo.getEditableText().toString());
		}catch(Exception e){
		}
		mLogView.setText("");
		mOkCount = mFailedCount = 0;
		mHandler.sendEmptyMessage(INIT);
		mTask = new ConnectTask();
		mTask.start();
		mStop.setEnabled(true);
		mStart.setEnabled(false);
		mEditDelayFrom.setEnabled(false);
		mEditDelayTo.setEnabled(false);
		mEditCount.setEnabled(false);
	}
	private void stopTest(){
		if(null!=mTask)
		mTask.canceConnect();
	}
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onBackPressed() {
		if (mTask != null && mTask.isAlive()) {
			Toast.makeText(this, "连接未停，不能退出", 0).show();
		} else
			super.onBackPressed();
	}

	@Override
	protected void onDestroy() {
		if(mTask!=null)
		mTask.canceConnect();
		super.onDestroy();
		Process.killProcess(Process.myPid());
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
				// Cancel discovery because it will slow down the connection
				BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
				synchronized (this) {
					//create local connsoket.
					try {
						int r = Math.max(1, DELAY);
						if (DELAY_TO > DELAY){
							Random rand= new Random();
							rand.setSeed(System.currentTimeMillis());
							int random=Math.abs(rand.nextInt());
							r += (random) % (DELAY_TO - DELAY);
						}
						display(0,r+" [s]");
                        try {					
							wait(r * 1000);
                        } catch (InterruptedException e) {
                        	loge("154] InterruptedException");
							e.printStackTrace();
                        	display(154,e.toString());
                        	break;
                        }
						
						socket = mBluetoothDevice
								.createRfcommSocketToServiceRecord(UUID
										.fromString(MY_UUID));
					  
					    	
						} catch (IOException ee) {
							loge("164]" + ee);
							ee.printStackTrace();
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
							ee.printStackTrace();
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
				e.printStackTrace();
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
