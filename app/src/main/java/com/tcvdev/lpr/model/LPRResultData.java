package com.tcvdev.lpr.model;

public class LPRResultData {

    public static int MAXPLATENUM = 5;
    public static int SIZE_OF_LPRResultData = 224;

    public class RECT{

        public int left;
        public int top;
        public int right;
        public int bottom;
    }

    public class OnePlateData{

        public RECT lprRect;
        public char lprStr[];
        public float conf;
        public int isPassed;

        public OnePlateData() {

            lprRect = new RECT();
            lprStr = new char[20];
            conf = 0;
            isPassed = 0;
        }

    }


    public int nPlateNum;
    public OnePlateData plateData[];

    public LPRResultData() {

        nPlateNum = 0;
        plateData = new OnePlateData[MAXPLATENUM];
        for (int i = 0; i < MAXPLATENUM; i++)
            plateData[i] = new OnePlateData();
    }


    public static void parseFromByte(byte[] pData, LPRResultData lprResultData) {

        int nCursor = 0;
        lprResultData.nPlateNum = BaseConvertor.byteArray2Int(pData, nCursor);
        nCursor += 4;

        for (int i = 0; i < MAXPLATENUM; i++) {

            //Rect
            lprResultData.plateData[i].lprRect.left = BaseConvertor.byteArray2Int(pData, nCursor);
            nCursor += 4;
            lprResultData.plateData[i].lprRect.top = BaseConvertor.byteArray2Int(pData, nCursor);
            nCursor += 4;
            lprResultData.plateData[i].lprRect.right = BaseConvertor.byteArray2Int(pData, nCursor);
            nCursor += 4;
            lprResultData.plateData[i].lprRect.bottom = BaseConvertor.byteArray2Int(pData, nCursor);
            nCursor += 4;

            //Str
            for (int j = 0 ; j < 20; j++) {
                lprResultData.plateData[i].lprStr[j] = (char)(pData[nCursor]);
                nCursor++;
            }

            //Conf
            lprResultData.plateData[i].conf = BaseConvertor.byteArray2Float(pData, nCursor);
            nCursor += 4;

            //isPassed;
            lprResultData.plateData[i].isPassed = BaseConvertor.byte2Int(pData, nCursor);
            nCursor += 4;
        }
    }

    public static void getByteInfo(LPRResultData lprResultData, byte[] byteInfo) {

        int nCursor = 0;

        byte[] plateNum = new byte[4];
        BaseConvertor.Int2ByteArray(lprResultData.nPlateNum, plateNum, 0);
        System.arraycopy(plateNum, 0, byteInfo, nCursor,4); nCursor += 4;

        for (int i = 0; i < MAXPLATENUM; i++) {

            //Rect;
            byte[] rectLeft = new byte[4];
            BaseConvertor.Int2ByteArray(lprResultData.plateData[i].lprRect.left, rectLeft, 0);
            System.arraycopy(rectLeft, 0, byteInfo, nCursor,4); nCursor += 4;

            byte[] rectTop = new byte[4];
            BaseConvertor.Int2ByteArray(lprResultData.plateData[i].lprRect.top, rectTop, 0);
            System.arraycopy(rectTop, 0, byteInfo, nCursor,4); nCursor += 4;

            byte[] rectRight = new byte[4];
            BaseConvertor.Int2ByteArray(lprResultData.plateData[i].lprRect.right, rectRight, 0);
            System.arraycopy(rectRight, 0, byteInfo, nCursor,4); nCursor += 4;

            byte[] rectBottom = new byte[4];
            BaseConvertor.Int2ByteArray(lprResultData.plateData[i].lprRect.bottom, rectBottom, 0);
            System.arraycopy(rectBottom, 0, byteInfo, nCursor,4); nCursor += 4;

            //Str
            byte[] strArray = new byte[20];
            String lprStr = new String(lprResultData.plateData[i].lprStr);
            strArray = lprStr.getBytes();
            System.arraycopy(strArray, 0, byteInfo, nCursor, 20); nCursor += 20;

            //Conf;
            byte[] confArray = BaseConvertor.float2ByteArray(lprResultData.plateData[i].conf);
            System.arraycopy(confArray, 0, byteInfo, nCursor, 4); nCursor += 4;

            //isPassed;
            byte[] passedArray = new byte[4];
            BaseConvertor.Int2ByteArray(lprResultData.plateData[i].isPassed, passedArray, 0);
            System.arraycopy(passedArray, 0, byteInfo, nCursor,4); nCursor += 4;


        }
    }
}
