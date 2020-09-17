package com.example.namecardexchange;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static com.example.namecardexchange.DBHelper.TB1;
import static com.example.namecardexchange.Function.byte2HexStr;
import static com.example.namecardexchange.Function.hexToAscii;
import static com.example.namecardexchange.MainActivity.DH;
import static com.example.namecardexchange.MainActivity.TAG;
import static com.example.namecardexchange.MainActivity.data_list;
import static com.example.namecardexchange.MainActivity.list_device;
import static com.example.namecardexchange.MainActivity.num_list;
import static com.example.namecardexchange.MainActivity.peripheralTextView;


public class Service_scan_function {
    static String name, phone, email, company, position, other;

    static ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result){

            String id;
            int total,order;
            String received_data = byte2HexStr(Objects.requireNonNull(Objects.requireNonNull(result.getScanRecord()).getManufacturerSpecificData(0xffff)));
            String received_data_rsp = byte2HexStr(Objects.requireNonNull(Objects.requireNonNull(result.getScanRecord()).getManufacturerSpecificData(0xfff1)));

            received_data = received_data + received_data_rsp;


            order = Array.getByte(Objects.requireNonNull(result.getScanRecord().getManufacturerSpecificData(0xffff)), 0);
            total = Array.getByte(Objects.requireNonNull(result.getScanRecord().getManufacturerSpecificData(0xffff)), 1);
            id = received_data.subSequence(2,12).toString();

            received_data = received_data.subSequence(12,received_data.length()).toString();




            /*------------------------------------------------------------message-------------------------------------------------------------------------*/
            String msg;


            /*----------------------------------------------------------message END-----------------------------------------------------------------------*/



            /*-------------------------------------------------------interval-----------------------------------------------------------------------------*/


            if(!list_device.contains(id)){
                list_device.add(id);
            }

            final int index = list_device.indexOf(id);

            if(list_device.size()>num_list.size()){
                num_list.add(new ArrayList<Long>());
                data_list.add(new ArrayList<String>());
            }


            //重組segmentation
            Log.e(TAG,"received_data.length: "+received_data.length());
            if(received_data.length()==96) {
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

            int num_finish = 0;
            for(int count = 0 ; count < data_list.size() ; count ++){
                if(data_list.get(count).contains("finish")){
                    num_finish = num_finish + 1;
                }

            }


            peripheralTextView.setText("");
            msg = "There are " + data_list.size() + " user around.\n" +
                    "You have received " + num_finish + " business card!\n";
            peripheralTextView.setText(msg);


            /*-------------------------------------------------------interval END--------------------------------------------------------------------------*/

        }


        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d("onScanFailed: " , String.valueOf(errorCode));
        }
    };


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
