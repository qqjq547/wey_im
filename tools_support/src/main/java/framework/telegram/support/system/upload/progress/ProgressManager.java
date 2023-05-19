package framework.telegram.support.system.upload.progress;


import java.io.File;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Created by lzh on 20-1-13.
 * INFO:
 */
public class ProgressManager {

    public static RequestBody createCustomRequestBody(final MediaType contentType, final File file, final ProgressListener listener) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return contentType;
            }


            @Override
            public void writeTo(BufferedSink sink) {
                Source source;
                try {
                    source = Okio.source(file);
                    //sink.writeAll(source);
                    Buffer buf = new Buffer();
                    Long fileSize = file.length();
                    Long remaining = fileSize;
                    for (long readCount; (readCount = source.read(buf, 2048 * 4)) != -1; ) {
                        sink.write(buf, readCount);
                        listener.onProgress(fileSize, remaining -= readCount, remaining == 0);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

}
