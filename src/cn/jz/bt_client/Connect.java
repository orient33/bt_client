package cn.jz.bt_client;

import java.io.IOException;
import java.lang.reflect.Method;
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
	static final int Connected = 13;
	static final int CLOSE = 11;	//socket关闭
	static final int OVER = 12; //线程结束
	static int DEFAULT_DELAY = 1;
	static int DELAY = DEFAULT_DELAY, DELAY_TO = -1;
	static int TEST_COUNT=Integer.MAX_VALUE;
	private boolean SECURE_CONNECT = false;
	private boolean useChanel=false;
	BluetoothDevice mBluetoothDevice;
	ConnectTask mTask;
	TextView mCountView,mLogView;
	TextView mState;
	EditText mEditCount,mEditDelayFrom,mEditDelayTo;
	Button mStart,mStop,mUnit,mMethod;
	String mac;
	int mOkCount,mFailedCount;
	Handler mHandler;
	final String s1="connecting......",s11="connected , read()...",
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
		mUnit = (Button)findViewById(R.id.unit);
		mMethod=(Button)findViewById(R.id.method);
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
		mUnit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				switchUnit();
			}
		});
		mMethod.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				switchMethod();
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
//					mOkCount = msg.arg1;
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
				case Connected:
					mState.setText(s11);
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
		refreshMethod();
	}
	private void switchMethod(){
		if(useChanel)
			useChanel=false;
		else
			useChanel=true;
		refreshMethod();
	}
	private void refreshMethod(){
		if(useChanel){
			mMethod.setText(" Chanel ");
		}else{
			mMethod.setText(" UUID ");
		}
	}
	private void switchUnit(){
		String unit=mUnit.getText().toString();
		if("ms".equals(unit)){
			mUnit.setText("s");
		}else
			mUnit.setText("ms");
	}
	boolean mUnitIsMs;
	private boolean isUnitMs(){
		if("ms".equals(mUnit.getText().toString()))
			return true;
		else return false;
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
		mMethod.setEnabled(true);
		mUnit.setEnabled(true);
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
		mUnitIsMs = isUnitMs();
		mHandler.sendEmptyMessage(INIT);
		mTask = new ConnectTask();
		mTask.start();
		mStop.setEnabled(true);
		mStart.setEnabled(false);
		mMethod.setEnabled(false);
		mUnit.setEnabled(false);
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

			Log.d("sw2df", " {client}SECURE_CONNECT is "+ SECURE_CONNECT);
			// Cancel discovery because it will slow down the connection
			BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
			while (runing && TEST_COUNT > 0) {
				TEST_COUNT--;
				socket = null;
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
						if (!mUnitIsMs)
							r *= 1000;
						display(0, r + " [ms]");
                        try {
							wait(r);
                        } catch (InterruptedException e) {
                        	loge("154] InterruptedException");
                        	display(154,e.toString());
                        	break;
                        }
						if (useChanel) {
							Method m = BluetoothDevice.class.getMethod(
									"createRfcommSocket", int.class);
							socket = (BluetoothSocket) m.invoke(
									mBluetoothDevice, 13);
						} else {
							if (!SECURE_CONNECT
									&& android.os.Build.VERSION.SDK_INT >= 10) {
								socket = mBluetoothDevice
										.createInsecureRfcommSocketToServiceRecord(UUID
												.fromString(MY_UUID));
							} else {
								if (!SECURE_CONNECT
										&& android.os.Build.VERSION.SDK_INT < 10) {
									loge("it is not a secure_connect , but SDK Level < 10, and then run secure connect");
								}
								socket = mBluetoothDevice
										.createRfcommSocketToServiceRecord(UUID
												.fromString(MY_UUID));
							}
						}
						} catch (Exception ee) {
							loge("164]" + ee);
							display(164,ee.toString());
							break;
					   }
					//connect remote device.
					int success = 1;
					try {
						mHandler.removeMessages(CONNECTING);
						mHandler.sendEmptyMessage(CONNECTING);
						loge("start connect()");
						socket.connect();
						mHandler.removeMessages(Connected);
						mHandler.sendEmptyMessage(Connected);
						loge("connect() OK ; ");
						try {
							int receive = socket.getInputStream().read();// block
							loge("read from socket , OK. " + receive
									+ ", write back ......");
							Message msg = mHandler.obtainMessage(SUCCESS,
									receive, 0);
							msg.sendToTarget();
							socket.getOutputStream().write(receive);
							loge("write back OK ");
						} catch (IOException ee) {
							loge("174]read, OR write error." + ee);
							display(174, ee.toString());
						}
					} catch (IOException ee) {
						loge("176]" + ee);
						display(176, ee.toString());
						success = 0;
						break;
					} finally {
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
