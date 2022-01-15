package com.machine.serialport.util.other.power;


import com.machine.serialport.util.LogUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
//import com.znht.iodev2.PowerCtl;


public class OtherPower {
	public static String OpAddr="/dev/ttysWK0";
	public static boolean oPowerUp()
	{
		//power_up();
		//new OtherPower().panningpowerup();
		return true;
	}

	public static boolean oPowerDown()
	{
		//power_down();
		return true;

	}


	//肖邦新手持机上电？？？
	private void power_up() {
		File com = new File("/sys/devices/platform/odm/odm:exp_dev/gps_com_switch");
		File power = new File("/sys/bus/platform/devices/odm:exp_dev/psam_en");
		LogUtils.INSTANCE.logD("YM","power:" + power);

		writeFile(com, "1");
		writeFile(power, "1");
	}

	private void power_down() {
		File com = new File("/sys/devices/platform/odm/odm:exp_dev/gps_com_switch");
		File power = new File("/sys/bus/platform/devices/odm:exp_dev/psam_en");
		LogUtils.INSTANCE.logD("YM","power:" + power);

		writeFile(com, "0");
		writeFile(power, "0");
	}
	private synchronized void writeFile(File file, String value) {

		try {
			FileOutputStream outputStream = new FileOutputStream(file);
			outputStream.write(value.getBytes());
			outputStream.flush();
			outputStream.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	//>>>>>>>>>>>>>>>>>>>>>>>

	//攀宁上电
	/*
	private PowerCtl powerCtl ;
	public void panningpowerup() {

		try {
			powerCtl = new PowerCtl();
			powerCtl.psam_ctl(1);
			powerCtl.sub_board_power(1);
			powerCtl.identity_ctl(1);
			powerCtl.scan_trig(1);
			powerCtl.scan_wakeup(0);
			powerCtl.scan_power(1);
			powerCtl.identity_uhf_power(1);
			powerCtl.uhf_ctl(1);

		} catch (SecurityException e) {
			//DisplayError(R.string.error_security);
		} catch (InvalidParameterException e) {
			//DisplayError(R.string.error_configuration);
		}
	}
	*/
}
