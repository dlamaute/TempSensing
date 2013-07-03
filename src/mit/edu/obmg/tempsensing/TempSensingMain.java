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

		}

		@Override
		public void loop() throws ConnectionLostException {
			try {
				//ReadSensor(0x77, twi);
				ReadSensor(0x5A, twi);
				//ReadSensor(0x50, twi);
				
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

			//Log.d(TAG, "ReceivedTemp: "+ receivedTemp);
			
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
		case 119:
			TempCelsius2.post(new Runnable() {
				public void run() {
					TempCelsius2.setText("Celsius 2: "+ celsius);
					TempFahrenheit2.setText("Fahrenheit 2: "+ fahrenheit);
				}
			});
			break;
		case 80:
			TempCelsius3.post(new Runnable() {
				public void run() {
					TempCelsius3.setText("Celsius 3: "+ celsius);
					TempFahrenheit3.setText("Fahrenheit 3: "+ fahrenheit);
				}
			});
			break;
			
		}

	}
}