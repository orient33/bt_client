package cn.jz.bt_client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

	ListView mListView;
	ArrayList<BluetoothDevice> mDevices = new ArrayList<BluetoothDevice>();
	BluetoothAdapter mBluetoothAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.paired_devices);
		mListView = (ListView)findViewById(R.id.listview);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		Set<BluetoothDevice>  pairedDevices = mBluetoothAdapter.getBondedDevices();
		Iterator<BluetoothDevice> its =pairedDevices.iterator();
		while(its.hasNext()){
			mDevices.add(its.next());
		}
		mListView.setAdapter(mListAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View v, int arg2,
					long arg3) {
				String mac = (String) v.getTag();
				startTest(mac);
			}
		});
	}
	
	private void startTest(String mac){
		Intent i = new Intent(this,Connect.class);
		i.putExtra("mac", mac);
		startActivity(i);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private BaseAdapter mListAdapter = new BaseAdapter() {

		@Override
		public View getView(int i, View arg1, ViewGroup arg2) {
			TextView tv = (TextView) arg1;
			if (null == tv) {
				tv = new TextView(MainActivity.this);
				tv.setPadding(10, 10, 10, 10);
				tv.setTextSize(20);
			}
			BluetoothDevice dev = mDevices.get(i);
			String name = dev.getName();
			if (TextUtils.isEmpty(name)) {
				name = dev.getAddress();
			}
			tv.setText(name);
			tv.setTag(dev.getAddress());
			return tv;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public int getCount() {
			return mDevices.size();
		}
	};
}
