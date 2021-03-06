package edu.lehigh.cse.paclab.carbot;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

/**
 * This is a parent class so that all of our activities have easy access to constants and TTS
 */
public abstract class BasicBotActivityBeta extends Activity implements TextToSpeech.OnInitListener
{
    // constants for preference tags
    final public static String         PREFS_NAME              = "CARBOT_NAME";
    final public static String         PREFS_BYE               = "CARBOT_BYE";
    final public static String         PREFS_DIST              = "CARBOT_DIST";
    final public static String         PREFS_ROT               = "CARBOT_ROT";

    /**
     * Indicate the port this app uses for sending control signals between a client and server
     * 
     * TODO: move to preferences?
     */
    public static final int            WIFICONTROLPORT         = 9599;

    /**
     * Tag for debugging...
     */
    public static final String         TAG                     = "Carbot";

    /**
     * For accessing the preferences storage of the activity
     */
    SharedPreferences                  prefs;

    /**
     * A self reference, for alarms
     */
    public static BasicBotActivityBeta _self;

    /**
     * The text to speech interface
     */
    TextToSpeech                       tts;

    // The following code block is setting up the android to arduino communication.
    // TODO: can we make this a has-a instead of an is-a?
    // TODO: comment better, so that new users can learn about USBManager from this code
    private static final String        ACTION_USB_PERMISSION   = "com.google.android.Demokit.action.USB_PERMISSION";
    private UsbManager                 mUsbManager;
    private PendingIntent              mPermissionIntent;
    private boolean                    mPermissionRequestPending;
    UsbAccessory                       mAccessory;
    ParcelFileDescriptor               mFileDescriptor;
    FileInputStream                    mInputStream;
    FileOutputStream                   mOutputStream;

    /**
     * Flag for tracking if we have USB support configured
     */
    private boolean                    isUSBReceiverRegistered = false;

