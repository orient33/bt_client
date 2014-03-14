package cn.jz.bt_client;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
	private static final int Uuid = 1, Chanel = 2, TCP = 3;
	private int useMethod = 3;
    private static int TCP_DEBUG_PORT = 49761;
    private static String TCP_IP;
    private static final int CONNECT_WAIT_TIMEOUT = 4500;

    AlertDialog mDialog;
	BluetoothDevice mBluetoothDevice;
	ConnectTask mTask;
	TcpConnectTask mTcpTask;
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
	
	private Class<?> mSystemProperties;
	private Method mGet;
	boolean mIsGateway;// true if on phone, false if on watch
	String get(String key){
		String value="";
		try {
			if (mSystemProperties == null) {
				mSystemProperties = Class.forName("android.os.SystemProperties");
				mGet = mSystemProperties.getDeclaredMethod("get", String.class);
			}
			value = (String) mGet.invoke(mSystemProperties, key);
		} catch (Exception e) {

		}
		return value;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if("Ingenic".equalsIgnoreCase(get("ro.product.brand"))
				||"s2122b".equalsIgnoreCase(get("ro.product.device"))){
			mIsGateway = false;
		}else 
			mIsGateway = true;
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
		if (useMethod == Uuid)
			useMethod = Chanel;
		else if (useMethod == Chanel)
			useMethod = TCP;
		else
			useMethod = Uuid;
		refreshMethod();
	}
	private void refreshMethod(){
		if (useMethod == Chanel)
			mMethod.setText(" Chanel ");
		else if (useMethod == Uuid)
			mMethod.setText(" UUID ");
		else if (useMethod == TCP)
			mMethod.setText(" TCP ");
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
		if (useMethod == Uuid || useMethod == Chanel) {
			mTask = new ConnectTask();
			mTask.start();
			startTaskUI();
		} else {
			if (!mIsGateway) {
				TCP_IP = getGatewayIP();
				mTcpTask = new TcpConnectTask();
				mTcpTask.start();
				startTaskUI();
			} else {
				showDialog();
			}
		}
	}
	
	private void startTaskUI(){
		mLogView.setText("");
		mOkCount = mFailedCount = 0;
		mUnitIsMs = isUnitMs();
		mHandler.sendEmptyMessage(INIT);
		mStop.setEnabled(true);
		mStart.setEnabled(false);
		mMethod.setEnabled(false);
		mUnit.setEnabled(false);
		mEditDelayFrom.setEnabled(false);
		mEditDelayTo.setEnabled(false);
		mEditCount.setEnabled(false);	
	}
	String getGatewayIP(){
		String key="dhcp.bt-pan.gateway";
//		if(mIsGateway)
//			key="dhcp.bt-pan.ipaddress";
		return get(key);
	}
	private void showDialog(){
		if(null == mDialog){
			AlertDialog.Builder b = new AlertDialog.Builder(this);
			ViewGroup vg=(ViewGroup)View.inflate(this, R.layout.input_ip_port, null);
			final EditText ip = (EditText)vg.findViewById(R.id.ip),
				 port = (EditText)vg.findViewById(R.id.port);
//			ip.setVisibility(View.GONE);
			port.setVisibility(View.GONE);
			b.setView(vg);
			b.setTitle("输入验证码");
			b.setPositiveButton("确认", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface di, int arg1) {

					TCP_IP = ip.getText().toString();
					try{
						TCP_DEBUG_PORT = Integer.parseInt(port.getText().toString());
					}catch (Exception e){}
					mTcpTask = new TcpConnectTask();
					mTcpTask.start();
					startTaskUI();
				}
			});
			mDialog = b.create();
		}
		mDialog.show();
	}

	private void stopTest() {
		if (null != mTask)
			mTask.canceConnect();
		if (null != mTcpTask)
			mTcpTask.canceConnect();
	}
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onBackPressed() {
		if ((mTask != null && mTask.isAlive())
				|| (mTcpTask != null && mTcpTask.isAlive())) {
			Toast.makeText(this, "连接未停，不能退出", 0).show();
		} else
			super.onBackPressed();
	}

	@Override
	protected void onDestroy() {
		if (null != mDialog)
			mDialog.dismiss();
		if (mTask != null)
			mTask.canceConnect();
		if (mTcpTask != null)
			mTcpTask.canceConnect();
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
						if (useMethod==Chanel) {
							Method m = BluetoothDevice.class.getMethod(
									"createRfcommSocket", int.class);
							socket = (BluetoothSocket) m.invoke(
									mBluetoothDevice, 13);
						} else if(useMethod == Uuid){
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
						}else return;
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
	
		public void canceConnect() {
		    runing = false;
	   }
	}
	private void display(int type, String error) {
		Message msg=mHandler.obtainMessage(FAILED, type,0, error);
		msg.sendToTarget();
	}
	class TcpConnectTask extends Thread {
		private volatile boolean runing;
		public TcpConnectTask() {
			runing = true;
		}

		public void run(){
			Socket socket = null;

			while (runing && TEST_COUNT > 0) {
				TEST_COUNT--;
				// Cancel discovery because it will slow down the connection
				BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
				synchronized (this) {
					//create local connsoket.
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
                        	loge("3840] InterruptedException");
                        	display(3840,e.toString());
                        	break;
                        }

                        socket = new Socket();
					//connect remote device.
					int success = 1;
						try {
							mHandler.removeMessages(CONNECTING);
							mHandler.sendEmptyMessage(CONNECTING);
							loge("start connect() ");
                            InetAddress ia = InetAddress.getByName(TCP_IP); //host address.
                            socket.connect(new InetSocketAddress(ia, TCP_DEBUG_PORT), CONNECT_WAIT_TIMEOUT);

                            loge("local Address: "+socket.getLocalAddress().toString());
                            int ret = FAILED;
                            int receive = 0;
                            if (socket.isConnected()) {
                                ret = SUCCESS;
							    loge("connect() OK ;start read from socket ");
							    receive=socket.getInputStream().read();//block
							    loge("read from socket , OK. "+receive);
                            } else {
                                loge("connect() fail");
                                ret = FAILED;
                            }
							Message msg=mHandler.obtainMessage(ret, receive, 0);
							msg.sendToTarget();
							if(ret == FAILED) break;
						} catch (IOException ee) {
							loge("4150]"+ee);
							display(4150,ee.toString());
							success = 0;
							break;
						}finally{							
							closeConnect(socket, success);
							socket = null;
						}
					}
				}
			
			mHandler.sendEmptyMessage(OVER);
			loge("thread over================");
		}

		/** Will cancel the listening socket, and cause the thread to finish */
		public void closeConnect(Socket socket, int success) {
			try {
				Message msg=mHandler.obtainMessage(CLOSE, success, 0);
				msg.sendToTarget();
				socket.close();	
			} catch (IOException e) {
				loge("4370]" + e.toString());
				display(4370,e.toString());
			}
		}
		public void canceConnect() {
		    runing = false;
	   }
	}
	void logd(String s) {
		Log.d("sw2df", "(Client)" + s);
	}
	void loge(String s) {
		Log.e("sw2df", "(Client)" + s);
	}
}
