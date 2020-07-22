package com.example.namecardexchange;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.example.namecardexchange.DBHelper.TB1;
import static com.example.namecardexchange.Function.byte2HexStr;
import static com.example.namecardexchange.Function.hexToAscii;
import static com.example.namecardexchange.MainActivity.DH;
import static com.example.namecardexchange.MainActivity.TAG;
import static com.example.namecardexchange.MainActivity.data_list;
import static com.example.namecardexchange.MainActivity.list_device;
import static com.example.namecardexchange.MainActivity.list_device_detail;
import static com.example.namecardexchange.MainActivity.matrix;
import static com.example.namecardexchange.MainActivity.mean_total;
import static com.example.namecardexchange.MainActivity.num_list;
import static com.example.namecardexchange.MainActivity.num_time;
import static com.example.namecardexchange.MainActivity.num_total;
import static com.example.namecardexchange.MainActivity.peripheralTextView;
import static com.example.namecardexchange.MainActivity.time_interval;
import static com.example.namecardexchange.MainActivity.time_previous;


//TODO 都是利用收到下一個封包當作觸發裝置
public class Service_scan_function {
    private static SimpleDateFormat f = new SimpleDateFormat("YYYY-MM-dd,HH:mm:ss.SS");

    //限單一裝置
    static ArrayList<Calendar> received_time_Calendar = new ArrayList<>();
    static ArrayList<String> received_time = new ArrayList<>();
    static ArrayList<Long> received_time_interval = new ArrayList<>();

    static ArrayList<Integer> rssi_level_1 = new ArrayList<>();  //rssi>-70
    static ArrayList<Integer> rssi_level_2 = new ArrayList<>();  //-70>rssi>-90
    static ArrayList<Integer> rssi_level_3 = new ArrayList<>();  //rssi<-90

    static String name, phone, email, company, position, other;


    static ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result){
            String id;
            int total,order;
            String received_data = byte2HexStr(Objects.requireNonNull(Objects.requireNonNull(result.getScanRecord()).getManufacturerSpecificData(0xffff)));
//            Log.e(TAG,"received_data: "+ received_data);
            order = Array.getByte(Objects.requireNonNull(result.getScanRecord().getManufacturerSpecificData(0xffff)), 0);
            total = Array.getByte(Objects.requireNonNull(result.getScanRecord().getManufacturerSpecificData(0xffff)), 1);
            id = received_data.subSequence(2,12).toString();
//            Log.e(TAG,"order: "+ order + " ; " + "total: " + total + " ; " + "id: " + id);

            received_data = received_data.subSequence(12,received_data.length()).toString();
//            Log.e(TAG,"received_data: "+ received_data);



            Calendar a = Calendar.getInstance();
            String currentTime = f.format(a.getTime());

            /*------------------------------------------------------------message-------------------------------------------------------------------------*/
            String msg;

            result.getTimestampNanos();
            msg="Device Name: " + result.getDevice().getName() +"\n"+ "rssi: " + result.getRssi() +"\n" + "add: " + result.getDevice().getAddress() +"\n"
                    + "time: " + currentTime +"\n" + "data: " + received_data +"\n\n";

            peripheralTextView.append(msg);

            // auto scroll for text view
            final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0)
                peripheralTextView.scrollTo(0, scrollAmount);

            /*----------------------------------------------------------message END-----------------------------------------------------------------------*/



            /*-------------------------------------------------------interval-----------------------------------------------------------------------------*/


            if(!list_device.contains(id)){
                list_device.add(id);
                list_device_detail.add(msg);
            }


        /*
        -----------|---------------------
        address    |
        -----------|---------------------
        time_pre   |
        -----------|---------------------
        time_now   |
        -----------|---------------------
        interval   |
        -----------|---------------------
        num        |
        -----------|---------------------
        mean_total |
        -----------|---------------------
        mean       |
        -----------|---------------------
        */
            int rssi = result.getRssi();
            int level;
            if(rssi>-70){
                level = 1;
            }else if(rssi>-90){
                level = 2;
            }else {
                level = 3;
            }

            long TimestampMillis = result.getTimestampNanos()/1000000; //單位:ms
            int index = list_device.indexOf(id);
            int initial = 0;
            if(matrix.get(0).size()<list_device.size()){
                matrix.get(0).add(index);                 //address
                matrix.get(1).add(initial);               //time_pre
                matrix.get(2).add(TimestampMillis);       //time_now
                matrix.get(3).add(initial);               //interval
                matrix.get(4).add(num_total.get(index));  //num
                matrix.get(5).add(mean_total.get(index));                  //mean_total
                matrix.get(6).add(mean_total.get(index)/num_total.get(index));     //mean
                time_previous.set(index,TimestampMillis);
                num_total.set(index,num_total.get(index)+1);
                mean_total.set(index,TimestampMillis-time_previous.get(index));
                switch (level){
                    case 1:
                        rssi_level_1.add(1);
                        rssi_level_2.add(0);
                        rssi_level_3.add(0);
                        break;
                    case 2:
                        rssi_level_1.add(0);
                        rssi_level_2.add(1);
                        rssi_level_3.add(0);
                        break;
                    case 3:
                        rssi_level_1.add(0);
                        rssi_level_2.add(0);
                        rssi_level_3.add(1);
                        break;
                }
            }else {
                long interval = TimestampMillis-time_previous.get(index);
//                Log.e(TAG, "interval:"+interval);
//                Log.e(TAG, "time_previous:"+time_previous);
                mean_total.set(index,mean_total.get(index)+interval);
                matrix.get(1).set(index,time_previous.get(index));
                matrix.get(2).set(index,TimestampMillis);
                matrix.get(3).set(index,interval);
                matrix.get(4).set(index,num_total.get(index));
                matrix.get(5).set(index,mean_total.get(index));
                matrix.get(6).set(index,mean_total.get(index)/num_total.get(index));
                time_previous.set(index,TimestampMillis);
                num_total.set(index,num_total.get(index)+1);

                switch (level){
                    case 1:
                        int i = rssi_level_1.get(index);
                        rssi_level_1.set(index,i+1);
                        break;
                    case 2:
                        i = rssi_level_2.get(index);
                        rssi_level_2.set(index,i+1);
                        break;
                    case 3:
                        i = rssi_level_3.get(index);
                        rssi_level_3.set(index,i+1);
                        break;
                }
            }


            //每個不同address的time interval
            if(list_device.size()>time_interval.size()){  //list_device or imei
                time_interval.add(new ArrayList<>());
                num_time.add(new ArrayList<Long>());
                num_list.add(new ArrayList<Long>());
                data_list.add(new ArrayList<String>());
            }

            time_interval.get(index).add(matrix.get(3).get(index));

            //重組segmentation
            Log.e(TAG,"received_data.length: "+received_data.length());
            if(received_data.length()==42) {
                if (data_list.get(index).isEmpty()) {
                    for (int i = 0; i < total; i++) {
                        data_list.get(index).add("0");
                        num_list.get(index).add((long) 0);
                    }
                }
                if (!data_list.get(index).get(order - 1).equals(received_data)) {
                    data_list.get(index).set(order - 1, received_data);
                }
                if (!data_list.get(index).contains("0") && !data_list.get(index).contains("finish")) {
                    data_list.get(index).add("finish");
                    String regroup = "";
                    for (int i = 0; i < total; i++) {
                        regroup = regroup + data_list.get(index).get(i);
                    }
                    Log.e(TAG, "regroup:" + hexToAscii(regroup));
                    split(hexToAscii(regroup));
                }

                if(!data_list.get(index).contains(received_data)){
                    data_list.get(index).set(order - 1, received_data);
                    data_list.get(index).remove("finish");
                }
            }
            //重組結束

