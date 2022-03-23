package mqtt_connectivity.test1;
/*import java.io.FileOutputStream;
import java.util.Properties;*/
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
public class createFile {
   public static void main(String args[]) {
	  	   /*String currentPath = System.getProperty("user.dir");
	  String bundlePath = currentPath + "/src/main/java/emptyBundle";
	  File directorio = new File(bundlePath);
      if (!directorio.exists()) {
          if (directorio.mkdirs()) {
              System.out.println("directory created");
          } else {
              System.out.println("an error was made while creating the directory");
          }
      }
      Properties props = new Properties();
      props.put("examplename", "exampleprop");
      System.out.println(bundlePath);
      FileOutputStream outputStream = new FileOutputStream(bundlePath + "/pom.xml");
      props.storeToXML(outputStream, "Example bundle pom file");
      System.out.println("File was created......");
      */
   }
   public static void creation() throws IOException, InterruptedException {
	   String currentDir = System.getProperty("user.dir");
	   String subDir = currentDir.substring(0 , currentDir.lastIndexOf("/") + 1); 
	   File dir =  new File(subDir);
	   String command = "mvn archetype:generate -DgroupId=test2 -DartifactId=bundleTest2 -DarchetypeGroupId=org.apache.maven.archetypes -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.1 -Dversion=1.0 -DinteractiveMode=false";
	   //groupId e artifactId cambiar por datos payload?
	   Process proc = Runtime.getRuntime().exec(command, null, dir);
	   BufferedReader reader =  new BufferedReader(new InputStreamReader(proc.getInputStream()));
	   String line = "";
	   while((line = reader.readLine()) != null) {
		   System.out.print(line + "\n");
	   }
	   proc.waitFor();
   }
}