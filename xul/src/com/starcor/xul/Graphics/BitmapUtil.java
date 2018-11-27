package com.starcor.xul.Graphics;

import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

/**
 * Created by skycnlr on 2018/5/31.
 */
public class BitmapUtil {
    private static final int MARK_BYTES = 64*1024;
    private static ThreadLocal<MarkInputStream> _local_decode_stream;

    static {
        _local_decode_stream = new ThreadLocal<MarkInputStream>();
    }

    public static InputStream decodeStream(InputStream is, BitmapFactory.Options opts){
        if (is == null) {
            return null;
        }
        MarkInputStream _is = _local_decode_stream.get();
        if (_is == null) {
            _is = new MarkInputStream(is, MARK_BYTES);
            _local_decode_stream.set(_is);
        } else {
            _is.setInputStream(is, MARK_BYTES);
        }

        try {
            switch (_is.readByte()) {
                case 0x89:
                    if(_is.readByte() == 0x50
                            && _is.readByte() == 0x4e
                            && _is.readByte() == 0x47
                            && _is.readByte() == 0x0d
                            && _is.readByte() == 0x0a
                            && _is.readByte() == 0x1a
                            && _is.readByte() == 0x0a) {
                        _is.skipBytes(8);
                        opts.outWidth = _is.readInt();
                        opts.outHeight = _is.readInt();
                    } else {
                        return doSystemDecode(_is, opts);
                    }
                    break;
                case 0xff:
                    if (_is.readByte() == 0xd8) {
                        decodeJPGHeader(_is, null, opts);
                        if (opts.outHeight == 0 || opts.outWidth == 0) {
                            _is.reset();

                            _is.setMarkLimit(Integer.MAX_VALUE);
                            _is.skipBytes(2);

                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                            os.write(0xff);
                            os.write(0xd8);
                            decodeJPGHeader(_is, os, opts);
                            return new BitmapInputStream(is, os.toByteArray());
                        }
                    }  else {
                        return doSystemDecode(_is, opts);
                    }
                    break;
                case 0x42:
                    if (_is.readByte() == 0x4d) {
                        _is.skipBytes(4 + 2 + 2 + 4 + 4);
                        opts.outWidth = _is.readInt();
                        opts.outHeight = _is.readInt();
                    }  else {
                        return doSystemDecode(_is, opts);
                    }
                    break;
                default:
                    return doSystemDecode(_is, opts);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static InputStream doSystemDecode(MarkInputStream localIs, BitmapFactory.Options opts) {
        if (localIs != null) {
            BitmapFactory.decodeStream(localIs.is, null, opts);
        }
        return null;
    }

    private static boolean decodeJPGHeader(MarkInputStream is, OutputStream os, BitmapFactory.Options options){
        while (true) {
            try {
                int start = is.readByte();
                if (start == -1) {
                    break;
                }
                if (start != 0xff) {
                    write(os, start);
                    continue;
                }
                int type = is.readByte();
                int packageSize = is.readShort();
                switch (type) {
                    case 0xc0:
                    case 0xc1:
                    case 0xc2:
                    case 0xc3:
                        int skipByte = is.readByte();
                        options.outHeight = is.readShort();
                        options.outWidth = is.readShort();
                        write(os, start);
                        write(os, type);
                        write(os, (packageSize >> 8) & 0xff);
                        write(os, (packageSize & 0xff));
                        write(os, skipByte);

                        write(os, (options.outHeight >> 8) & 0xff);
                        write(os, (options.outHeight & 0xff));
                        write(os, (options.outWidth >> 8) & 0xff);
                        write(os, (options.outWidth & 0xff));
                        while (packageSize - 7 > 0) {
                            write(os, is.readByte());
                            packageSize--;
                        }
                        return true;
                    case 0xe0:
                    case 0xe1:
                    case 0xe2:
                    case 0xe3:
                    case 0xe4:
                    case 0xe5:
                    case 0xe6:
                    case 0xe7:
                    case 0xe8:
                    case 0xe9:
                    case 0xea:
                    case 0xeb:
                    case 0xec:
                    case 0xed:
                    case 0xee:
                    case 0xef:
                        is.skipBytes(packageSize - 2);
                        break;
                    case 0xd8:
                        break;
                    default:
                        write(os, start);
                        write(os, type);
                        write(os, (packageSize >> 8) & 0xff);
                        write(os, (packageSize & 0xff));
                        while (packageSize - 72 > 0) {
                            write(os, is.readByte());
                            packageSize--;
                        }
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private static void write(OutputStream out, int value) {
        if (out != null) {
            try {
                out.write(value);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class MarkInputStream {
        int readLimit = Integer.MAX_VALUE;
        InputStream is;

        public MarkInputStream(InputStream is, int count) {
            setInputStream(is, count);
        }

        public void setInputStream(InputStream is, int count) {
            this.is = is;
            this.readLimit = count;
            this.is.mark(count);
        }

        public void setMarkLimit(int maxCount) {
            if (is == null) {
                return;
            }
            this.readLimit = maxCount;
        }

        public void reset() {
            try {
                is.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public int readByte() throws Exception {
            if (is == null) {
                return -1;
            }
            if (readLimit <= 0) {
                throw new Exception();
            }
            try {
                int v = is.read();
                readLimit--;
                return v == -1 ? -1 : (v & 0xff);
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return -1;
        }

        public int readShort() throws Exception {
            if (is == null) {
                return -1;
            }
            int b = readByte();
            int l = readByte();
            return (( b != -1) && (l != -1)) ? ((b << 8) & 0xff00) | (l & 0xff) : -1;
        }

        public int readInt() throws Exception {
            if (is == null) {
                return -1;
            }
            int f1 = readByte();
            int f2 = readByte();
            int f3 = readByte();
            int f4 = readByte();
            return (( f1 != -1) && (f2 != -1) && (f3 != -1) && (f4 != -1)) ?
                    ((f1 << 24) & 0xff000000) | ((f2 << 16) &0xff0000) 
                            | ((f3 << 8) & 0xff00) | (f4 & 0xff)
                    : -1;
        }

        public void skipBytes(int count) throws Exception {
            if (is == null || count == 0)  {
                return;
            }
            if (readLimit < count) {
                throw new Exception();
            }
            try {
                is.skip(count);
                readLimit -= count;
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
     public static class BitmapInputStream extends InputStream{
        int len;
         byte[] buffer; //有效头信息
         InputStream is; //原始图片流

         public BitmapInputStream(InputStream is, byte[] buffer) {
             this.is = is;
             this.buffer = buffer;

         }

         @Override
         public int read() throws IOException {
             if (len < buffer.length) {
                 return buffer[len++] & 0xff;
             }
             if (is != null) {
                 return is.read();
             }
             return -1;
         }

         @Override
         public void close() throws IOException {
             if (is != null) {
                 is.close();
             }
         }
     }

}
