package com.undebugged.heraldry.server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.undebugged.heraldry.core.Heraldry;

public class HeraldryServer extends SimpleApplication {

	private static Server server;
    private static HeraldryServer app;
	
    private ServerWorldManager worldManager;
    private ServerGameManager gameManager;
    private ServerPhysicsSyncManager syncManager;
    private ServerNetListener listenerManager;
    private BulletAppState bulletState;
    
    
	public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setFrameRate(Heraldry.SCENE_FPS);
        settings.setRenderer(null);
        settings.setAudioRenderer(null);
		app = new HeraldryServer();
		app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.setSettings(settings);
	    app.start(JmeContext.Type.Headless);
	}

	@Override
	public void simpleInitApp() {
		try {
			server = Network.createServer(
					Heraldry.VERSION, Network.DEFAULT_VERSION, 
					Heraldry.DEFAULT_PORT_TCP, Heraldry.DEFAULT_PORT_UDP);
			server.start();
		} catch (IOException e) {
			Logger.getLogger(HeraldryServer.class.getName()).log(Level.SEVERE, "Cannot start server: {0}", e);
            return;
		}
        bulletState = new BulletAppState();
        getStateManager().attach(bulletState);
        bulletState.getPhysicsSpace().setAccuracy(Heraldry.PHYSICS_FPS);
        syncManager = new ServerPhysicsSyncManager(app, server);
        syncManager.setSyncFrequency(Heraldry.NETWORK_SYNC_FREQUENCY);
        syncManager.setMessageTypes();
        stateManager.attach(syncManager);
        
        worldManager = new ServerWorldManager(this, rootNode);
        stateManager.attach(worldManager);
        //register world manager with sync manager so that messages can apply their data
        syncManager.addObject(-1, worldManager);
        //create server side game manager
        gameManager = new ServerGameManager();
        stateManager.attach(gameManager);
        listenerManager = new ServerNetListener(this, server, worldManager, gameManager);
	}
	
    @Override
    public void simpleUpdate(float tpf) {
//        worldManager.update(tpf);
    }

    @Override
    public void simpleRender(RenderManager rm) {
    }

    @Override
    public void destroy() {
        super.destroy();
        server.close();
    }
}
