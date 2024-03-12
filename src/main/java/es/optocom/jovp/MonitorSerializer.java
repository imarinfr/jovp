package es.optocom.jovp;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class MonitorSerializer implements JsonSerializer<Monitor> {
    public MonitorSerializer() {
        super();
    }

    @Override
    public JsonElement serialize(final Monitor monitor, final Type type, final JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();

        jsonObject.add("monitor", context.serialize(monitor.getHandle()));
        jsonObject.add("name", context.serialize(monitor.getName()));
        jsonObject.add("width", context.serialize(monitor.getWidth()));
        jsonObject.add("height", context.serialize(monitor.getHeight()));
        jsonObject.add("refreshRate", context.serialize(monitor.getRefreshRate()));
        jsonObject.add("widthMM", context.serialize(monitor.getWidthMM()));
        jsonObject.add("heightMM", context.serialize(monitor.getHeightMM()));
        jsonObject.add("aspect", context.serialize(monitor.getAspect()));
        jsonObject.add("pixelWidth", context.serialize(monitor.getPixelWidth()));
        jsonObject.add("pixelHeight", context.serialize(monitor.getPixelHeight()));
        jsonObject.add("pixelAspect", context.serialize(monitor.getPixelAspect()));
        jsonObject.add("colorDepth", context.serialize(monitor.getColorDepth()));
        
            // TODO: these look too hard for now (Feb 2024 Turpin)
        //jsonObject.add("videoMode", context.serialize(monitor.getVideoMode()));
        //jsonObject.add("videoModes", context.serialize(monitor.getVideoModes()));

        return jsonObject;
    }
    
}
