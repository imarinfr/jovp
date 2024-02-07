/*
 * This forst pass was written to work with the OPI3 software. It needs some work!
 */
package es.optocom.jovp;

//import org.lwjgl.glfw.GLFWVidMode;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class MonitorDeserializer implements JsonDeserializer<Monitor> {
    
     public Monitor deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        long monitor = jsonObject.get("monitor").getAsInt(); 
        //String name = jsonObject.get("name").getAsString();
        int width = jsonObject.get("width").getAsInt();
        int height = jsonObject.get("height").getAsInt();
        int refreshRate  = jsonObject.get("refreshRate").getAsInt();
        int widthMM = jsonObject.get("widthMM").getAsInt();
        int heightMM = jsonObject.get("heightMM").getAsInt();
        //float aspect = jsonObject.get("aspect").getAsFloat();
        //float pixelWidth = jsonObject.get("pixelWidth").getAsFloat();
        //float pixelHeight = jsonObject.get("pixelHeight").getAsFloat();
        //float pixelAspect = jsonObject.get("pixelAspect").getAsFloat();
        //int[] colorDepth = context.deserialize(jsonObject.get("colorDepth"), int[].class);
        //final GLFWVidMode videoMode = null;
        //GLFWVidMode.Buffer videoModes = null;

            // This is not faithful to deserializing, but it's a start (Turpin Feb 2024)
        Monitor m = new Monitor(monitor);
        m.setSize(width, height);
        m.setPhysicalSize(widthMM, heightMM);
        m.setRefreshRate(refreshRate);

        return m;
    }

}
