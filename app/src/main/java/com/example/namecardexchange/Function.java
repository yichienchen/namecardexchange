package com.example.namecardexchange;

public class Function {
    static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }

    static String byte2HexStr(byte[] b) {
        String stmp="";
        StringBuilder sb = new StringBuilder("");

        for (int n=0;n<b.length;n++) {
            stmp = Integer.toHexString(b[n] & 0xFF);
            sb.append((stmp.length()==1)? "0"+stmp : stmp);
        }
        return sb.toString().trim();
    }

    static byte intToByte(int x) {
        return (byte) x;
    }

}
