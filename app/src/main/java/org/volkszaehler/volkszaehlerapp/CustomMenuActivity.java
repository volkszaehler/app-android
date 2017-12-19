package org.volkszaehler.volkszaehlerapp;

import android.Manifest;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class CustomMenuActivity extends Activity {
    private Menu menu;


    boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                //Log.d("Tools Permission check ","Permission is granted");
                return true;
            } else {
                //Log.d("Tools Permission check ","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            //Log.d("Tools Permission check ","Permission is granted automatically");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            //Log.d("Tools Permission check ","Permission: "+permissions[0]+ " was "+grantResults[0]);
            //resume tasks needing this permission
            onOptionsItemSelected(menu.findItem(R.id.backup_settings));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_settings:
                startActivityForResult(new Intent(this, Preferences.class), 1);
                return (true);
            case R.id.backup_settings:
                if(isStoragePermissionGranted()) {
                    boolean saved = Tools.saveFile(getApplicationContext());
                    if (saved) {
                        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.notsaved, Toast.LENGTH_SHORT).show();
                    }
                }
                return (true);
            case R.id.restore_settings:

                boolean restored = Tools.loadFile(getApplicationContext());
                if (restored) {
                    Toast.makeText(this, R.string.restored, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.notrestored, Toast.LENGTH_SHORT).show();
                }
                return (true);
            case R.id.about:
                return Tools.showAboutDialog(this);

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