    /**
     * BroadcastReceiver is the object responsible for establishing communication with any sort of entity sending
     * information to the application, in this case, the application is receiving information from an arduino. The
     * BroadcastReceiver sees if the entity sending information is a supported usb accessory, in which case opens full
     * communication with the device beyond the handshake which is initiated upon application launch.
     * 
     * TODO: make this code less ugly
     */
    private final BroadcastReceiver    mUsbReceiver            = new BroadcastReceiver()
                                                               {
                                                                   @Override
                                                                   public void onReceive(Context context, Intent intent)
                                                                   {
                                                                       String action = intent.getAction();
                                                                       if (ACTION_USB_PERMISSION.equals(action)) {
                                                                           synchronized (this) {
                                                                               UsbAccessory accessory = (UsbAccessory) intent
                                                                                       .getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                                                                               if (intent
                                                                                       .getBooleanExtra(
                                                                                               UsbManager.EXTRA_PERMISSION_GRANTED,
                                                                                               false)) {
                                                                                   openAccessory(accessory);
                                                                               }
                                                                               else {
                                                                                   Log.d(TAG,
                                                                                           "permission denied for accessory "
                                                                                                   + accessory);
                                                                               }
                                                                               mPermissionRequestPending = false;
                                                                           }
                                                                       }
                                                                       else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED
                                                                               .equals(action)) {
                                                                           UsbAccessory accessory = (UsbAccessory) intent
                                                                                   .getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                                                                           if (accessory != null
                                                                                   && accessory.equals(mAccessory)) {
                                                                               closeAccessory();
                                                                           }
                                                                       }
                                                                   }
                                                               };

    /**
     * If the application has stopped and then has resumed, the application will check to see if the input and output
     * stream of data is still active then checks to see if an accessory is present, if so, opens communication; if not,
     * the application will request permission for communication.
     */
    @Override
    protected void onResume()
    {
        super.onResume();

        // resurrect the USBManager
        if (mInputStream != null && mOutputStream != null) {
            return;
        }
        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory)) {
                openAccessory(accessory);
            }
            else {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory, mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        }
    }

    /**
     * When 'back' is pressed, shut off the USBManager support and terminate the app
     */
    @Override
    public void onBackPressed()
    {
        closeAccessory();
        if (isUSBReceiverRegistered)
            unregisterReceiver(mUsbReceiver);
        finish();
    }

    /**
     * In the event of the application being terminated or paused closeAccessory is called to end the data input stream.
     */
    protected void closeAccessory()
    {
        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }
        }
        catch (IOException e) {
        }
        finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }

    /**
     * Called upon the construction of the BroadcastReceiver assuming the BroadcastReceiver has found an accessory to
     * interact with. openAccessory is also called in the onResume method. Opens up a data output and input stream for
     * communication with an accessory.
     * 
     * @param accessory TODO:
     */
    protected void openAccessory(UsbAccessory accessory)
    {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
        }
        else {
            Log.d(TAG, "accessory open fail");
        }
    }

    /**
     * Called any time information is to be sent out from the application over the output stream. An array of bytes is
     * created with the first value holding the "address" of the hardware being communicated with. In this case, 1 means
     * the forward, 2 means reverse, etc. In our system, Arduino handles the task to be carried out by the hardware, so,
     * Arduino only needs to know whether or not to carry out a particular action indicated by the action reference
     * number.
     * 
     * @param target TODO:
     */
    public void sendCommand(byte target)
    {
        byte[] buffer = new byte[1];
        buffer[0] = target;

        Log.e(TAG, "Message sent" + buffer[0]);
        if (mOutputStream != null) {
            try {
                // TODO: we could use this instead: mOutputStream.write(oneByte);
                mOutputStream.write(buffer);
            }
            catch (IOException e) {
                Log.e(TAG, "write failed", e);
            }
        }
    }

    /**
     * Send a byte to the Arduino that instructs it to stop
     */
    public void robotStop()
    {
        sendCommand((byte) 0);
    }

    /**
     * Send a byte to the Arduino that instructs it to go forward
     */
    public void robotForward()
    {
        sendCommand((byte) 1);
    }

    /**
     * Send a byte to the Arduino that instructs it to go backward
     */
    public void robotReverse()
    {
        sendCommand((byte) 2);
    }

    /**
     * Send a byte to the Arduino that instructs it to go clockwise
     */
    public void robotClockwise()
    {
        sendCommand((byte) 3);
    }

    /**
     * Send a byte to the Arduino that instructs it to go counterclockwise
     */
    public void robotCounterClockwise()
    {
        sendCommand((byte) 4);
    }

    /**
     * Send a byte to the Arduino that instructs it to do a point turn right
     */
    public void robotPointTurnRight()
    {
        sendCommand((byte) 6);
    }

    /**
     * Send a byte to the Arduino that instructs it to do a pont turn left
     */
    public void robotPointTurnLeft()
    {
        sendCommand((byte) 5);
    }

    /**
     * Program-wide configuration goes here
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Keep a self reference, so that alarms can work correctly
        _self = this;

        // configure tts and preferences
        tts = new TextToSpeech(this, this);
        prefs = getSharedPreferences("edu.lehigh.cse.paclab.carbot.CarBotActivity", Activity.MODE_WORLD_WRITEABLE);

        // Don't let the app sleep
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Arduino Support

        // Configure USBManager
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        // Creates new IntentFilter to indicate future communication with a
        // particular entity
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);
        isUSBReceiverRegistered = true;

        if (getLastNonConfigurationInstance() != null) {
            mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
            openAccessory(mAccessory);
        }
    }

    /**
     * When the activity goes away, we need to clear out TTS
     */
    @Override
    public void onDestroy()
    {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    /**
     * When TTS is initialized, we need this
     */
    @Override
    public void onInit(int status)
    {
        // if we were successful initialization, set the language to US
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Trouble with language pack");
            }
        }
        else {
            Log.e(TAG, "TTS Initialization error");
        }
    }

    /**
     * Simple mechanism for using text-to-speech
     */
    void speak(String s)
    {
        tts.speak(s, TextToSpeech.QUEUE_FLUSH, null);
    }

    /**
     * This method returns the phone's IP addresses
     * 
     * @return A string representation of the phone's IP addresses
     */
    static String getLocalIpAddress()
    {
        String ans = "";
        try {
            // get all network interfaces, and create a string of all their addresses
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                Enumeration<InetAddress> addrList = ni.getInetAddresses();
                while (addrList.hasMoreElements()) {
                    InetAddress addr = addrList.nextElement();
                    if (!addr.isLoopbackAddress()) {
                        ans += addr.getHostAddress().toString() + ";";
                    }
                }
            }
        }
        catch (SocketException ex) {
            Log.e("ServerActivity", ex.toString());
        }
        return ans;
    }

    /**
     * A wrapper for Toast that handles problems that stem from trying to Toast from a thread that isn't the UI thread.
     * This variant prints a short toast.
     * 
     * @param s
     *            The message to display
     */
    void shortbread(final String s)
    {
        BasicBotActivityBeta.this.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(BasicBotActivityBeta.this, s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * A wrapper for Toast that handles problems that stem from trying to Toast from a thread that isn't the UI thread.
     * This variant prints a long toast.
     * 
     * @param s
     *            The message to display
     */
    void longbread(final String s)
    {
        BasicBotActivityBeta.this.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(BasicBotActivityBeta.this, s, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * This function gives us the ability to have an AlarmReceiver that can do all sorts of arbitrary stuff
     */
    abstract public void callback();
}