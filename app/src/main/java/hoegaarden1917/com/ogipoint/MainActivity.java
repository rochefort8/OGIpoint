package hoegaarden1917.com.ogipoint;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    boolean mWriteMode = false;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(MainActivity.this);
        mNfcPendingIntent = PendingIntent.getActivity(MainActivity.this, 0,
                new Intent(MainActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        ((Button) findViewById(R.id.butun_write)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
/*
                mNfcAdapter = NfcAdapter.getDefaultAdapter(MainActivity.this);
                mNfcPendingIntent = PendingIntent.getActivity(MainActivity.this, 0,
                        new Intent(MainActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
*/
                enableTagWriteMode();

                new AlertDialog.Builder(MainActivity.this).setTitle("Touch tag to write")
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                disableTagWriteMode();
                            }

                        }).create().show();
            }
        });
        handleIntent(getIntent());
    }

    private void enableTagWriteMode() {
        mWriteMode = true;
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] mWriteTagFilters = new IntentFilter[] { tagDetected };
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
    }

    private void disableTagWriteMode() {
        mWriteMode = false;
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        enableTagWriteMode();
    }

    @Override
    public void onPause(){
        super.onPause();
        disableTagWriteMode();
    }

    private void handleIntent(Intent intent) {


        Log.d("NewIntent","N") ;
        // Tag writing mode
        if (mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//	        NdefRecord record = NdefRecord.createMime( ((TextView)findViewById(R.id.mime)).getText().toString(), ((TextView)findViewById(R.id.value)).getText().toString().getBytes());
            NdefRecord record = NdefRecord.createApplicationRecord("hoegaarden1917.com.ogipoint");
            NdefMessage message = new NdefMessage(new NdefRecord[] { record });
            if (writeTag(message, detectedTag)) {
                Toast.makeText(this, "Success: Wrote placeid to nfc tag", Toast.LENGTH_LONG)
                        .show();
            }
        } else {
            NdefMessage[] msgs = getNdefMessagesFromIntent(intent);

        }
    }

    NdefMessage[] getNdefMessagesFromIntent(Intent intent)
    {
        // Parse the intent
        NdefMessage[] msgs = null;
        String action = intent.getAction();
        if (action.equals(NfcAdapter.ACTION_TAG_DISCOVERED) || action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED))
        {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null)
            {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++)
                {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }

            }
            else
            {
                // Unknown tag type
                byte[] empty = new byte[]{};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
                msgs = new NdefMessage[]{msg};
            }

        }
        else
        {
            Log.e("TAG", "Unknown intent.");
            finish();
        }
        return msgs;
    }

    private void readFromNFC(Ndef ndef) {

        try {
            ndef.connect();
            NdefMessage ndefMessage = ndef.getNdefMessage();
            String message = new String(ndefMessage.getRecords()[0].getPayload());
            Log.d("AAA", "readFromNFC: "+message);
            ndef.close();

        } catch (IOException | FormatException e) {
            e.printStackTrace();

        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent) ;
    }

    /*
    * Writes an NdefMessage to a NFC tag
    */
    public boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Toast.makeText(getApplicationContext(),
                            "Error: tag not writable",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    Toast.makeText(getApplicationContext(),
                            "Error: tag too small",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                ndef.writeNdefMessage(message);
                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }
}
