package com.example.bluetooth;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void isForward() {
        BluetoothBackground bt = new BluetoothBackground();

        byte[] test = bt.setCarData((byte)15, (byte) 8);
        assertEquals(test[0], -0x71);

    }
    @Test
    public void isBackward() {
        BluetoothBackground bt = new BluetoothBackground();

        byte[] test = bt.setCarData((byte)15, (byte) 4);
        assertEquals(test[0], 0x4F);

    }
    @Test
    public void isRight() {
        BluetoothBackground bt = new BluetoothBackground();

        byte[] test = bt.setCarData((byte)15, (byte) 1);
        assertEquals(test[0], 0x1F);

    }
    @Test
    public void isLeft() {
        BluetoothBackground bt = new BluetoothBackground();

        byte[] test = bt.setCarData((byte)15, (byte) 2);
        assertEquals(test[0], 0x2F);

    }
    @Test
    public void isStopped() {
        BluetoothBackground bt = new BluetoothBackground();

        byte[] test = bt.setCarData((byte)0, (byte) 8);
        assertEquals(test[0], -0x80);

    }
    @Test
    public void isSlow() {
        BluetoothBackground bt = new BluetoothBackground();

        byte[] test = bt.setCarData((byte)4, (byte) 2);
        assertEquals(test[0], 0x24);

    }
    @Test
    public void isFast() {
        BluetoothBackground bt = new BluetoothBackground();

        byte[] test = bt.setCarData((byte)12, (byte) 2);
        assertEquals(test[0], 0x2C);

    }
}
