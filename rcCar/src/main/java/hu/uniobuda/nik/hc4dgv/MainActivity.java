package hu.uniobuda.nik.hc4dgv;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;

import hu.uniobuda.nik.hc4dgv.client.ClientActivity;
import hu.varadi.zoltan.rccar.R;
import hu.uniobuda.nik.hc4dgv.server.ServerActivity;

public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton ibServer = (ImageButton) findViewById(R.id.imageButtonServer);
        ImageButton ibClient = (ImageButton) findViewById(R.id.imageButtonClient);

        ibServer.setOnClickListener(buttonOnClick);
        ibClient.setOnClickListener(buttonOnClick);


    }

    private View.OnClickListener buttonOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent mIntent = null;
            if (v.getId() == R.id.imageButtonServer) {
                mIntent = new Intent(getApplicationContext(), ServerActivity.class);
            } else if (v.getId() == R.id.imageButtonClient) {
                mIntent = new Intent(getApplicationContext(), ClientActivity.class);
            }
            startActivity(mIntent);
        }
    };

}
