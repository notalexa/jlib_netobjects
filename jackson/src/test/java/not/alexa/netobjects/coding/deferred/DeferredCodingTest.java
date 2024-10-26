/*
 * Copyright (C) 2024 Not Alexa
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
package not.alexa.netobjects.coding.deferred;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.ByteEncoder;
import not.alexa.netobjects.coding.json.JsonCodingScheme;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme;
import not.alexa.netobjects.types.DeferredObject;

/**
 * The test is closely related to spotify API.
 * 
 * @author notalexa
 */
@RunWith(org.junit.runners.Parameterized.class)
public class DeferredCodingTest {
	
    @Parameters
    public static List<String> testObjects() {
        return Arrays.asList(new String[] {
        		"deferred1.json",
        		"deferred2.json",
        		"deferred3.json",
        });
    }

	
	@Parameter public String message;
	
	@Test public void loadTest() {
		Context context=Context.createRootContext();
		try(InputStream in=DeferredCodingTest.class.getResourceAsStream(message)) {
			DialerMessage msg=JsonCodingScheme.REST_SCHEME.createDecoder(context, in).decode(DialerMessage.class);
			System.out.println(msg);
			try(ByteEncoder encoder=JsonCodingScheme.REST_SCHEME.newBuilder().setIndent("  ","\n").build().createEncoder(context)) {
				System.out.write(encoder.encode(msg).asBytes());
			}
			if("deferred1.json".equals(message)) {
				msg.getMessage(Payload.class);
			} else {
				msg.getDeferred();
			}
			try(ByteEncoder encoder=JsonCodingScheme.REST_SCHEME.newBuilder().setIndent("  ","\n").build().createEncoder(context)) {
				System.out.write(encoder.encode(msg).asBytes());
			}
			//System.out.println(msg.getDeferred());
		} catch(Throwable t) {
			t.printStackTrace();
			fail();
		}
	}

	@Test public void loadTest2() {
		Context context=Context.createRootContext();
		try(InputStream in=DeferredCodingTest.class.getResourceAsStream(message)) {
			DeferredObject msg=JsonCodingScheme.REST_SCHEME.createDecoder(context, in).decode(DeferredObject.class);
			System.out.println(msg);
			try(ByteEncoder encoder=YamlCodingScheme.REST_SCHEME.createEncoder(context)) {
				System.out.write(encoder.encode(msg).asBytes());
			}
		} catch(Throwable t) {
			t.printStackTrace();
			fail();
		}
	}


	public static class DialerMessage {
		@JsonProperty Map<String,String> headers;
		@JsonProperty DeferredObject payload;
		@JsonProperty List<DeferredObject> payloads;
		@JsonProperty Type type;
		@JsonProperty String uri;
		@JsonProperty String message_ident;
		@JsonProperty String key;
		
		private byte[] unzip(DeferredObject object,boolean wrapped) {
			try {
				String content=null;
				if(wrapped) {
					content=object.get(Wrapper.class).compressed; 
				} else {
					content=object.get(String.class);
				}
				
				try(InputStream in=new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(content)))) {
					ByteArrayOutputStream out=new ByteArrayOutputStream();
					byte[] buffer=new byte[1024];
					int n;
					while((n=in.read(buffer))>=0) {
						out.write(buffer,0,n);
					}
					return out.toByteArray();
				}
			} catch(Throwable t) {
				t.printStackTrace();
			}
			return new byte[0];
		}
		
		public <T> T getMessage(Class<T> clazz) throws BaseException {
			if("application/json".equals(headers.get("Content-Type"))) {
				return payloads.get(0).get(clazz);
			}
			return null;
		}
		
		public InputStream payload() {
			return new InputStream() {
				int offset;
				byte[] current;
				int index;
				byte[] buf=new byte[1];
				public int read(byte[] buffer,int o,int len) {
					if(current==null||offset==current.length) {
						if(index>=payloads.size()) {
							return -1;
						}
						current=unzip(payloads.get(index),false);
						offset=0;
						index++;
					}
					int n=Math.min(current.length-offset, len);
					System.arraycopy(current, offset, buffer, o, n);
					offset+=n;
					return n;
				}
				@Override
				public int read() throws IOException {
					if(read(buf)<0) {
						return -1;
					} else {
						return buf[0]&0xff;
					}
				}
			};
		}
		
		public Object getDeferred() {
			switch(type) {
				case request: if("gzip".equals(headers.get("Transfer-Encoding"))) {
					byte[] content=unzip(payload,true);
					//System.out.println(new String(content));
				}
				break;
				case message: if("gzip".equals(headers.get("Transfer-Encoding"))) {
					byte[] content=unzip(payloads.get(0),false);
					//System.out.println(new String(content));
				}
				break;
				default: return null;
			}
			return null;
		}
		
		public enum Type {
			request, pong, message;
		}
		
	}
	
	public static class Payload {
		@JsonProperty DeviceBroadcastStatus deviceBroadcastStatus;
	}
	
	public static class DeviceBroadcastStatus {
		@JsonProperty String timestamp;
		@JsonProperty String broadcast_status;
		@JsonProperty String device_id;
	}
	
	public static class Wrapper {
		@JsonProperty String compressed;
	}	
}
