package edu.umich.eecs.eecs589;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Log;

import java.util.Objects;


public class Demo extends Activity {

    //public char[] finalStr;
    public String final_str;
    public String tag = "tag";
    private CheckBox programBTN;
    private CheckBox transparentBTN;
    private CheckBox GPS_FINEBTN;
    private CheckBox GPS_MIDBTN;
    private CheckBox GPS_FARBTN;
    private CheckBox timeBTN;
    private CheckBox accelBTN;
    private CheckBox user_textBTN;
    private CheckBox user_pictureBTN;
    private CheckBox ambiantBTN;
    private EditText dataTEXT;
    private EditText levelTEXT;
    private EditText ipTEXT;
    private EditText programTEXT;
    private EditText rateTEXT;
    private EditText finalTEXT;
    private Button SendBTN;
    private Button GenBTN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        programBTN = (CheckBox) findViewById(R.id.programBTN);
        transparentBTN = (CheckBox) findViewById(R.id.transparentBTN);
        GPS_FINEBTN = (CheckBox) findViewById(R.id.SNR_1);
        GPS_MIDBTN = (CheckBox) findViewById(R.id.SNR_2);
        GPS_FARBTN = (CheckBox) findViewById(R.id.SNR_3);
        timeBTN = (CheckBox) findViewById(R.id.SNR_4);
        accelBTN = (CheckBox) findViewById(R.id.SNR_5);
        user_textBTN = (CheckBox) findViewById(R.id.SNR_6);
        user_pictureBTN = (CheckBox) findViewById(R.id.SNR_7);
        ambiantBTN  = (CheckBox) findViewById(R.id.SNR_8);
        dataTEXT = (EditText) findViewById(R.id.dataTEXT);
        levelTEXT = (EditText) findViewById(R.id.LevelTEXT);
        ipTEXT = (EditText) findViewById(R.id.ipTEXT);
        programTEXT = (EditText) findViewById(R.id.programTEXT);
        rateTEXT = (EditText) findViewById(R.id.rateTEXT);
        finalTEXT = (EditText) findViewById(R.id.finalTEXT);
        GenBTN = (Button) findViewById(R.id.GenBTN);
        SendBTN = (Button) findViewById(R.id.SendBTN);

        //finalStr = new char[500];
        doProgramBTN();
        doTransparentBTN();
        doGPS_FineBTN();
        doGPS_MidBTN();
        doGPS_FarBTN();
        doTimeBTN();
        doAccelBTN();
        doUser_TextBTN();
        doUser_PictureBTN();
        doAmbiantBTN();
        doDataTEXT();
        doLevelTEXT();
        doIPTEXT();
        doProgramTEXT();
        doRateTEXT();

