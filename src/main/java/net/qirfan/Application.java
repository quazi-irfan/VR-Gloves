package net.qirfan;

import com.jme3.app.SimpleApplication;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;


public class Application extends SimpleApplication {

    public static void main(String[] args) {
        Application app = new Application();
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1024, 768);
        app.setSettings(settings);
        app.showSettings = false;
        app.start();
    }

    public void simpleInitApp() {
        flyCam.setMoveSpeed(15);

        setDisplayFps(false);
        setDisplayStatView(false);

        cam.setLocation(new Vector3f(-8.402544f, 5.645521f, 6.9485188f));
        cam.setRotation(new Quaternion(0.06418777f, 0.8771454f, -0.12242156f, 0.45990095f));

        GameState gameState = new GameState();
        stateManager.attach(new GameState());
        stateManager.attach(new GuiState(gameState));
    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
    }

}