//            Log.e(TAG,"data_list: "+data_list);


//            num_list.get(index).set(order-1,result.getTimestampNanos()/1000000);
//            if(num_list.get(index).size()==16 && !num_list.get(index).contains((long)0)){
//                long max = Collections.max(num_list.get(index));
//                long min = Collections.min(num_list.get(index));
//                num_time.get(index).add(max-min);
//                Log.e(TAG,"device: "+list_device.get(index));
//                Log.e(TAG,"num_list: "+num_list.get(index));
//                Log.e(TAG,"num_time: "+num_time.get(index));
//            }




//            Log.e(TAG,"matrix: "+"\n"
//                    +matrix.get(0).toString()+"\n"
//                    +matrix.get(1).toString()+"\n"
//                    +matrix.get(2).toString()+"\n"
//                    +matrix.get(3).toString()+"\n"
//                    +matrix.get(4).toString()+"\n"
//                    +matrix.get(5).toString()+"\n"
//                    +matrix.get(6).toString()+"\n");

            //單一裝置time interval
            received_time_Calendar.add(a);
            received_time_interval.clear();
            for(int i = 0;i<received_time_Calendar.size();i++){
                received_time.add(f.format(received_time_Calendar.get(i).getTime()));
                if(i>0){
                    received_time_interval.add(time_difference_(received_time_Calendar.get(i-1),received_time_Calendar.get(i)));
                }
            }
//            Log.e(TAG,"received_time_interval.length: "+received_time_interval.size());
//            Log.e(TAG,"received_time_interval"+received_time_interval);

            /*-------------------------------------------------------interval END--------------------------------------------------------------------------*/

        }


        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d("onScanFailed: " , String.valueOf(errorCode));
        }
    };

//    public static void add(String s,Calendar first,Calendar last,int rssi_1,int rssi_2,int rssi_3) {
//        SQLiteDatabase db = DH.getReadableDatabase();
//        ContentValues values = new ContentValues();
//        values.put("user_id",s);
//        values.put("time_first",format.format(first.getTime()));
//        values.put("time_last",format.format(last.getTime()));
//        values.put("rssi_level_1",rssi_1);
//        values.put("rssi_level_2",rssi_2);
//        values.put("rssi_level_3",rssi_3);
//        values.put("is_contact",0);
//        db.insert(TB1,null,values);
//        show(db);
//    }

