package edu.lehigh.cse.paclab.carbot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * This is the main menu for choosing how to control a robot
 */
public class CarbotBeta extends Activity
{
    /**
     * The main activity is just a menu, so we don't have much to do here... just call super and then draw the screen
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.carbot_beta);
    }

    /**
     * On any button press, we simply launch a new activity
     * 
     * @param v
     *            A reference to the button that was pressed.
     */
    public void launchActivity(View v)
    {
        if (v == findViewById(R.id.btnTetheredBot))
            startActivity(new Intent(this, TetheredBotBeta.class));
        if (v == findViewById(R.id.btnColorDetection))
            startActivity(new Intent(this, ColorDetectionActivity.class));
        if (v == findViewById(R.id.btnRCReceiver))
            startActivity(new Intent(this, RCReceiverActivity.class));
        if (v == findViewById(R.id.btnRCSender))
            startActivity(new Intent(this, RCSenderActivity.class));
        if (v == findViewById(R.id.btnConfig))
            startActivity(new Intent(this, Configure.class));
        if (v == findViewById(R.id.btnDraw))
            startActivity(new Intent(this, DrawActivity.class));
        if (v == findViewById(R.id.btnTalking))
            startActivity(new Intent(this, Chatterbox.class));
    }
}