        programBTN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doProgramBTN();
            }
        });
        transparentBTN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doTransparentBTN();
            }
        });
        GPS_FINEBTN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doGPS_FineBTN();
            }
        });
        GPS_MIDBTN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doGPS_MidBTN();
            }
        });
        GPS_FARBTN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doGPS_FarBTN();
            }
        });
        timeBTN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doTimeBTN();

            }
        });
        accelBTN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doAccelBTN();
            }
        });
        user_textBTN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doUser_TextBTN();
            }
        });
        user_pictureBTN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doUser_PictureBTN();
            }
        });
        ambiantBTN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doAmbiantBTN();
            }
        });
        SendBTN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doSendBTN();
            }
        });
        GenBTN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doGenBTN();
            }
        });
        dataTEXT.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {
                doDataTEXT();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });
        levelTEXT.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {
                doLevelTEXT();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });
        ipTEXT.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {
                doIPTEXT();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });
        programTEXT.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {
                doProgramTEXT();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });
        rateTEXT.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {
                doRateTEXT();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });
    }



    public void doRateTEXT() {
        Log.w(tag, "doRateTEXT");

    }

    public void doDataTEXT() {
        Log.w(tag, "doDataTEXT");


    }

    public void doLevelTEXT() {
        Log.w(tag, "doLevelTEXT");


    }

    public void doIPTEXT() {
        Log.w(tag, "doIPTEXT");


    }

    public void doProgramTEXT() {
        Log.w(tag, "doProgramTEXT");


    }

    public void doProgramBTN() {
        Log.w(tag, "doProgramBTN");

    }

    public void doTransparentBTN() {
        Log.w(tag, "doTransparentBTN");


    }

    public void doGPS_FineBTN() {
        Log.w(tag, "doGPS_FineBTN");


    }

    public void doGPS_MidBTN() {
        Log.w(tag, "doGPS_MidBTN");


    }

    public void doGPS_FarBTN() {
        Log.w(tag, "doGPS_FarBTN");


    }

    public void doTimeBTN() {
        Log.w(tag, "doTimeBTN");


    }

    public void doAccelBTN() {
        Log.w(tag, "doAccelBTN");


    }

    public void doUser_TextBTN() {
        Log.w(tag, "doUser_TextBTN");


    }

    public void doUser_PictureBTN() {
        Log.w(tag, "doUser_PictureBTN");

    }

    public void doAmbiantBTN() {
        Log.w(tag, "doAmbiantBTN");

    }

    public String boolToStr(Boolean btn_state) {
        String res;
        if (btn_state) {
            return res = "1";
        } else return res = "0";
    }

    public void doSendBTN() {
        Log.w(tag, "doSendBTN");
        setResult(Activity.RESULT_OK,
                new Intent().putExtra("FINAL_STR", final_str));
        //Intent intent = new Intent(this, Gateway.class);
        //intent.putExtra("TOPARSE", final_str);
        //startActivity(intent);
        finish();
    }

    public void doGenBTN() {
        Log.w(tag, "doGenBTN");

        String programBTN = this.boolToStr(this.programBTN.isChecked());
        String transparentBTN = this.boolToStr(this.transparentBTN.isChecked());
        String gpsFineBTN = this.boolToStr(this.GPS_FINEBTN.isChecked());
        String gpsMidBTN = this.boolToStr(this.GPS_MIDBTN.isChecked());
        String gpsFarBTN = this.boolToStr(this.GPS_FARBTN.isChecked());
        String timeBTN = this.boolToStr(this.timeBTN.isChecked());
        String accelBTN = this.boolToStr(this.accelBTN.isChecked());
        String textBTN = this.boolToStr(this.user_textBTN.isChecked());
        String userPicBTN = this.boolToStr(this.user_pictureBTN.isChecked());
        String ambiantBTN = this.boolToStr(this.ambiantBTN.isChecked());
        String dataTEXT = this.dataTEXT.getText().toString();
        String levelTEXT = this.levelTEXT.getText().toString();
        String IPTEXT = this.ipTEXT.getText().toString();
        String programTEXT = this.programTEXT.getText().toString();
        String rateTEXT = this.rateTEXT.getText().toString();

        Log.w(tag, String.valueOf(programBTN));
        Log.w(tag, String.valueOf(transparentBTN));
        Log.w(tag, String.valueOf(gpsFineBTN));
        Log.w(tag, String.valueOf(gpsMidBTN));
        Log.w(tag, String.valueOf(gpsFarBTN));
        Log.w(tag, String.valueOf(timeBTN));
        Log.w(tag, String.valueOf(accelBTN));
        Log.w(tag, String.valueOf(textBTN));
        Log.w(tag, String.valueOf(userPicBTN));
        Log.w(tag, String.valueOf(ambiantBTN));
        Log.w(tag, dataTEXT);
        Log.w(tag, levelTEXT);
        Log.w(tag, IPTEXT);
        Log.w(tag, programTEXT);
        Log.w(tag, rateTEXT);


        rateTEXT = Integer.toString(Integer.valueOf(rateTEXT.replaceAll("\\s+","")), 2);
        if (rateTEXT.length() != 3) {
            rateTEXT = "0" + rateTEXT;
        }
        String first_byte = transparentBTN + rateTEXT;
        first_byte = String.format("%21X", Long.parseLong(first_byte,2)).replaceAll("\\s+","");
        Log.w("FIRST BYTE", first_byte);

        levelTEXT = Integer.toString(Integer.valueOf(levelTEXT.replaceAll("\\s+","")), 2);
        if (levelTEXT.length() != 3) {
            levelTEXT = "0" + levelTEXT;
        }
        String second_byte = levelTEXT + gpsFineBTN;
        second_byte = String.format("%21X", Long.parseLong(second_byte,2)).replaceAll("\\s+","");
        Log.w("SECOND BYTE", second_byte);

        String sensor_str = gpsMidBTN + gpsFarBTN;
        sensor_str += timeBTN + accelBTN + textBTN + userPicBTN;
        sensor_str += ambiantBTN + programBTN;
        String third_byte = String.format("%21X", Long.parseLong(sensor_str,2)).replaceAll("\\s+","");
        Log.w("THIRD BYTE", third_byte);

        programTEXT = Integer.toString(Integer.valueOf(programTEXT.replaceAll("\\s+","")), 2);
        if (programTEXT.length() != 4) {
            programTEXT = "0" + programTEXT;
        }
        String fourth_byte = programTEXT;
        fourth_byte = String.format("%21X", Long.parseLong(fourth_byte,2)).replaceAll("\\s+","");
        Log.w("FOURTH BYTE", fourth_byte);

        final_str = IPTEXT + first_byte + second_byte;
        final_str += third_byte + fourth_byte + dataTEXT;
        Log.w(tag, final_str);


        this.finalTEXT.setText(final_str);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_demo) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



}
