package com.undebugged.heraldry.client;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.network.Network;
import com.jme3.network.NetworkClient;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;
import com.undebugged.heraldry.controls.DefaultHUDControl;
import com.undebugged.heraldry.controls.UserInputControl;
import com.undebugged.heraldry.core.Heraldry;
import com.undebugged.heraldry.core.PlayerData;
import com.undebugged.heraldry.messages.ActionMessage;
import com.undebugged.heraldry.messages.AutoControlMessage;
import com.undebugged.heraldry.messages.ChatMessage;
import com.undebugged.heraldry.messages.ManualControlMessage;
import com.undebugged.heraldry.messages.ServerAddEntityMessage;
import com.undebugged.heraldry.messages.ServerAddPlayerMessage;
import com.undebugged.heraldry.messages.ServerDisableEntityMessage;
import com.undebugged.heraldry.messages.ServerEffectMessage;
import com.undebugged.heraldry.messages.ServerEnableEntityMessage;
import com.undebugged.heraldry.messages.ServerEnterEntityMessage;
import com.undebugged.heraldry.messages.ServerEntityDataMessage;
import com.undebugged.heraldry.messages.ServerRemoveEntityMessage;
import com.undebugged.heraldry.messages.ServerRemovePlayerMessage;
import com.undebugged.heraldry.messages.StartGameMessage;
import com.undebugged.heraldry.messages.SyncCharacterMessage;
import com.undebugged.heraldry.messages.SyncRigidBodyMessage;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.controls.dynamic.TextCreator;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

/**
 * The client Main class, also the screen controller for most parts of the
 * login and lobby GUI
 * @author normenhansen
 */
public class HeraldryClient extends SimpleApplication implements ScreenController {

    private static HeraldryClient app;

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setFrameRate(Heraldry.SCENE_FPS);
        settings.setSettingsDialogImage("/Interface/Images/cop4.png");
        settings.setTitle("Heraldry");
        Heraldry.registerSerializers();
        Heraldry.setLogLevels(true);
        app = new HeraldryClient();
        app.setSettings(settings);
        app.setPauseOnLostFocus(false);
        app.start();
    }
    private ClientWorldManager worldManager;
    private ClientPhysicsSyncManager syncManager;
    private ClientEffectsManager effectsManager;
    private Nifty nifty;
    private NiftyJmeDisplay niftyDisplay;
    private TextRenderer statusText;
    private NetworkClient client;
    private ClientNetListener listenerManager;
    private BulletAppState bulletState;
