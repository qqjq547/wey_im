/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2.converter.protobuf;

import androidx.annotation.Nullable;

import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;

import java.io.IOException;

import framework.telegram.support.BaseApp;
import framework.telegram.support.system.log.AppLogcat;
import framework.telegram.support.tools.BitUtils;
import framework.telegram.support.tools.ZipUtils;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import yourpet.client.android.sign.NativeLibUtil;

public final class ProtoResponseBodyConverter<T extends MessageLite>
        implements Converter<ResponseBody, T> {
    private final Parser<T> parser;
    private final @Nullable
    ExtensionRegistryLite registry;

    ProtoResponseBodyConverter(Parser<T> parser, @Nullable ExtensionRegistryLite registry) {
        this.parser = parser;
        this.registry = registry;
    }

    @Override
    public T convert(ResponseBody value) throws IOException {
        try {
            byte[] data = value.bytes();
            byte[] contentBuffer = parseRespBytes(data);
            if (contentBuffer != null) {
                T result = registry == null ? parser.parseFrom(contentBuffer) : parser.parseFrom(contentBuffer, registry);
                AppLogcat.INSTANCE.getLogger().d("HttpResp", result.toString());
                return result;
            } else {
                throw new RuntimeException();
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            value.close();
        }
    }

    public static byte[] parseRespBytes(byte[] data) {
        try {
            if (BitUtils.checkBitValue(data[0], 7) && BitUtils.checkBitValue(data[0], 6)) {
                if (!BitUtils.checkBitValue(data[0], 5)
                        && !BitUtils.checkBitValue(data[0], 4)
                        && !BitUtils.checkBitValue(data[0], 3)
                        && !BitUtils.checkBitValue(data[0], 2)
                        && !BitUtils.checkBitValue(data[0], 1)
                        && !BitUtils.checkBitValue(data[0], 0)) {
                    byte[] contentBuffer = new byte[data.length - 6];
                    System.arraycopy(data, 6, contentBuffer, 0, contentBuffer.length);
                    boolean isEncrypt = BitUtils.checkBitValue(data[1], 7);
                    boolean isGzip = BitUtils.checkBitValue(data[1], 6);

                    if (isGzip) {
                        contentBuffer = ZipUtils.unGZip(contentBuffer);
                    }

                    if (isEncrypt) {
                        contentBuffer = NativeLibUtil.getInstance().sign1(BaseApp.app, BaseApp.Companion.getIS_TEST_SERVER(), contentBuffer, 2);
                    }

                    return contentBuffer;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
