package mit.edu.obmg.tempsensing;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * This is the main activity of the HelloIOIO example application.
 * 
 * It displays a toggle button on the screen, which enables control of the
 * on-board LED. This example shows a very simple usage of the IOIO, by using
 * the {@link IOIOActivity} class. For a more advanced use case, see the
 * HelloIOIOPower example.
 */
public class TempSensingMain extends IOIOActivity {
	private final String TAG = "TempSensingMain";

	//Sensor I2C
	private TwiMaster twi;
	double sensortemp;

	//UI
	private TextView TempCelsius;
	private TextView TempFahrenheit;

	/**
	 * Called when the activity is first created. Here we normally initialize
	 * our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_temp_sensing_main);

		TempCelsius = (TextView) findViewById(R.id.tempC);
		TempFahrenheit = (TextView) findViewById(R.id.tempF);
	}

	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class Looper extends BaseIOIOLooper {
		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException {
			twi = ioio_.openTwiMaster(0, TwiMaster.Rate.RATE_100KHz, true);

		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException {
			try {
				ReadSensor(0x5A, twi);
				Log.d(TAG, "RAW: "+sensortemp);

				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}

	// temperature sensors
	public void ReadSensor(int address, TwiMaster port) {

		byte[] request = new byte[] { 0x07 };
		byte[] tempdata = new byte[2];
		double receivedTemp = 0x0000;
		double tempFactor = 0.02;

		try {
			port.writeRead(address, false, request,request.length,tempdata,tempdata.length);

			receivedTemp = (double)(((tempdata[1] & 0x007f) << 8)+ tempdata[0]);
			receivedTemp = (receivedTemp * tempFactor)-0.01;

			handleTemp(receivedTemp);
		} catch (ConnectionLostException e) {
			// TODO Auto-generated catch block 
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void handleTemp (double temp){

		final float celsius = (float) (temp - 273.15);
		Log.i(TAG, "C: "+celsius); 

		final float fahrenheit = (float) ((celsius*1.8) + 32);
		Log.i(TAG, "F: "+fahrenheit); 

		TempCelsius.post(new Runnable() {
			public void run() {
				TempCelsius.setText("celsius: "+ celsius);
				TempFahrenheit.setText("Fahrenheit: "+ fahrenheit);
			}
		});

	}
}