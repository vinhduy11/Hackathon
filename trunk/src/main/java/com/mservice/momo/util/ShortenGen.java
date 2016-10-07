package com.mservice.momo.util;

import java.util.ArrayList;

/**
 * Created by concu on 5/7/14.
 */
public class ShortenGen {

    //don't modify any more
    private static char[] a0="0147852369zaqwsxcderfvbgtyhnmjukliop".toCharArray();
    private static char[] a1="qazwsxedcrfvtgbyhnujmikolp7410852963".toCharArray();
    private static char[] a2="poiuytrewqasdfghjklmnbvcxz9632587410".toCharArray();
    private static char[] a3="mnbvcxzasdfghjklpoiuytrewq3698741025".toCharArray();
    private static char[] a4="7qawes48zxdrt159fcvgy0263bhuijnmkopl".toCharArray();

    private static char[] a5="l0kj2hgf1ds5azx4cvbn6mpo7iuy8tre9wq0".toCharArray();
    private static char[] a6="q9aw5zs8exd0rcftv7gybh6unj1im4k2olp3".toCharArray();

    private static char[] a7="rcftv7gybh6unj1im4k2olp3q9aw5zs8exd0".toCharArray();
    private static char[] a8="s8exd0rcftv7gybh6unjq9aw5z1im4k2olp3".toCharArray();
    private static char[] a9="xza425sdfm10nbvklpoi9cghju87ytrewq36".toCharArray();

    private static ArrayList<char[]> arrayList = new ArrayList<>();
    static {
        arrayList.add(a0);
        arrayList.add(a1);
        arrayList.add(a2);
        arrayList.add(a3);
        arrayList.add(a4);
        arrayList.add(a5);
        arrayList.add(a6);
        arrayList.add(a7);
        arrayList.add(a8);
        arrayList.add(a9);
    }

    private static int maxValue= 36*36*36*36*36;

    public static String getCode(int number, int len){
        number = number % maxValue;
        int[] indexs = new int[len];

        int du = number;

        for(int i=0;i<len;i++){
            indexs[i] = du%36;
            du = du/36;
        }

        String result ="";
        for (int i=0;i<len;i++){
            if(len ==5){
                result +=arrayList.get(i)[indexs[i]];
            }else{
                result +=arrayList.get(len -(1 +i))[indexs[i]];
            }

        }

        return  result;
        /*//ma khuyen mai
        if(len == 6){
            return a6[indexs[0]] + "" +
                    a5[indexs[1]] + "" +
                    a4[indexs[2]] + "" +
                    a3[indexs[3]] + "" +
                    a2[indexs[4]] + "" +
                    a1[indexs[5]] + "";
        }

        if(len == 5){
            return  a0[indexs[0]] + "" +
                    a1[indexs[1]] + "" +
                    a2[indexs[2]] + "" +
                    a3[indexs[3]] + "" +
                    a4[indexs[4]];
        }


        int index0;
        int index1;
        int index2;
        int index3;
        int index4;

        int n1 = number/36;
        index0 = number%36;

        int n2 = n1/36;
        index1 = n1%36;

        int n3 = n2/36;
        index2 = n2%36;

        index4 = n3/36;
        index3 = n3%36;*/

    }
}
