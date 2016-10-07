package com.mservice.momo.avatar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by concu on 4/29/14.
 */
public class AvatarIO {
    public static String folder="";
    private static String encoding ="UTF-8";

    public static boolean write(String content, int number){
        boolean r = true;
        File file = new File( folder + number + ".txt");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();

        } catch (IOException e) {
            System.out.println(e.getMessage());
            r =false;
        }
        return r;
    }
    public static boolean write(byte[] bytes, int number){

        boolean r= true;
        String content = new String(bytes,0,bytes.length);

        File file = new File( folder + number + ".txt");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();

        } catch (IOException e) {
            System.out.println(e.getMessage());
            r =false;
        }
        return r;
    }

    public static String read(int number){
        String r="";
        String path = folder + number + ".txt";
        try
        {
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            r = new String(encoded, encoding);
        }catch (IOException ex){
            System.out.println(ex.getMessage());
        }
        return  r;
    }
}
