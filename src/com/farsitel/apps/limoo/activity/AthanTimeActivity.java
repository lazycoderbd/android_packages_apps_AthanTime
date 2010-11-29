package com.farsitel.apps.limoo.activity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.farsitel.apps.limoo.R;
import com.farsitel.apps.limoo.data.AthanTime;
import com.farsitel.apps.limoo.data.DayTime;
import com.farsitel.apps.limoo.logic.AthanTimeCalculator;
import com.farsitel.apps.limoo.util.ConstantUtil;
import com.farsitel.apps.limoo.util.TimeZoneUtil;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.Jalali;
import android.text.format.JalaliDate;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.farsitel.apps.limoo.views.*;

public class AthanTimeActivity extends Activity implements LocationListener,
        OnSharedPreferenceChangeListener,OnClickListener {
    /** Called when the activity is first created. */
    PanelSwitcher panelSwitcher;
    private static final int TODAY_PANEL = 1;
    private static final int TOMORROW_PANEL = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        panelSwitcher = (PanelSwitcher) findViewById(R.id.panelswitch);

        setContentView(R.layout.main);
        Context context = getApplicationContext();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);
        String gpsPerfKey = getString(R.string.gps_pref_key);
        boolean isGPS = false;
        try {
            isGPS = prefs.getBoolean(gpsPerfKey, false);
        } catch (ClassCastException e) {
            isGPS = Boolean.parseBoolean(prefs.getString(gpsPerfKey, "false"));
        }
        if (isGPS)
            registerListener();
        else
            useDefaultLocation(prefs,
                    getString(R.string.state_location_pref_key));
        Button nextBtn = (Button)findViewById(R.id.nextBtn);
        Button previousBtn = (Button)findViewById(R.id.previous);
        nextBtn.setOnClickListener(this);
        previousBtn.setOnClickListener(this);
    }

    public boolean isRegistered = false;

    private void registerListener() {
        if (!isRegistered) {
            ((LocationManager) getSystemService(Context.LOCATION_SERVICE))
                    .requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            ConstantUtil.MIN_LOCATION_TIME,
                            ConstantUtil.MIN_LOCATION_DISTANCE, this);
            isRegistered = true;
        }
    }

    private void unregisterListener() {
        if (isRegistered) {
            ((LocationManager) getSystemService(Context.LOCATION_SERVICE))
                    .removeUpdates(this);
            isRegistered = false;
        }
    }

    AthanTimeCalculator athanTime = null;
    private Location previousLocation = null;

    @Override
    public void onLocationChanged(Location location) {
        panelSwitcher = (PanelSwitcher) findViewById(R.id.panelswitch);
        if (previousLocation == null || isFarEnough(location, previousLocation)) {
            if (athanTime == null) {
                athanTime = new AthanTimeCalculator();
                athanTime = prepareAthanCalculator(athanTime);
                previousLocation = location;
            }
            Calendar c = Calendar.getInstance();
            c.setTimeZone(TimeZone.getDefault());
            c.setTimeInMillis(System.currentTimeMillis());
            athanTime.setCalcMethod(athanTime.Jafari);
            athanTime.setAsrJuristic(athanTime.Shafii);
            athanTime.setAdjustHighLats(athanTime.AngleBased);
            int[] offsets = { 0, 0, 0, 0, 0, 0, 0 }; // {Fajr,Sunrise,Dhuhr,Asr,Sunset,Maghrib,Isha}
            athanTime.tune(offsets);
            AthanTime prayerTimes = athanTime.getPrayerTimes(c,
                    location.getLatitude(), location.getLongitude(),
                    athanTime.getBaseTimeZone());
            c.add(Calendar.DATE, 1);
            todayAthanTime = prayerTimes;
            AthanTime tommorowPrayerTime = athanTime.getPrayerTimes(c,
                    location.getLatitude(), location.getLongitude(),
                    athanTime.getBaseTimeZone());
            tomorrowAthanTime = tommorowPrayerTime;
            setAthanTimeText(prayerTimes, tommorowPrayerTime);
        }

    }

    private AthanTime todayAthanTime;
    private AthanTime tomorrowAthanTime;

    private void setAthanTimeText(AthanTime todayPrayerTimes,
            AthanTime tommorowPrayerTimes) {
        TextView fajr;
        TextView sunrise;
        TextView duhr;
        TextView sunset;
        TextView maghrib;
        TextView latitude;
        TextView longitude;
        TextView datePrefixText;
        TextView dateText;
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getDefault());
        c.setTimeInMillis(System.currentTimeMillis());
       
        

        fajr = (TextView) findViewById(R.id.fajrID);
        sunrise = (TextView) findViewById(R.id.sunriseID);
        duhr = (TextView) findViewById(R.id.noonID);
        sunset = (TextView) findViewById(R.id.nightID);
        maghrib = (TextView) findViewById(R.id.nightAthanID);
        datePrefixText = (TextView) findViewById(R.id.todayID);
        // latitude = (TextView) findViewById(R.id.latitudeID);

        // longitude = (TextView) findViewById(R.id.longitudeID);
       datePrefixText.setText( getString(R.string.today)+ " " + getFormattedDate(c));
