package com.example;

import android.util.Log;

import java.util.Objects;

class appConstants {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static final String HEX_DIGITS = "0123456789ABCDEF";

    public static final String KeyA = "A", KeyB = "B";

    private static byte g_CardMasterKey[] = { (byte) 0x97, (byte) 0x66, (byte) 0x25,
            (byte) 0x68, (byte) 0x36, (byte) 0x21 };

    public static byte g_CardAuthKeyA[] = new byte[6];
    public static byte g_CardAuthKeyB[] = new byte[6];


    public static String authKeyA = null, authKeyB = null;
    public static final int slotNum = 0, controlCode = 3500;

    public static byte balance_[] = new byte[12], txnCtr_[] = new byte[12], userID[] = new byte[12];


    public static String hexToDec(String hex) {
        char[] sources = hex.toCharArray();
        int dec = 0;
        for (int i = 0; i < sources.length; i++) {
            int digit = HEX_DIGITS.indexOf(Character.toUpperCase(sources[i]));
            dec += digit * Math.pow(16, (sources.length - (i + 1)));
        }
        return String.valueOf(dec);
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String hexToASCII(String hexValue)
    {
        StringBuilder output = new StringBuilder("");
        for (int i = 0; i < hexValue.length(); i += 2)
        {
            String str = hexValue.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }

    public static void diversify(byte[] Uid) {
        byte[] newMasterKey= g_CardMasterKey;

        byte bDivKeySet[] = new byte[8];
        for (int i = 0; i < 4; i++) {
            bDivKeySet[i] = Uid[i];
            bDivKeySet[i + 4] = Uid[i];
        }
        for (int i = 0; i < newMasterKey.length; i++)
            bDivKeySet[i] ^= newMasterKey[i];

        for (int i = 0; i < 6; i++) {
            g_CardAuthKeyA[i] = bDivKeySet[i];
            g_CardAuthKeyB[i] = bDivKeySet[i + 2];
        }

        authKeyA = bytesToHex(g_CardAuthKeyA);
        authKeyB = bytesToHex(g_CardAuthKeyB);

        //authKeyA = byteToString(g_CardAuthKeyA, 6);
        //authKeyB = byteToString(g_CardAuthKeyB, 6);

    }


    public static void parseData()
    {
        try{
            //userID
            String u_ID = hexToASCII(bytesToHex(userID).toString().substring(0,2)) + hexToASCII(bytesToHex(userID).toString().substring(2,4)) +
                    hexToASCII(bytesToHex(userID).toString().substring(4,6)) + hexToASCII(bytesToHex(userID).toString().substring(6,8)) +
                    hexToASCII(bytesToHex(userID).toString().substring(8,10)) + hexToASCII(bytesToHex(userID).toString().substring(10,12));

        }catch (Exception e){
            e.printStackTrace();
        }

        try{
            //balance and txnctr
            byte []bal = balance_;
            byte []txn = txnCtr_;

            //parsing balance
            byte lsb=bal[3];
            bal[3]=bal[0];
            bal[0]=lsb;

            lsb=bal[2];
            bal[2]=bal[1];
            bal[1]=lsb;

            //parsing txn
            lsb=txn[3];
            txn[3]=txn[0];
            txn[0]=lsb;

            lsb=txn[2];
            txn[2]=txn[1];
            txn[1]=lsb;


            float parsedResult = (int) Long.parseLong(bytesToHex(bal), 16);
            float balan=parsedResult/100;
            //int txnn = (int) Long.parseLong(bytesToHex(txn), 16);
            //int txnn = (int) Integer.parseInt(hexToDec(bytesToHex(txn)), 16);
            String txnn = hexToDec(bytesToHex(txn));

            //Log.i(TAG, "bal:" + String.valueOf(balan));
            //Log.i(TAG, "Txn Ctr:" + String.valueOf(txnn));

        }catch (Exception e){
            e.printStackTrace();
        }
    }


}


class APDU_build {

    //Block Type
    public static String block_type_binary = "B0";
    public static String block_type_value = "B1";

    //Blocks
    public static String block_0C = "0C";
    public static String block_0D = "0D";
    public static String block_0E = "0E";
    public static String block_0F = "0F";

    //Sizes
    public static String size_bytes_02 = "02";
    public static String size_bytes_04 = "04";
    public static String size_bytes_06 = "06";
    public static String size_bytes_08 = "08";

    //Txn Types
    public static String txn_op_topup = "01";
    public static String txn_op_sale = "02";

    private static String key_loc_A = "00", key_loc_B = "01";

    public static String storeKey(String keyType, String Key)
    {
        return "FF8200" + ((Objects.equals(keyType, appConstants.KeyA)) ? "00" : "01") + size_bytes_06 + Key;
    }

    public static String authenticate(String keyType, String block)
    {
        return "FF860000050100" + block + ((Objects.equals(keyType, appConstants.KeyA)) ? "6000" : "6101");
    }

    public static String readBlock(String blockType, String block, String size)
    {
        return "FF" + blockType + "00" + block + size;
    }

    public static String transact(String block, String txn_op, int amount )
    {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toHexString(amount * 100));
        String amountHex = sb.toString();
        amountHex = ("00000000" + amountHex).substring(amountHex.length());

        return "FFD700" + block + "05" + txn_op + amountHex;
    }

}