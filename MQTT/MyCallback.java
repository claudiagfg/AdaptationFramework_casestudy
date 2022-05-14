package mqtt_connectivity.test1;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;

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
		System.out.println("message received");
		System.out.println(topic + ":" + message + " Retained flag is: " + message.isRetained());
		System.out.println("Payload is: "+ message.getPayload().toString());
		System.out.println(message.toString());
		
		/*Extract fields the provider might be interested in knowing, such as the name, id and 
		 * bundleurl of the new device (given we know the standard of mqtt discovery messages) */
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

	@Override
	public void deliveryComplete(IMqttDeliveryToken token){
	}	
	
}