//        datePrefixText.setText(String.format(getString(R.string.today) +" %Ld " +Jalali.getLongMonthName(c.get(Calendar.MONTH)) + " ماه %Ld",c.get(Calendar.DATE),c.get(Calendar.YEAR)));
        fajr.setText(getTimeForPrint(todayPrayerTimes.getFajr()));
        sunrise.setText(getTimeForPrint(todayPrayerTimes.getSunrise()));
        duhr.setText(getTimeForPrint(todayPrayerTimes.getDhuhr()));
        sunset.setText(getTimeForPrint(todayPrayerTimes.getSunset()));
        maghrib.setText(getTimeForPrint(todayPrayerTimes.getMaghrib()));
        fajr.setTypeface(Typeface.DEFAULT_BOLD);
        sunrise.setTypeface(Typeface.DEFAULT_BOLD);
        duhr.setTypeface(Typeface.DEFAULT_BOLD);
        sunset.setTypeface(Typeface.DEFAULT_BOLD);
        maghrib.setTypeface(Typeface.DEFAULT_BOLD);
        // today.setText(" today " + c.get(Calendar.YEAR) + "/"
        // + (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.DATE)
        // + " Ho " + effective + " hi " + TimeZone.getDefault()
        // + getTitle());
        // Log.w(ATHAN_TIME_LOG, "go" + effective + " ");

        // longitude.setText(getLocationForPrint(previousLocation.getLongitude(),
        // false));
        c.add(Calendar.DATE, 1);
        fajr = (TextView) findViewById(R.id.fajrID2);
        sunrise = (TextView) findViewById(R.id.sunriseID2);
        duhr = (TextView) findViewById(R.id.noonID2);
        sunset = (TextView) findViewById(R.id.nightID2);
        maghrib = (TextView) findViewById(R.id.nightAthanID2);
        datePrefixText = (TextView)findViewById(R.id.tommorrowId);