//    private ChaseCamera chaseCam;

    @Override
    public void simpleInitApp() {
        startNifty();
        client = Network.createClient(Heraldry.VERSION, Heraldry.PROTOCOL_VERSION);
        bulletState = new BulletAppState();
        if (Heraldry.PHYSICS_THREADED) {
            bulletState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        }
        getStateManager().attach(bulletState);
        bulletState.getPhysicsSpace().setAccuracy(Heraldry.PHYSICS_FPS);
        if(Heraldry.PHYSICS_DEBUG){
            bulletState.getPhysicsSpace().enableDebug(assetManager);
        }
        inputManager.setCursorVisible(false);
        flyCam.setEnabled(false);
//        chaseCam = new ChaseCamera(cam, inputManager);
//        chaseCam.setSmoothMotion(true);
//        chaseCam.setChasingSensitivity(100);
//        chaseCam.setTrailingEnabled(true);

        syncManager = new ClientPhysicsSyncManager(app, client);
        syncManager.setMaxDelay(Heraldry.NETWORK_MAX_PHYSICS_DELAY);
        syncManager.setMessageTypes(AutoControlMessage.class,
                ManualControlMessage.class,
                ActionMessage.class,
                SyncCharacterMessage.class,
                SyncRigidBodyMessage.class,
                ServerEntityDataMessage.class,
                ServerEnterEntityMessage.class,
                ServerAddEntityMessage.class,
                ServerAddPlayerMessage.class,
                ServerEffectMessage.class,
                ServerEnableEntityMessage.class,
                ServerDisableEntityMessage.class,
                ServerRemoveEntityMessage.class,
                ServerRemovePlayerMessage.class);
        stateManager.attach(syncManager);

        //world manager, manages entites and server commands
        //commandControl = new UserCommandControl(nifty.getScreen("default_hud"), inputManager);
        //world manager, manages entites and server commands
        worldManager = new ClientWorldManager(this, rootNode);
        worldManager.addUserControl(new UserInputControl(inputManager, cam));
        worldManager.addUserControl(new DefaultHUDControl(nifty.getScreen("default_hud")));
        //register effects manager with sync manager so that messages can apply their data
        worldManager.getSyncManager().addObject(-2, effectsManager);
        syncManager.addObject(-1, worldManager);
        stateManager.attach(worldManager);

        //effects manager for playing effects
        effectsManager = new ClientEffectsManager();
        stateManager.attach(effectsManager);
        
        syncManager.addObject(-2, effectsManager);
        listenerManager = new ClientNetListener(this, client, worldManager, effectsManager);
    }

    /**
     * starts the nifty gui system
     */
    private void startNifty() {
        guiNode.detachAllChildren();
        guiNode.attachChild(fpsText);
        niftyDisplay = new NiftyJmeDisplay(assetManager,
                inputManager,
                audioRenderer,
                guiViewPort);
        nifty = niftyDisplay.getNifty();
        try {
            nifty.fromXml("Interface/ClientUI.xml", "load_game", this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        statusText = nifty.getScreen("load_game").findElementByName("status_text")
        		.getRenderer(TextRenderer.class);
        guiViewPort.addProcessor(niftyDisplay);
    }

    /**
     * sets the status text of the main login view, threadsafe
     * @param text
     */
    public void setStatusText(final String text) {
        enqueue(new Callable<Void>() {

            public Void call() throws Exception {
                statusText.setText(text);
                return null;
            }
        });
    }

    /**
     * updates the list of players in the lobby gui, threadsafe
     */
    public void updatePlayerData() {
        Logger.getLogger(HeraldryClient.class.getName()).log(Level.INFO, "Updating player data");
        enqueue(new Callable<Void>() {

            public Void call() throws Exception {
                Screen screen = nifty.getScreen("lobby");
                Element panel = screen.findElementByName("layer").findElementByName("panel")
                		.findElementByName("players_panel").findElementByName("players_list")
                		.findElementByName("panel");
                List<PlayerData> players = PlayerData.getHumanPlayers();
                List<Element> elements = new LinkedList<Element>(panel.getElements());
                for (Iterator<Element> it = elements.iterator(); it.hasNext();) {
                    Element element = it.next();
                    element.markForRemoval();//disable();
                }
                TextCreator labelCreator = new TextCreator("unknown player");
                labelCreator.setStyle("my-listbox-item-style");
                for (Iterator<PlayerData> it = players.iterator(); it.hasNext();) {
                    PlayerData data = it.next();
                    Logger.getLogger(HeraldryClient.class.getName()).log(Level.INFO, "List player {0}", data);
                    labelCreator.setText(data.getStringData("name"));
                    labelCreator.create(nifty, screen, panel);
                }
                return null;
            }
        });
    }

    /**
     * add text to chat window, threadsafe
     * @param text
     */
    public void addChat(final String text) {
        enqueue(new Callable<Void>() {

            public Void call() throws Exception {
                Screen screen = nifty.getScreen("lobby");
                Element panel = screen.findElementByName("chat_list_panel");
                TextCreator labelCreator = new TextCreator(text);
                labelCreator.setStyle("my-listbox-item-style");
                labelCreator.create(nifty, screen, panel);
                return null;
            }
        });
    }

    /**
     * gets the text currently entered in the textbox and sends it as a chat message
     */
    public void sendChat() {
        Logger.getLogger(HeraldryClient.class.getName()).log(Level.INFO, "Send chat message");
        enqueue(new Callable<Void>() {

            public Void call() throws Exception {
                Screen screen = nifty.getScreen("lobby");
                TextField control = screen.findNiftyControl("chat_text",TextField.class);
                String text = control.getText();
                sendMessage(text);
                control.setText("");
                return null;
            }
        });
    }

    //FIXME: nifty cannot find sendChat() when sendChat(String text) is existing too
    public void sendMessage(String text) {
    	client.send(new ChatMessage(text));
    }

    /**
     * connect to server (called from gui)
     */
    public void connect() {
        //TODO: not connect when already trying..
        final String userName = nifty.getCurrentScreen()
        		.findNiftyControl("username_text",TextField.class).getText();
        if (userName.trim().length() == 0) {
            setStatusText("Username invalid");
            return;
        }
        final String passWord = nifty.getCurrentScreen()
        		.findNiftyControl("password_text",TextField.class).getText();
        if (passWord.trim().length() == 0) {
            setStatusText("Password invalid");
            return;
        }
        listenerManager.setName(userName);
        listenerManager.setPass(passWord);
        setStatusText("Connecting...");
        try {
            client.connectToServer(Heraldry.DEFAULT_SERVER, 
            		Heraldry.DEFAULT_PORT_TCP, Heraldry.DEFAULT_PORT_UDP);
            client.start();
        } catch (IOException ex) {
            setStatusText(ex.getMessage());
            Logger.getLogger(HeraldryClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * brings up the lobby display
     */
    public void lobby() {
//        chaseCam.setDragToRotate(false);
        inputManager.setCursorVisible(true);
        nifty.gotoScreen("lobby");
    }

    /**
     * send message to start selected game
     */
    public void startGame() {
        //TODO: map selection
           client.send(new StartGameMessage("Scenes/MonkeyZone.j3o"));
    }

    /**
     * loads a level, basically does everything on a seprate thread except
     * updating the UI and attaching the level
     * @param name
     * @param modelNames
     */
    public void loadLevel(final String name, final String[] modelNames) {
        final TextRenderer statusText = nifty.getScreen("load_level")
        		.findElementByName("status_text").getRenderer(TextRenderer.class);
        if (name.equals("null")) {
            enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    worldManager.closeLevel();
                    lobby();
                    return null;
                }
            });
            return;
        }
        new Thread(new Runnable() {

            public void run() {
                try {
                    enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            nifty.gotoScreen("load_level");
                            statusText.setText("Loading Terrain..");
                            return null;
                        }
                    }).get();
                    worldManager.loadLevel(name);
                    enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            statusText.setText("Creating NavMesh..");
                            return null;
                        }
                    }).get();
                    //worldManager.createNavMesh();
                    enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            statusText.setText("Loading Models..");
                            return null;
                        }
                    }).get();
                    worldManager.preloadModels(modelNames);
                    enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            worldManager.attachLevel();
                            statusText.setText("Done Loading!");
                            nifty.gotoScreen("default_hud");
                            inputManager.setCursorVisible(false);
//                            chaseCam.setDragToRotate(false);
                            return null;
                        }
                    }).get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void bind(Nifty nifty, Screen screen) {
    }

    public void onStartScreen() {
    }

    public void onEndScreen() {
    }

    @Override
    public void simpleUpdate(float tpf) {
    }

    @Override
    public void simpleRender(RenderManager rm) {
    }

    public void quit() {
        app.stop(); 
      }
    
    
    @Override
    public void destroy() {
        try {
            client.close();
        } catch (Exception ex) {
            Logger.getLogger(HeraldryClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        super.destroy();
    }
}
