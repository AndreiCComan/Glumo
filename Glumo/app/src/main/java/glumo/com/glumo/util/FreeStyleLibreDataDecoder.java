package glumo.com.glumo.util;

import java.io.IOException;

/**
 * this class decodes the data received from a Free Style Libre Glucose Sensor
 */
public class FreeStyleLibreDataDecoder {

    /**
     * for now, just return the given string checking if it's castable to an Integer
     * @param encodedData the glucose data encoded
     * @return the glucose data decode
     */
    public static String decodeData (String encodedData) {
        try {
            Integer.valueOf(encodedData);
        }
        catch (Exception e) {
            encodedData = "-1";
        }
        return encodedData;
    }
}