//        datePrefixText = (TextView) findViewById(R.id.to);
        // latitude = (TextView) findViewById(R.id.latitudeID2);
        // longitude = (TextView) findViewById(R.id.longitudeID2);
       
        datePrefixText.setText(getString(R.string.tomorrow)+" " +getFormattedDate(c));
        fajr.setText(getTimeForPrint(tommorowPrayerTimes.getFajr()));
        sunrise.setText(getTimeForPrint(tommorowPrayerTimes.getSunrise()));
        duhr.setText(getTimeForPrint(tommorowPrayerTimes.getDhuhr()));
        sunset.setText(getTimeForPrint(tommorowPrayerTimes.getSunset()));
        maghrib.setText(getTimeForPrint(tommorowPrayerTimes.getMaghrib()));
        fajr.setTypeface(Typeface.DEFAULT_BOLD);
        sunrise.setTypeface(Typeface.DEFAULT_BOLD);
        duhr.setTypeface(Typeface.DEFAULT_BOLD);
        sunset.setTypeface(Typeface.DEFAULT_BOLD);
        maghrib.setTypeface(Typeface.DEFAULT_BOLD);

    }

    // @Override
     public boolean onTouchEvent(MotionEvent event) {
     // TODO Auto-generated method stub
     super.onTouchEvent(event);
     panelSwitcher = (PanelSwitcher) findViewById(R.id.panelswitch);
     boolean isTurned = panelSwitcher.onTouchEvent(event);
     //rebuildForPanelChange(panelSwitcher);
     return isTurned;
    
     }
    //
    
    private String getFormattedDate(Calendar c){
        int year = c.get(Calendar.YEAR);int month = c.get(Calendar.MONTH); int date = c.get(Calendar.DATE);
        
        JalaliDate jDate = Jalali.gregorianToJalali(year, month, date);
        String monthName = Jalali.getLongMonthName(jDate.month);
        date= jDate.day;
        year = jDate.year;
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        String dayOfWeekStr = "";
        switch(dayOfWeek){
        case 0:
            break;
        case 1:
            break;
        case 2:
            break;
        case 3:
            break;
        case 4:
            break;
        case 5:
            break;
        case 6:
            break;
            
                
        }
        int postKey =R.string.forth_above;;
        switch (date){
            case 1:
                postKey=R.string.first;
                break;
            case 2:
                postKey = R.string.second;
                break;
            case 3:
                postKey = R.string.third;
                break;
        }
//        return "" + android.text.format.DateFormat.format(getString(R.string.dateInFormat), c,this);
        return String.format("%s %Ld%s %s %Ld",dayOfWeekStr, date,getString(postKey),monthName,year);
    }
    private String getTimeForPrint(DayTime dayTime) {
        int hour = dayTime.getHour();
        int minute = dayTime.getMinute();
        String hourStr = "" + hour;
        String minuteStr = "" + minute;
        return String.format("%L02d:%L02d", hour, minute);

    }

    private String getLocationForPrint(double value, boolean isLatitude) {
        int degree = (new Double(Math.floor(value))).intValue();
        String end = getString(((isLatitude) ? R.string.latitude_south
                : R.string.longitude_west));
        if (degree > 0) {
            end = getString(((isLatitude) ? R.string.latitude_north
                    : R.string.longitude_east));
        }
        double second = (value - degree) * 100;
        double minDouble = (second * 3d / 5d);
        int minute = new Double(Math.floor(minDouble)).intValue();
        return Jalali.persianDigits(degree + "") + "" + '\u00B0' + " "
                + Jalali.persianDigits("" + minute) + "" + '\u00B4' + "" + end;

    }

    @Override
    public void onResume() {
        super.onResume();
        registerListener();
    }

    private boolean isFarEnough(Location newLocation, Location previousLocation) {
        double newLatitude = newLocation.getLatitude();
        double newLongitude = newLocation.getLongitude();
        double prevLatitude = previousLocation.getLatitude();
        double prevLongitude = previousLocation.getLongitude();
        return true;
    }

    private AthanTimeCalculator prepareAthanCalculator(
            AthanTimeCalculator athanTime) {
        athanTime.setCalcMethod(AthanTimeCalculator.ISNA);
        return athanTime;
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    public static final int MENU_ARAK = 1;
    public static final int MENU_ARDABIL = 2;
    public static final int MENU_ORUMIYEH = 3;
    public static final int MENU_ESFEHAN = 4;
    public static final int MENU_AHVAZ = 5;
    public static final int MENU_ILAM = 6;
    public static final int MENU_BOJNURD = 7;
    public static final int MENU_BANDAR_ABAS = 8;
    public static final int MENU_BUSHEHR = 9;
    public static final int MENU_BIRJAND = 10;
    public static final int MENU_TABRIZ = 11;
    public static final int MENU_TEHRAN = 12;
    public static final int MENU_KHORAM_ABAD = 13;
    public static final int MENU_RASHT = 14;
    public static final int MENU_ZAHEDAN = 15;
    public static final int MENU_ZANJAN = 16;
    public static final int MENU_SARI = 17;
    public static final int MENU_SEMNAN = 18;
    public static final int MENU_SANANDAJ = 19;
    public static final int MENU_SHAHREKORD = 20;
    public static final int MENU_SHIRAZ = 21;
    public static final int MENU_GHAZVIN = 22;
    public static final int MENU_GHOM = 23;
    public static final int MENU_KARAJ = 24;
    public static final int MENU_KERMAN = 25;
    public static final int MENU_KERMANSHAH = 26;
    public static final int MENU_GORGAN = 27;
    public static final int MENU_MASHHAD = 28;
    public static final int MENU_HAMEDAN = 29;
    public static final int MENU_YASUJ = 30;
    public static final int MENU_YAZD = 31;
    public static final Map<Integer, Integer> STATE_VALUE_KEY = new HashMap<Integer, Integer>();
    public static final int MENU_HELP = 35;
    public static final int MENU_PREFS = 34;
    public static final int MENU_ABOUT = 33;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        int aboutGroupID = 0;
        int helpGroupID = 1;
        int prefsGroupID = 2;
        int aboutItemOrder = Menu.FIRST;
        int helpItemOrder = 2;
        int prefsItemOrder = 3;
       
       
        MenuItem prefsMenuItem = menu.add(prefsGroupID, MENU_PREFS,
                prefsItemOrder, R.string.prefs_menu_title);
        MenuItem helpMenuItem = menu.add(helpGroupID, MENU_HELP, helpItemOrder,
                R.string.help_menu_title);
        MenuItem aboutMenuItem = menu.add(aboutGroupID, MENU_ABOUT,
                aboutItemOrder, R.string.about_menu_title);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        // Check for each known menu item
        case (MENU_PREFS):
            Intent i = new Intent(this, UserPreferenceActivity.class);
            startActivityForResult(i, 1);
            return true;
        case (MENU_ABOUT):
            Intent i2 = new Intent(this, AboutActivity.class);
            startActivityForResult(i2, 2);
            return true;
        case (MENU_HELP):
            Intent i3 = new Intent(this, HelpActivity.class);
            startActivityForResult(i3, 3);
            return true;
        default:
            return false;
        }
        // return false;
    }

    public static final String ATHAN_TIME_LOG = "Oghat shari";

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        // TODO Auto-generated method stub
        String gpsPerfKey = getString(R.string.gps_pref_key);
        String defaultLocationPerfKey = getString(R.string.state_location_pref_key);
        if (gpsPerfKey.equals(key)) {
            boolean isGPS = false;
            try {
                isGPS = sharedPreferences.getBoolean(key, false);
            } catch (ClassCastException e) {
                isGPS = Boolean.parseBoolean(sharedPreferences.getString(key,
                        "false"));
            }
            if (isGPS) {
                registerListener();
            } else {
                unregisterListener();
                useDefaultLocation(sharedPreferences, defaultLocationPerfKey);
            }
        } else if (defaultLocationPerfKey.equals(key)) {
            sharedPreferences.edit().putBoolean(gpsPerfKey, false);
            sharedPreferences.edit().commit();
            useDefaultLocation(sharedPreferences, key);
        } else {
            Log.w(ATHAN_TIME_LOG, "preference with key:" + key
                    + " is changed and it is not handled properly");
        }

    }

    private void useDefaultLocation(SharedPreferences perfs, String key) {
        int defLocationID = Integer.parseInt(perfs.getString(key, ""
                + MENU_TEHRAN));
        // Log.w(ATHAN_TIME_LOG,"Guz: " + defLocationID.getClass().getName());
        Location location = getLocationByID(defLocationID);
        onLocationChanged(location);
    }

    private Location getLocationByID(int locationID) {
        Location returnLocation = new Location("GPS");
        switch (locationID) {
        case MENU_TABRIZ:
            returnLocation.setLatitude(38.08d);
            returnLocation.setLongitude(46.3);

            break;
        case MENU_ORUMIYEH:
            returnLocation.setLatitude(37.53);
            returnLocation.setLongitude(45);
            break;
        case MENU_ARDABIL:
            returnLocation.setLatitude(38.25);
            returnLocation.setLongitude(48.28);
            break;
        case MENU_ESFEHAN:
            returnLocation.setLatitude(32.65);
            returnLocation.setLongitude(51.67);
            break;
        case MENU_KARAJ:
            returnLocation.setLatitude(35.82);
            returnLocation.setLongitude(50.97);
            break;
        case MENU_ILAM:
            returnLocation.setLatitude(33.63);
            returnLocation.setLongitude(46.42);
            break;
        case MENU_BUSHEHR:
            returnLocation.setLatitude(28.96);
            returnLocation.setLongitude(50.84);
            break;
        default:
        case MENU_TEHRAN:
            returnLocation.setLatitude(35.68);
            returnLocation.setLongitude(51.42);
            break;
        case MENU_SHAHREKORD:
            returnLocation.setLatitude(32.32);
            returnLocation.setLongitude(50.85);
            break;
        case MENU_BIRJAND:
            returnLocation.setLatitude(32.88);
            returnLocation.setLongitude(59.22);
            break;
        case MENU_MASHHAD:
            returnLocation.setLatitude(34.3);
            returnLocation.setLongitude(59.57);
            break;
        case MENU_BOJNURD:
            returnLocation.setLatitude(37.47);
            returnLocation.setLongitude(57.33);
            break;
        case MENU_AHVAZ:
            returnLocation.setLatitude(31.52);
            returnLocation.setLongitude(48.68);
            break;
        case MENU_ZANJAN:
            returnLocation.setLatitude(36.67);
            returnLocation.setLongitude(48.48);
            break;
        case MENU_SEMNAN:
            returnLocation.setLatitude(35.57);
            returnLocation.setLongitude(53.38);
            break;
        case MENU_ZAHEDAN:
            returnLocation.setLatitude(29.5);
            returnLocation.setLongitude(60.85);
            break;
        case MENU_SHIRAZ:
            returnLocation.setLatitude(29.62);
            returnLocation.setLongitude(52.53);
            break;
        case MENU_GHAZVIN:
            returnLocation.setLatitude(36.45);
            returnLocation.setLongitude(50);
            break;
        case MENU_GHOM:
            returnLocation.setLatitude(34.65);
            returnLocation.setLongitude(50.95);
            break;
        case MENU_SANANDAJ:
            returnLocation.setLatitude(35.3);
            returnLocation.setLongitude(47.02);
            break;
        case MENU_KERMAN:
            returnLocation.setLatitude(30.28);
            returnLocation.setLongitude(57.06);
            break;
        case MENU_KERMANSHAH:
            returnLocation.setLatitude(34.32);
            returnLocation.setLongitude(47.06);
            break;
        case MENU_YASUJ:
            returnLocation.setLatitude(30.82);
            returnLocation.setLongitude(51.68);
            break;
        case MENU_GORGAN:
            returnLocation.setLatitude(36.83);
            returnLocation.setLongitude(54.48);
            break;
        case MENU_RASHT:
            returnLocation.setLatitude(37.3);
            returnLocation.setLongitude(49.63);
            break;
        case MENU_KHORAM_ABAD:
            returnLocation.setLatitude(33.48);
            returnLocation.setLongitude(48.35);
            break;
        case MENU_SARI:
            returnLocation.setLatitude(36.55);
            returnLocation.setLongitude(53.1);
            break;
        case MENU_ARAK:
            returnLocation.setLatitude(34.08);
            returnLocation.setLongitude(49.7);
            break;
        case MENU_BANDAR_ABAS:
            returnLocation.setLatitude(27.18);
            returnLocation.setLongitude(56.27);
            break;
        case MENU_HAMEDAN:
            returnLocation.setLatitude(34.77);
            returnLocation.setLongitude(48.58);
            break;
        case MENU_YAZD:
            returnLocation.setLatitude(31.90);
            returnLocation.setLongitude(54.37);
            break;

        }
        return returnLocation;
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        unregisterListener();
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
       int id = v.getId();
       PanelSwitcher panel = (PanelSwitcher)findViewById(R.id.panelswitch);
       switch(id){
       case R.id.previous:
           panel.moveLeft();
           break;
       case R.id.nextBtn:
           panel.moveRight();
       }
    }

}