package mqtt_connectivity.test1;

import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class App 
{
	public static MqttAsyncClient myClient;
	//public static String clientID = UUID.randomUUID().toString();
	//public static MemoryPersistence persistence = new MemoryPersistence();  	   
	
    public static void main( String[] args ) throws MqttException
    { 	
    	String clientID = UUID.randomUUID().toString();
    	MemoryPersistence persistence = new MemoryPersistence(); 
    	myClient = new MqttAsyncClient("tcp://localhost:1883", clientID, persistence);
    	MyCallback myCallback = new MyCallback();
        myClient.setCallback(myCallback);
        IMqttToken token = myClient.connect();
        token.waitForCompletion();
        
        myClient.subscribe("tfghome/discovery", 0);
    }
    
}
