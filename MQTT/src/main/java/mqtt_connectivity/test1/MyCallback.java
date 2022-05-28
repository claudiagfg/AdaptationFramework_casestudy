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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.sql.SQLException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MyCallback implements MqttCallback{
	
	private String url = "jdbc:postgresql://localhost:5432/postgres";
    private String user = "tfg";
    private String password = "";
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
		//INSERT NEW DEVICE INTO DATABASE
		insertInitialInfo(message);
		//THE PROVIDER IS NOTIFIED THAT A NEW DEVICE HAS BEEN REGISTERED
		providerNotifier(message);		
		/*
		 *  ONCE THE PROVIDER HAS BEEN NOTIFIED, IT SENDS BACK THE INFORMATION ABOUT THE CATEGORIES 
		 *  AND SERVICES RELATED TO THE DEVICE (PLUS ITS INFORMATION IN ORDER TO IDENTIFY IT). 
		 *  THE HOUSE RECEIVES THIS INFO, UPDATES THE SERVICES/CATEGORIES TABLES IF THE ONES SENT BY 
		 *  THE PROVIDER WEREN'T ALREADY IN ITS DATABASE, AND THEN SENDS BACK AN UPDATED IMAGE 
		 *  (OR DIGITAL TWIN) TO THE PROVIDER
		 */
		digitalTwinMaker();
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token){
	}	
	
	private void insertInitialInfo(MqttMessage message) throws IOException, InterruptedException, JSONException {
		
		String jsonString = message.toString();
		JSONObject obj = new JSONObject(jsonString);
		String name = obj.getString("name");
		String devtype = obj.getString("main_feature");
		String manufacturer = obj.getString("manufacturer");
		String bundleurl = obj.getString("configuration_url"); 
		String data = "";
		
		try(Connection con = DriverManager.getConnection(url, user, password);
				Statement stt = con.createStatement();
		      ) {		      
		         System.out.println("Inserting new device into database...");          
		         String sql2 = "select count(*) from devices;";
		         ResultSet rs = stt.executeQuery(sql2);
		         while (rs.next()) {
		        	 System.out.println("Number of devices before: " + rs.getString(1));
		         }
		         String sql = "insert into devices(home_id, name, "
		    				+ "bundle_url, manufacturer, device_type) values(1,'"+ name+ "','" + bundleurl + "','" 
		    				+ manufacturer + "','" + devtype + "');";
		         stt.executeUpdate(sql);
		         System.out.println("Successfully inserted device");
		         ResultSet rs2 = stt.executeQuery(sql2);
		         while (rs2.next()) {
		        	 System.out.println("Number of devices after: " + rs2.getString(1));
		         }
		         con.close();
		         System.out.println("jdbc connection closed");
		      } catch (SQLException e) {
		         e.printStackTrace();
		      } 
	}
	
	private void providerNotifier(MqttMessage message) throws IOException, InterruptedException, JSONException {
		//Extract fields the provider might be interested in knowing, such as the name, id and 
		//bundleurl of the new device (given we know the standard of mqtt discovery messages)
		
		//Añadir ip como variable
		String jsonString = message.toString();
		JSONObject obj = new JSONObject(jsonString);
		String name = obj.getString("name");
		int id = obj.getInt("id");
		String manufacturer = obj.getString("manufacturer");
		String model = obj.getString("model");
		String sw_version = obj.getString("sw_version");
		String main_feature = obj.getString("main_feature");
		String data = "{\"name\":\"" + name + "\",\"id\":" + id + ", \"main_feature\":\"" 
				+ main_feature + "\"," + "\"manufacturer\":\"" + manufacturer + "\", "
				+ "\"sw_version\":\"" + sw_version + "\", \"model\":\"" + model +"\"}";
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
        System.out.println("RESPONSE IS: " + response.body());
        updateDB(response.body());
        
	}	
	private void updateDB(String response) throws JSONException{
		List<Integer> listsrv = new ArrayList<Integer>();
		List<Integer> listcat = new ArrayList<Integer>();
		Integer device_id = 0;
		String jsonString = response.toString();
		JSONObject obj = new JSONObject(jsonString);
		String name = obj.getString("name");
		String manufacturer = obj.getString("manufacturer");
		JSONArray ArrayServices = obj.getJSONArray("services");
		JSONArray ArrayCategories= obj.getJSONArray("categories");
		try(Connection con = DriverManager.getConnection(url, user, password);
				Statement stt = con.createStatement();) 
		{
				 String sql2 = "select id from services;";
				 ResultSet rs2 = stt.executeQuery(sql2);
				 while (rs2.next()) {
					 listsrv.add(Integer.parseInt(rs2.getString(1))); 
				 }
				 String sql3 = "select id from categories;";
				 ResultSet rs3 = stt.executeQuery(sql3);
				 while (rs3.next()) {
					 listcat.add(Integer.parseInt(rs3.getString(1))); 
				 }
				 String sql = "select id from devices where name='" + name + "' and manufacturer= '" + manufacturer + "';";
		         ResultSet rs = stt.executeQuery(sql);
		         while (rs.next()) {
		        	 if (rs.getString(1) != null) {
		        		 device_id = Integer.parseInt(rs.getString(1));
		        		 System.out.println("Device id is:" + device_id);
		        	 }else {
		        		 System.out.println("Device not found. Can not update categories and services");
		        	 }
		         }
		         for(int i = 0; i < ArrayServices.length(); i++){
		 		       JSONObject srv = new JSONObject(ArrayServices.get(i).toString());
		 			   if (!listsrv.contains(srv.getInt("id"))){
		 				  String ins = "insert into services(id, name, bundle_url) values(" + srv.getInt("id") + ",'" + srv.getString("name") + "','" + srv.getString("bundleservice_url") + "');";
		 				  stt.addBatch(ins);
		 				  stt.executeBatch();
		 				  stt.clearBatch();
		 			   }
		 			  String ins2 = "insert into device_service(device_id, service_id) values(" + device_id + "," + srv.getInt("id") + ");";
	 				  stt.addBatch(ins2);
	 				  stt.executeBatch();
	 				  stt.clearBatch();
		 		 }		
		         for(int i = 0; i < ArrayCategories.length(); i++){
		 		       JSONObject srv = new JSONObject(ArrayCategories.get(i).toString());
		 			   if (!listcat.contains(srv.getInt("id"))){
		 				  String ins = "insert into categories(id, name) values(" + srv.getInt("id") + ",'" + srv.getString("name") + "');";
		 				  stt.addBatch(ins);
		 				  stt.executeBatch();
		 				  stt.clearBatch();
		 			   }
		 			  String ins2 = "insert into device_category(device_id, category_id) values(" + device_id + "," + srv.getInt("id") + ");";
	 				  stt.addBatch(ins2);
	 				  stt.executeBatch();
	 				  stt.clearBatch();
		 		 }	
		         con.close();
		         System.out.println("jdbc connection closed");
		      } catch (SQLException e) {
		         e.printStackTrace();
		      } 
		
	}
	private void digitalTwinMaker() throws IOException, InterruptedException{
		
		String data = "";
		String data2 = "";
        String dataToSend = "";
        try (Connection con = DriverManager.getConnection(url, user, password);
        		PreparedStatement st = con.prepareStatement("select home_id from devices limit 1;");
	                ResultSet rs = st.executeQuery()) {
			        	while (rs.next()) {
			        		data2 = rs.getString(1);
			        	}
			        con.close();
	        } catch (SQLException ex) {
	            System.out.println(ex);
	        }
        try (Connection con = DriverManager.getConnection(url, user, password);
        		PreparedStatement st = con.prepareStatement("select json_agg(dev) as device\n"
        				+ "from(\n"
        				+ "  select d.id, d.name, d.area_id, d.device_type, d.bundle_url, d.is_active,\n"
        				+ "  (select json_agg(ctg)\n"
        				+ "    from (\n"
        				+ "      select distinct categories.name from categories \n"
        				+ "      JOIN device_category on categories.id= device_category.category_id\n"
        				+ "      join devices on d.id = device_category.device_id\n"
        				+ "    ) ctg\n"
        				+ "  ) as categories,\n"
        				+ "  (select json_agg(srv)\n"
        				+ "    from (\n"
        				+ "      select distinct services.name from services\n"
        				+ "    JOIN device_service on services.id= device_service.service_id\n"
        				+ "    join devices on d.id = device_service.device_id\n"
        				+ "    ) srv\n"
        				+ "  ) as services\n"
        				+ "from devices as d) dev;");
	                ResultSet rs = st.executeQuery()) {
			        	while (rs.next()) {
			        		data = rs.getString(1);
			        	    dataToSend = "{ \"home_id\":" + data2 + ", \"devices \":" + data + "}";
			        	    System.out.println("Data to send: " + dataToSend);
			        	}
		        	HttpRequest request = HttpRequest.newBuilder()
	  		                .POST(BodyPublishers.ofString(dataToSend))
	  		                .uri(URI.create("http://54.93.210.112/api/set_status/tfghome_digitaltwin"))
	  		                .header("Content-Type", "application/json")
	  		                .build();
	  			
	  		        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	  		        //Print status code
	  		        System.out.println("\n" + response.statusCode());
	  		        con.close();
	        } catch (SQLException ex) {
	            System.out.println(ex);
	        }
        
	}
	
}
