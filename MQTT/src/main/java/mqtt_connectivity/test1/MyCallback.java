package mqtt_connectivity.test1;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;


public class MyCallback implements MqttCallback{
	
	private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
	
	public void connectionLost(Throwable cause) {
		System.out.println("There has been a mistake. Connection lost. Cause: " + cause);
		
	}

	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// A NEW DEVICE HAS ARRIVED
		System.out.println("message received");
		System.out.println(topic + ":" + message + " Retained flag is: " + message.isRetained());
		System.out.println("Payload is: "+ message.getPayload().toString());
		System.out.println(message.toString());
		//THE PROVIDER IS NOTIFIED THAT A NEW DEVICE HAS BEEN REGISTERED
		providerNotifier(message);
		/*
		 *  ONCE THE PROVIDER HAS BEEN NOTIFIED, IT DECIDES (THROUGH SOME KIND OF REASONING PROCESS), HOW TO 
		 * UPGRADE THE SMART HOME BY A) SENDING BACK THE URL OF THE BUNDLE OF THE NEW SERVICE THAT CAN BE
		 * STARTED THUS B) SOMEHOW UPDATING (or sending back the required information for the smart home to
		 * update) THE DATABASE BY INSERTING THE NEW DEVICE, AND FILLING AT LEAST THE FOLLOWING ATTRIBUTES:
		 * ID (sequential, automatic), SERVICE_ID, CATEGORY_ID AND BUNDLE_URL
		 * 
		 * NOW THAT THE DATABASE HAS BEEN UPDATED, A NEW DIGITAL TWIN IS BUILT AND SENT BACK TO THE PROVIDER
		 * 
		 */
		digitalTwinMaker();
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token){
	}	
	
	private void providerNotifier(MqttMessage message) throws IOException, InterruptedException {
		//Extract fields the provider might be interested in knowing, such as the name, id and 
		//bundleurl of the new device (given we know the standard of mqtt discovery messages)
		String jsonString = message.toString();
		JSONObject obj = new JSONObject(jsonString);
		String name = obj.getString("name");
		String id = obj.getString("id");
		String bundleurl = obj.getString("configuration_url");
		String data = "{\"name\":\"" + name + "\",\"id\":\"" + id + "\", \"bundle_url\":\"" + bundleurl + "\" }";
		System.out.println("content is: " + data);
		
		//Make a POST Request to the provider's server
		HttpRequest request = HttpRequest.newBuilder()
                .POST(BodyPublishers.ofString(data))
                .uri(URI.create("http://54.93.210.112/api/set_status/tfghome_newdevices"))
                .header("Content-Type", "application/json")
                .build();
	
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        //Print status code
        System.out.println(response.statusCode());
        //Print response body
        System.out.println(response.body());
	}
	
	private void digitalTwinMaker() throws IOException, InterruptedException {
		
		// Database must be started right?
		
		String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "tfg";
        String password = "";
        String data = "";
        
        try (Connection con = DriverManager.getConnection(url, user, password);
        		PreparedStatement st = con.prepareStatement("select row_to_json(dev) as devices\n"
        				+ "from(\n"
        				+ "select d.id, d.home_id, d.area_id, d.device_type_id, d.bundle_url, d.is_active,\n"
        				+ "(select json_agg(ctg)\n"
        				+ "from (\n"
        				+ "select * from categories where id = d.category_id\n"
        				+ ") ctg\n"
        				+ ") as category,\n"
        				+ "(select json_agg(srv)\n"
        				+ "from (\n"
        				+ "select * from services where id = d.service_id\n"
        				+ ") srv\n"
        				+ ") as service\n"
        				+ "from devices as d) dev;");
                ResultSet rs = st.executeQuery()) {
		        	while (rs.next()) {
		        	      System.out.print(rs.getString(1));
		        	      data = rs.getString(1);
		        	}
		        	HttpRequest request = HttpRequest.newBuilder()
	  		                .POST(BodyPublishers.ofString(data))
	  		                .uri(URI.create("http://54.93.210.112/api/set_status/tfghome_digitaltwin"))
	  		                .header("Content-Type", "application/json")
	  		                .build();
	  			
	  		        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	  		        //Print status code
	  		        System.out.println(response.statusCode());
	  		        //Print response body
	  		        System.out.println(response.body());
        } catch (SQLException ex) {
            System.out.println(ex);
        }
	}
	
}
