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
package com.github.lianjiatech.retrofit.plus.core;

import com.github.lianjiatech.retrofit.plus.exception.FailResponseException;
import com.github.lianjiatech.retrofit.plus.exception.HttpExecuteException;
import com.github.lianjiatech.retrofit.plus.exception.HttpIOException;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;


/**
 * 同步调用，如果返回的http状态码是是成功，返回responseBody 反序列化之后的对象 <br>
 * 否则，抛出异常！异常信息中包含请求和响应相关信息。
 *
 * @author 陈添明
 */
public final class BodyCallAdapterFactory extends CallAdapter.Factory {

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (Call.class.isAssignableFrom(getRawType(returnType))) {
            return null;
        }
        if (CompletableFuture.class.isAssignableFrom(getRawType(returnType))) {
            return null;
        }
        if (Response.class.isAssignableFrom(getRawType(returnType))) {
            return null;
        }
        return new BodyCallAdapter(returnType);
    }

    final class BodyCallAdapter<R> implements CallAdapter<R, R> {

        private Type returnType;

        public BodyCallAdapter(Type returnType) {
            this.returnType = returnType;
        }

        @Override
        public Type responseType() {
            return returnType;
        }

        @Override
        public R adapt(Call<R> call) {
            Response<R> response;
            Request request = call.request();
            try {
                response = call.execute();
            } catch (IOException e) {
                RequestHolder requestHolder = new RequestHolder(request);
                throw new HttpIOException(requestHolder, null, e);
            } catch (Exception e) {
                RequestHolder requestHolder = new RequestHolder(request);
                throw new HttpExecuteException(requestHolder, null, e);
            }
            if (!response.isSuccessful()) {
                RequestHolder requestHolder = new RequestHolder(request);
                okhttp3.Response raw = response.raw();
                ResponseBody errorBody = response.errorBody();
                ResponseHolder responseHolder = new ResponseHolder(raw, errorBody);
                throw new FailResponseException(requestHolder, responseHolder);
            }
            return response.body();
        }
    }
}
