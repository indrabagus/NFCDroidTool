package com.bagus.nfc.isodep;
import java.io.IOException;

import org.apache.http.util.ByteArrayBuffer;

import com.bagus.nfc.isodep.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


@SuppressLint({ "ParserError", "ParserError" })
public class MainIsoDep extends Activity{

	NfcAdapter adapter;
	PendingIntent pendingIntent;
	IntentFilter writeTagFilters[];
	boolean writeMode;
	Tag mytag;
	Context ctx;
	AlertDialog mDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.d("ISO_DEP","Activity OnCreate called");

		ctx=this;
		Button btnWrite = (Button) findViewById(R.id.button);
		final TextView message = (TextView)findViewById(R.id.edit_message);

		btnWrite.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v) {
				try {
					if(mytag==null){
						Toast.makeText(ctx, ctx.getString(R.string.error_detected), Toast.LENGTH_LONG ).show();
					}else{
						Log.d("ISO_DEP","message: "+message.getText());
						WriteISoDEP(message.getText().toString(),IsoDep.get(mytag));
						//Toast.makeText(ctx, ctx.getString(R.string.ok_writing), Toast.LENGTH_LONG ).show();
					}
				} catch (IOException e) {
					Toast.makeText(ctx, ctx.getString(R.string.error_writing), Toast.LENGTH_LONG ).show();
					e.printStackTrace();
				} catch (FormatException e) {
					Toast.makeText(ctx, ctx.getString(R.string.error_writing) , Toast.LENGTH_LONG ).show();
					e.printStackTrace();
				}
			}
		});
		

		adapter = NfcAdapter.getDefaultAdapter(this);
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
		tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
		writeTagFilters = new IntentFilter[] { tagDetected };
		// Dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.dialog_text).setTitle(R.string.dialog_title);
		mDialog = builder.create();
		mDialog.show();
		
	}

	

	private void WriteISoDEP(String text, IsoDep isodep) throws IOException, FormatException {
		byte[] datain = String2Pdu(text);
		Log.d("ISO_DEP","APDU: "+Pdu2Str(datain));
		isodep.connect();
		byte[] resp = isodep.transceive(datain);
		Log.d("ISO_DEP","APDU resp: "+Pdu2Str(resp));
		TextView vresp = (TextView)findViewById(R.id.labelresp);
		vresp.setText(Pdu2Str(resp));
		isodep.close();
	}
	
	private String Pdu2Str(byte[] payload){
		String retval="";
		for(int i=0;i<payload.length;i++){
			retval+=String.format("%02X", payload[i]);
		}
		return retval;
	}
	
	private byte[] String2Pdu(String str){
		int len = str.length();
		if((len % 2) != 0){
			len+=1;
		}
		ByteArrayBuffer buffer = new ByteArrayBuffer(len/2);
		
		for(int istart=0,iend=1;iend<len;istart++,iend++){
			byte bval = Byte.parseByte(str.substring(istart, iend), 16);
			buffer.append(bval);
		}

		return buffer.toByteArray();
	}





	@Override
	protected void onNewIntent(Intent intent){
		Log.d("ISO_DEP","OnNewIntent: "+intent);
		if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
			mytag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);    
			mDialog.dismiss();
			//Toast.makeText(this, this.getString(R.string.ok_detection) + mytag.toString(), Toast.LENGTH_LONG ).show();
		}
	}
	
	@Override
	public void onPause(){
		super.onPause();
		adapter.disableForegroundDispatch(this);
	}

	@Override
	public void onResume(){
		super.onResume();
		adapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
	}



}