//    public static void delete(String _id){
//        SQLiteDatabase db = DH.getWritableDatabase();
//        db.delete(TB1,"_id=?",new String[]{_id});
//        show(db);
//    }

//    private static void show(SQLiteDatabase db){
//        Cursor cursor = db.query(TB1,new String[]{"_id","user_id","time_first","time_last","rssi_level_1","rssi_level_2","rssi_level_3","is_contact"},
//                null,null,null,null,null);
//
//        StringBuilder resultData = new StringBuilder("RESULT: \n");
//        while(cursor.moveToNext()){
//            int id = cursor.getInt(0);
//            String user_id = cursor.getString(1);
//            String time_first = cursor.getString(2);
//            String time_last = cursor.getString(3);
//            int rssi_1 = cursor.getInt(4);
//            int rssi_2 = cursor.getInt(5);
//            int rssi_3 = cursor.getInt(6);
//            int is_contact = cursor.getInt(7);
//
//            resultData.append(id).append(": ");
//            resultData.append(user_id).append("\n ");
//            resultData.append(time_first).append(", ");
//            resultData.append(time_last).append("\n");
//            resultData.append("RSSI level: ").append(rssi_1).append(", ");
//            resultData.append(rssi_2).append(", ");
//            resultData.append(rssi_3).append("\n ");
//            resultData.append("is check: ").append(is_contact);
//            resultData.append("\n");
//        }
//        sql_Text.setText(resultData);
//        sql_Text.setMovementMethod(new ScrollingMovementMethod()); //垂直滾動
//        cursor.close();
//    }

    private static long time_difference_(Calendar first, Calendar last){
        Date first_time = first.getTime();
        Date last_time = last.getTime();

        long different = last_time.getTime() - first_time.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;
        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;
        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;
        long elapsedSeconds = different / secondsInMilli;
//        Log.e(TAG,"different: "+elapsedDays +"days, " + elapsedHours + "hours, " + elapsedMinutes +"minutes, " + elapsedSeconds +"seconds. ");
        return different;
    }

    private static void split(String string){
        String[] parts = string.split("\\:");
        name = parts[0];
        phone = parts[1];
        email = parts[2];
        company = parts[3];
        position = parts[4];
        other = parts[5];

        Log.e(TAG,"name: "+ name + "\n"
                + "phone: " + phone + "\n"
                + "email: " + email + "\n"
                + "company: " + company + "\n"
                + "position: " + position + "\n"
                + "other: " + other );

        StringBuilder resultData = new StringBuilder("");
        resultData.append(name).append(phone).append(email).append(company).append(position).append(other);
        if(!compare_database(resultData.toString())){
            add(name,phone,email,company,position,other);
        }

    }

    public static void add(String n,String p,String e, String c, String po, String o) {
        SQLiteDatabase db = DH.getReadableDatabase();
        ContentValues values = new ContentValues();

        values.put("NAME",n);
        values.put("PHONE",p);
        values.put("EMAIL",e);
        values.put("COMPANY",c);
        values.put("POSITION",po);
        values.put("OTHER",o);
        db.insert(TB1,null,values);
        show(db);
    }

    private static void show(SQLiteDatabase db){
        Cursor cursor = db.query(TB1,new String[]{"_id","NAME","PHONE","EMAIL","COMPANY","POSITION","OTHER"},
                null,null,null,null,null);

        StringBuilder resultData = new StringBuilder("RESULT: \n");
        while(cursor.moveToNext()){
            int _id = cursor.getInt(0);
            String n = cursor.getString(1);
            String p = cursor.getString(2);
            String e = cursor.getString(3);
            String c = cursor.getString(4);
            String po = cursor.getString(5);
            String o = cursor.getString(6);


            resultData.append("\n").append(_id).append("\n");
            resultData.append("name: ").append(n).append("\n");
            resultData.append("phone: ").append(p).append("\n");
            resultData.append("email: ").append(e).append("\n");
            resultData.append("company: ").append(c).append("\n");
            resultData.append("position: ").append(po).append("\n");
            resultData.append("other: ").append(o).append("\n");

        }

        Log.e(TAG,"resultData: " + resultData );
//        sql_Text.setText(resultData);
//        sql_Text.setMovementMethod(new ScrollingMovementMethod()); //垂直滾動
        cursor.close();
    }

    private static boolean compare_database(String data){
        SQLiteDatabase db = DH.getReadableDatabase();
        Cursor cursor = db.query(TB1,new String[]{"_id","NAME","PHONE","EMAIL","COMPANY","POSITION","OTHER"},
                null,null,null,null,null);
        StringBuilder resultData = new StringBuilder("");
        boolean b =false;
        while(cursor.moveToNext()){
            String n = cursor.getString(1);
            String p = cursor.getString(2);
            String e = cursor.getString(3);
            String c = cursor.getString(4);
            String po = cursor.getString(5);
            String o = cursor.getString(6);
            resultData.append(n).append(p).append(e).append(c).append(po).append(o);
            if(data.equals(resultData.toString())){
                b=true;
                Log.e(TAG,"一樣");
            }

        }
        cursor.close();
        return b;
    }
}
