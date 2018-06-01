package test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProofFakeStream {

	public static final Logger log = LoggerFactory.getLogger(test.ProofFakeStream.class);
	
	public static void main(String[] args) throws ClientProtocolException, IOException {
		String postUrl = "http://localhost:8080/objects/upload";
		HttpPost httpPost = new HttpPost(postUrl);
		Path filePath = Paths.get("/tmp/input/lena.png");
		HttpEntity entity = MultipartEntityBuilder.create()
				.addBinaryBody("file", filePath.toFile(), ContentType.create("image/png"), filePath.getFileName().toString())
				.build();
		httpPost.setEntity(entity);
		HttpClientBuilder builder = HttpClientBuilder.create();
		builder.setConnectionManager(new BasicHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new PlainConnectionSocketFactory() {
					
					@Override
					public Socket createSocket(HttpContext context) throws IOException {
						return super.createSocket(context);
					}
					
					@Override
					public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host, InetSocketAddress remoteAddress,
							InetSocketAddress localAddress, HttpContext context) throws IOException {
						return super.connectSocket(connectTimeout, sock, host, remoteAddress, localAddress, context);
					}
				})
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build(), null, null, null));
		HttpResponse response = builder.build().execute(httpPost);
		
		// TODO check result code properly

		String json = EntityUtils.toString(response.getEntity());
		String obj = new org.json.JSONObject(json).getString("id");
		log.info("obj: " + obj);
	}
}
