package com.anapp.bluecontrol;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
  private static final String TAG = "LEDOnOff";
  
  Button btnOn, btnOff;
  TextView text;
  
  private BluetoothAdapter btAdapter = null;
  private BluetoothSocket btSocket = null;
  private OutputStream outStream = null;
  
  
  // Intent request codes
  private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
  private static final int REQUEST_ENABLE_BT = 3;
  
  // Well known SPP UUID
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  // Insert your bluetooth devices MAC address
  private static String address = "00:00:00:00:00:00";
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    btnOn = (Button) findViewById(R.id.btnOn);
    btnOff = (Button) findViewById(R.id.btnOff);
    text = (TextView) findViewById(R.id.textEdit);
    
    btnOn.setEnabled(false);
    btnOff.setEnabled(false);
    
    btAdapter = BluetoothAdapter.getDefaultAdapter();
    
    checkBTState();
    
    Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
    
//    if (pairedDevices.size() > 0) {
//       for (BluetoothDevice device : pairedDevices) {
//    	
//        String deviceBTName = device.getName();
//        text.setText(text.getText() + " Device: " + deviceBTName);
//        if (deviceBTName.equals("HC-06")){
//        	address = device.getAddress();
//        }
//       }
//   }
  }
  
  public void ledOn(View v){
	  sendData("1");
      Toast msg = Toast.makeText(getBaseContext(), "LED is ON", Toast.LENGTH_SHORT);
      msg.show();
  }
  
  public void ledOff(View v){
	  sendData("0");
      Toast msg = Toast.makeText(getBaseContext(), "LED is OFF", Toast.LENGTH_SHORT);
      msg.show();
  }
  

  public void connectToDevice(String adr) {
    super.onResume();
    
    //enable buttons once connection established.
    btnOn.setEnabled(true);
    btnOff.setEnabled(true);
    
    
    
    // Set up a pointer to the remote node using it's address.
    BluetoothDevice device = btAdapter.getRemoteDevice(adr);
    
    // Two things are needed to make a connection:
    //   A MAC address, which we got above.
    //   A Service ID or UUID.  In this case we are using the
    //     UUID for SPP.
    try {
      btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
    } catch (IOException e) {
      errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
    }
  
    // Discovery is resource intensive.  Make sure it isn't going on
    // when you attempt to connect and pass your message.
    btAdapter.cancelDiscovery();
  
    // Establish the connection.  This will block until it connects.
    try {
      btSocket.connect();
    } catch (IOException e) {
      try {
        btSocket.close();
      } catch (IOException e2) {
        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
      }
    }
    
    // Create a data stream so we can talk to server.
    try {
      outStream = btSocket.getOutputStream();
    } catch (IOException e) {
      errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
    }
  }

//  @Override
//  public void onPause() {
//    super.onPause();
//
////    if (outStream != null) {
////      try {
////        outStream.flush();
////      } catch (IOException e) {
////        errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
////      }
////    }
//
//    try     {
//      btSocket.close();
//    } catch (IOException e2) {
//      errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
//    }
//  }
  
  private void checkBTState() {
    // Check for Bluetooth support and then check to make sure it is turned on

    // Emulator doesn't support Bluetooth and will return null
    if(btAdapter==null) { 
      errorExit("Fatal Error", "Bluetooth Not supported. Aborting.");
    } else {
      if (btAdapter.isEnabled()) {
        Log.d(TAG, "...Bluetooth is enabled...");
      } else {
        //Prompt user to turn on Bluetooth
        Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
      }
    }
  }

  private void errorExit(String title, String message){
    Toast msg = Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_SHORT);
    msg.show();
    finish();
  }

  private void sendData(String message) {
    byte[] msgBuffer = message.getBytes();
    try {
      outStream.write(msgBuffer);
    } catch (IOException e) {
      String msg = "In onResume() and an exception occurred during write: " + e.getMessage();      
      errorExit("Fatal Error", msg);       
    }
  }
  
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
      switch (requestCode) {
      case REQUEST_CONNECT_DEVICE_SECURE:
          // When DeviceListActivity returns with a device to connect
          if (resultCode == Activity.RESULT_OK) {
              connectDevice(data, true);
          }
          break;
      }
  }
  
  private void connectDevice(Intent data, boolean secure) {
      // Get the device MAC address
      address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
      text.setText("Device Address: " + address);
      connectToDevice(address);
      // Get the BluetoothDevice object
      BluetoothDevice device = btAdapter.getRemoteDevice(address);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.option_menu, menu);
      return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      Intent serverIntent = null;
      switch (item.getItemId()) {
      case R.id.secure_connect_scan:
          // Launch the DeviceListActivity to see devices and do scan
          serverIntent = new Intent(this, DeviceListActivity.class);
          startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
          return true;
      }
      return false;
  }
}