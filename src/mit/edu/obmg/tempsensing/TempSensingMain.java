package mit.edu.obmg.tempsensing;

import ioio.lib.api.PwmOutput;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class TempSensingMain extends IOIOActivity {
	private final String TAG = "TempSensingMain";

	//Sensor I2C
	private TwiMaster twi;
	double sensortemp;

	//UI
	private TextView TempCelsius1;
	private TextView TempFahrenheit1;
	private TextView TempCelsius2;
	private TextView TempFahrenheit2;
	private TextView TempCelsius3;
	private TextView TempFahrenheit3;

	//Vibration output
	private PwmOutput mVibrate01, mVibrate02, mVibrate03;
	private int mVibrate_pin01 = 34;
	private int mVibrate_pin02 = 35;
	private int mVibrate_pin03 = 36;
	private int freq01 = 349;
	private float period1, period2, period3 = 0;
	private int freq02 = 261;
	private int freq03 = 493;
	private final int VALUE_MULTIPLIER = 1;
	
	/*
	 *  TONES  ==========================================
	 * Start by defining the relationship between 
	 * 
	 *	       note, period, &  frequency. 
	 *	#define  c     3830     261 Hz 
	 *	#define  d     3400     294 Hz 
	 *	#define  e     3038     329 Hz 
	 *	#define  f     2864     349 Hz 
	 *	#define  g     2550     392 Hz 
	 *	#define  a     2272     440 Hz 
	 *	#define  b     2028     493 Hz 
	 *	#define  C     1912     523 Hz 
	 */


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_temp_sensing_main);

		TempCelsius1 = (TextView) findViewById(R.id.tempC1);
		TempFahrenheit1 = (TextView) findViewById(R.id.tempF1);
		TempCelsius2 = (TextView) findViewById(R.id.tempC2);
		TempFahrenheit2 = (TextView) findViewById(R.id.tempF2);
		TempCelsius3 = (TextView) findViewById(R.id.tempC3);
		TempFahrenheit3 = (TextView) findViewById(R.id.tempF3);
	}

	protected void onStart(){
		super.onStart();
	}

	protected void onStop(){
		super.onStop();
		mVibrate01.close();
		mVibrate02.close();
		mVibrate03.close();
	}

	class Looper extends BaseIOIOLooper {

		@Override
		protected void setup() throws ConnectionLostException {
			twi = ioio_.openTwiMaster(0, TwiMaster.Rate.RATE_100KHz, true);
			mVibrate01 = ioio_.openPwmOutput(mVibrate_pin01, freq01);
			mVibrate02 = ioio_.openPwmOutput(mVibrate_pin02, freq02);
			mVibrate03 = ioio_.openPwmOutput(mVibrate_pin03, freq03);
			//InitSensor(0x00, twi);
			//changeAddress(twi,0x5A);
			//checkAddress(twi);
		}

		@Override
		public void loop() throws ConnectionLostException {
			try {
				ReadSensor(0x34, twi);		//dec 52
				ReadSensor(0x2a, twi);		//dec 42
				ReadSensor(0x5a, twi);		//dec 90

				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}

	// temperature sensors
	public void ReadSensor(int address, TwiMaster port) {


		byte[] request = new byte[] { 0x07 };	//Byte address to ask for sensor data
		byte[] tempdata = new byte[2];			//Byte to save sensor data
		double receivedTemp = 0x0000;			//Value after processing sensor data
		double tempFactor = 0.02;				//0.02 degrees per LSB (measurement resolution of the MLX90614)

		try {
			port.writeRead(address, false, request,request.length,tempdata,tempdata.length);

			receivedTemp = (double)(((tempdata[1] & 0x007f) << 8)+ tempdata[0]);
			receivedTemp = (receivedTemp * tempFactor)-0.01;

			Log.d(TAG, "ReceivedTemp: "+ receivedTemp);

			handleTemp(address, receivedTemp);

		} catch (ConnectionLostException e) {
			// TODO Auto-generated catch block 
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void handleTemp (double address, double temp){
		//Log.d(TAG, "Address: "+address);

		final float celsius = (float) (temp - 273.15);
		Log.i(TAG, "Address: "+address+" C: "+celsius); 

		period1 = (2864 + (celsius*20))-3200;
		period2 = (3830 + (celsius*20))-1700;
		period3 = (2028 + (celsius*20))-1700;
		
		final float fahrenheit = (float) ((celsius*1.8) + 32);
		Log.i(TAG, "Address: "+address+" F: "+fahrenheit); 

		switch ((int)address){
		case 90:
			TempCelsius1.post(new Runnable() {
				public void run() {
					TempCelsius1.setText("Celsius 1: "+ celsius);
					TempFahrenheit1.setText("Period: "+ period1);
				}
			});
			try {
				mVibrate01.setPulseWidth(celsius);
			} catch (ConnectionLostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case 42:
			TempCelsius2.post(new Runnable() {
				public void run() {
					TempCelsius2.setText("Celsius 2: "+ celsius);
					TempFahrenheit2.setText("Period 2: "+ period2);
				}
			});
			try {
				mVibrate02.setPulseWidth(celsius);
			} catch (ConnectionLostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case 52:
			TempCelsius3.post(new Runnable() {
				public void run() {
					TempCelsius3.setText("Celsius 3: "+ celsius);
					TempFahrenheit3.setText("Period 3: "+ period3);
				}
			});
			try {
				mVibrate03.setPulseWidth(celsius);
			} catch (ConnectionLostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}
	}

	public void checkAddress(TwiMaster port){
		Log.i(TAG, ":| Checking Address...");
		byte[] request_on = new byte[] { 0x07 };
		byte[] response = new byte[2];
		for(int i=0; i<120; i++){

			try {
				if( port.writeRead(i, false, request_on,request_on.length,response,response.length)){
					Log.i(TAG, ":)  Address "+ i+ " works!");
				}else{
					Log.i(TAG, ":(  Address "+ i+ " doesn't work!");
				}
			} catch (ConnectionLostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void changeAddress(TwiMaster port, double address){
		byte[] eraseExisting = new byte[] {0x2E, 0,0 };
		byte[] response = new byte[2];
		byte[] newAddress = new byte [] { 0x2E, 90, 0};

		try {
			port.writeRead(0, false, eraseExisting,eraseExisting.length,response,response.length);
			Log.d( TAG, "Erase response 1: "+response[0]);
			Log.d( TAG, "Erase response 2: "+response[1]);

			port.writeRead(0, false, newAddress,newAddress.length,response,response.length);
			Log.d( TAG, "Write response 1: "+response[0]);
			Log.d( TAG, "Write response 2: "+response[1]);

		} catch (ConnectionLostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}