package com.mservice.momo.vertx.processor;

/**
 * Created by duyhuynh on 19/04/2016.
 */
public class Base64 {
    private char[] sBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    public Base64() {
    }

    public byte[] decode(String aSource) {
        int aSource_length;
        for(aSource_length = aSource.length(); aSource_length > 0 && aSource.charAt(aSource_length - 1) == 61; --aSource_length) {
            ;
        }

        if(aSource_length % 4 == 1) {
            return null;
        } else {
            int len = aSource_length / 4 * 3 + aSource_length % 4 - (aSource_length % 4 == 0?0:1);
            byte[] result = new byte[len];
            int o_ofs = 0;
            int i_ofs = 0;

            while(i_ofs < aSource_length) {
                int i24 = 0;

                for(int bscan = 0; bscan < 4; ++bscan) {
                    if(i_ofs < aSource_length) {
                        int decode = this.decode(aSource.charAt(i_ofs));
                        if(decode == -1) {
                            return null;
                        }

                        i24 = (i24 << 6) + decode;
                    } else {
                        i24 <<= 6;
                    }

                    ++i_ofs;
                }

                if(o_ofs < len) {
                    result[o_ofs++] = (byte)(i24 >> 16);
                }

                if(o_ofs < len) {
                    result[o_ofs++] = (byte)(i24 >> 8);
                }

                if(o_ofs < len) {
                    result[o_ofs++] = (byte)i24;
                }
            }

            return result;
        }
    }

    private int decode(char c) {
        return c >= 65 && c <= 90?c - 65:(c >= 97 && c <= 122?c - 97 + 26:(c >= 48 && c <= 57?c - 48 + 52:(c == 43?62:(c == 47?63:-1))));
    }

    public String encode(byte[] aSource) {
        int result_length = aSource.length * 4 / 3 + (aSource.length % 3 == 0?0:1);
        char[] result = new char[(aSource.length + 2) / 3 * 4];
        int o_ofs = 0;
        int i_ofs = 0;

        while(i_ofs < aSource.length) {
            int i24 = 0;

            for(int bscan = 0; bscan < 3; ++bscan) {
                if(i_ofs >= aSource.length) {
                    i24 <<= 8;
                } else {
                    i24 = (i24 << 8) + (aSource[i_ofs] & 255);
                }

                ++i_ofs;
            }

            if(o_ofs < result_length) {
                result[o_ofs++] = this.sBase64[i24 >> 18 & 63];
            }

            if(o_ofs < result_length) {
                result[o_ofs++] = this.sBase64[i24 >> 12 & 63];
            }

            if(o_ofs < result_length) {
                result[o_ofs++] = this.sBase64[i24 >> 6 & 63];
            }

            if(o_ofs < result_length) {
                result[o_ofs++] = this.sBase64[i24 & 63];
            }
        }

        while(o_ofs < result.length) {
            result[o_ofs++] = 61;
        }

        return new String(result);
    }
}
