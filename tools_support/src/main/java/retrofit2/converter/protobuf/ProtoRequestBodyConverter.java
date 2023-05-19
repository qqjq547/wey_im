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

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import framework.telegram.support.BaseApp;
import framework.telegram.support.system.network.http.HttpReq;
import framework.telegram.support.tools.BitUtils;
import framework.telegram.support.tools.Helper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Converter;
import yourpet.client.android.sign.NativeLibUtil;

final class ProtoRequestBodyConverter implements Converter<HttpReq, RequestBody> {
    private static final MediaType MEDIA_TYPE = MediaType.get("application/x-protobuf");

    @Override
    public RequestBody convert(HttpReq value) throws IOException {
        byte[] head = new byte[2];

        head[0] = BitUtils.setBitValue(head[0], 7, (byte) 1);//固定头
        head[0] = BitUtils.setBitValue(head[0], 6, (byte) 1);//固定头

        head[0] = BitUtils.setBitValue(head[0], 5, (byte) 0);//版本号
        head[0] = BitUtils.setBitValue(head[0], 4, (byte) 0);//版本号
        head[0] = BitUtils.setBitValue(head[0], 3, (byte) 0);//版本号
        head[0] = BitUtils.setBitValue(head[0], 2, (byte) 0);//版本号
        head[0] = BitUtils.setBitValue(head[0], 1, (byte) 0);//版本号
        head[0] = BitUtils.setBitValue(head[0], 0, (byte) 1);//版本号

        head[1] = BitUtils.setBitValue(head[1], 7, (byte) 1);//加密
        head[1] = BitUtils.setBitValue(head[1], 6, (byte) 0);//压缩

        ByteBuffer contentBuffer = null;
        byte[] content = value.getData().toByteArray();
        if (content != null) {
            Log.e("Request", value.getData().toString());
            content = NativeLibUtil.getInstance().sign1(BaseApp.app, BaseApp.Companion.getIS_TEST_SERVER(), content, 1);
//            content = ZipUtils.gZip(content);
            contentBuffer = ByteBuffer.allocate(2 + 4 + content.length);
        } else {
            contentBuffer = ByteBuffer.allocate(2 + 4);
        }
        contentBuffer.put(head);
        if (content != null) {
            contentBuffer.put(Helper.int2Bytes(content.length));
            contentBuffer.put(content);
        } else {
            contentBuffer.put(Helper.int2Bytes(0));
        }
        return RequestBody.create(MEDIA_TYPE, contentBuffer.array());
    }
}
