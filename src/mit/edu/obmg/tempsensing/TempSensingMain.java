package mit.edu.obmg.tempsensing;

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

	class Looper extends BaseIOIOLooper {

		@Override
		protected void setup() throws ConnectionLostException {
			twi = ioio_.openTwiMaster(0, TwiMaster.Rate.RATE_100KHz, true);
			//InitSensor(0x00, twi);
			checkAddress(twi);
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

		byte[] getAddress = new byte[] { 0x6f };
		byte[] getByte = new byte [2];

		try {
			/*port.writeRead(0x00, false, getAddress,getAddress.length,getByte,getByte.length);
			Log.d(TAG, "Get LSByte: " +(double)getByte[0]);
			Log.d(TAG, "Get MSByte: " +(double)getByte[1]);*/

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
		//Log.i(TAG, "C: "+celsius); 

		final float fahrenheit = (float) ((celsius*1.8) + 32);
		//Log.i(TAG, "F: "+fahrenheit); 

		switch ((int)address){
		case 90:
			TempCelsius1.post(new Runnable() {
				public void run() {
					TempCelsius1.setText("Celsius 1: "+ celsius);
					TempFahrenheit1.setText("Fahrenheit 1: "+ fahrenheit);
				}
			});
			break;
		case 42:
			TempCelsius2.post(new Runnable() {
				public void run() {
					TempCelsius2.setText("Celsius 2: "+ celsius);
					TempFahrenheit2.setText("Fahrenheit 2: "+ fahrenheit);
				}
			});
			break;
		case 52:
			TempCelsius3.post(new Runnable() {
				public void run() {
					TempCelsius3.setText("Celsius 3: "+ celsius);
					TempFahrenheit3.setText("Fahrenheit 3: "+ fahrenheit);
				}
			});
			break;

		}

	}
	
	public void checkAddress(TwiMaster port){
		byte[] request_on = new byte[] { 0x07 };
		byte[] response = new byte[2];
		for(int i=0; i<120; i++){
			
			try {
				if( port.writeRead(i, false, request_on,request_on.length,response,response.length)){
					Log.i(TAG, "Address "+ i+ " works!");
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
}