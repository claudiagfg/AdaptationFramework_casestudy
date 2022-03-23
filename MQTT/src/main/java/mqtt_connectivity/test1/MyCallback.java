package mqtt_connectivity.test1;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class MyCallback implements MqttCallback{

	public void connectionLost(Throwable cause) {
		System.out.println("There has been a mistake. Connection lost.");
	}

	public void messageArrived(String topic, MqttMessage message) throws Exception {
		System.out.println(topic + ":" + message + " Retained flag is: " + message.isRetained());
		System.out.println("Payload is: "+ message.getPayload().toString());
		//dependiendo del contenido ejecutar esto, o función de ?? ampliar servicio?? por ejemplo??
		createFile.creation();
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		//TO DO		
	}	
}
