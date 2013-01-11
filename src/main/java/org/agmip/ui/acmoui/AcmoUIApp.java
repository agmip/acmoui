package org.agmip.ui.acmoui;

import org.apache.pivot.collections.Map;
import org.apache.pivot.beans.BXMLSerializer;
import org.apache.pivot.wtk.Application;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.Display;

/**
 * Hello world!
 *
 */
public class AcmoUIApp extends Application.Adapter {

    private AcmoUIWindow window = null;

    @Override
    public void startup(Display display, Map<String, String> props) throws Exception {
        BXMLSerializer bxml = new BXMLSerializer();
        window = (AcmoUIWindow) bxml.readObject(getClass().getResource("/acmoui.bxml"));
        window.open(display);
    }

    @Override
    public boolean shutdown(boolean opt) {
        if (window != null) {
            window.close();
        }
        return false;
    }

    public static void main(String[] args) {
        DesktopApplicationContext.main(AcmoUIApp.class, args);
    }
}
