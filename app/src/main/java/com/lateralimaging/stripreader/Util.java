package com.lateralimaging.stripreader;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by matt on 03/08/15.
 */
public class Util {

    private static final String TAG = "Util";

    public static byte[] readAllBytes(File file) throws IOException
    {
        InputStream in = null;

        byte [] result = new byte[(int)file.length()];

        try
        {
            in = new BufferedInputStream(new FileInputStream(file));

            in.read(result);
        }
        finally
        {
            if (in != null)
            {
                in.close();
            }
        }

        return result;
    }

    public static int[] createRandomSet(ArrayList<Integer> values, int count)
    {
        int[] range = new int[count];

        for(int i = 0; values.size() > 0 && i < count; i++)
        {
            range[i] = values.remove((int)(Math.floor(Math.random() * values.size())));
        }

        return range;
    }

    public static ArrayList<Integer> createRange(int min, int max)
    {
        ArrayList<Integer> range = new ArrayList<>(max + 1);

        for(int i = min; i < max + 1; i++)
        {
            range.add(i);
        }

        return range;
    }
}
