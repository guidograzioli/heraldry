package com.undebugged.heraldry.core;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.network.message.DisconnectMessage;
import com.jme3.network.serializing.Serializer;
import com.undebugged.heraldry.messages.ActionMessage;
import com.undebugged.heraldry.messages.AutoControlMessage;
import com.undebugged.heraldry.messages.ChatMessage;
import com.undebugged.heraldry.messages.ClientJoinMessage;
import com.undebugged.heraldry.messages.HandshakeMessage;
import com.undebugged.heraldry.messages.ManualControlMessage;
import com.undebugged.heraldry.messages.ServerAddEntityMessage;
import com.undebugged.heraldry.messages.ServerAddPlayerMessage;
import com.undebugged.heraldry.messages.ServerDisableEntityMessage;
import com.undebugged.heraldry.messages.ServerEffectMessage;
import com.undebugged.heraldry.messages.ServerEnableEntityMessage;
import com.undebugged.heraldry.messages.ServerEnterEntityMessage;
import com.undebugged.heraldry.messages.ServerEntityDataMessage;
import com.undebugged.heraldry.messages.ServerJoinMessage;
import com.undebugged.heraldry.messages.ServerPlayerDataMessage;
import com.undebugged.heraldry.messages.ServerRemoveEntityMessage;
import com.undebugged.heraldry.messages.ServerRemovePlayerMessage;
import com.undebugged.heraldry.messages.StartGameMessage;
import com.undebugged.heraldry.messages.SyncCharacterMessage;
import com.undebugged.heraldry.messages.SyncRigidBodyMessage;

public class Heraldry {
	public static final String VERSION = "Heraldry v0.1";
	public static final String DEFAULT_SERVER = "127.0.0.1";
	public static final int DEFAULT_PORT_TCP = 6143;
	public static final int DEFAULT_PORT_UDP = 6143;
	public static final int PROTOCOL_VERSION = 1;
	public static final int CLIENT_VERSION = 1;
	public static final int SERVER_VERSION = 1;

	public static final float NETWORK_SYNC_FREQUENCY = 0.25f;
	public static final float NETWORK_MAX_PHYSICS_DELAY = 0.25f;
	public static final int SCENE_FPS = 60;
	public static final float PHYSICS_FPS = 1f / 30f;

	// only applies for client, server doesnt render anyway
	public static final boolean PHYSICS_THREADED = true;
	public static final boolean PHYSICS_DEBUG = false;


    public static void setLogLevels(boolean debug) {
        if (debug) {
            Logger.getLogger("de.lessvoid.nifty").setLevel(Level.SEVERE);
            Logger.getLogger("de.lessvoid.nifty.effects.EffectProcessor").setLevel(Level.SEVERE);
            Logger.getLogger("org.lwjgl").setLevel(Level.WARNING);
            Logger.getLogger("com.jme3").setLevel(Level.FINEST);
            Logger.getLogger("com.undebugged").setLevel(Level.FINEST);
        } else {
            Logger.getLogger("de.lessvoid").setLevel(Level.WARNING);
            Logger.getLogger("de.lessvoid.nifty.effects.EffectProcessor").setLevel(Level.WARNING);
            Logger.getLogger("org.lwjgl").setLevel(Level.WARNING);
            Logger.getLogger("com.jme3").setLevel(Level.WARNING);
            Logger.getLogger("com.undebugged").setLevel(Level.WARNING);
        }
    }

    public static void registerSerializers() {
        Serializer.registerClass(ActionMessage.class);
        Serializer.registerClass(DisconnectMessage.class);
        Serializer.registerClass(AutoControlMessage.class);
        Serializer.registerClass(ChatMessage.class);
        Serializer.registerClass(ClientJoinMessage.class);
        Serializer.registerClass(HandshakeMessage.class);
        Serializer.registerClass(ManualControlMessage.class);
        Serializer.registerClass(ServerAddEntityMessage.class);
        Serializer.registerClass(ServerAddPlayerMessage.class);
        Serializer.registerClass(SyncCharacterMessage.class);
        Serializer.registerClass(ServerEffectMessage.class);
        Serializer.registerClass(ServerEnableEntityMessage.class);
        Serializer.registerClass(ServerDisableEntityMessage.class);
        Serializer.registerClass(ServerEnterEntityMessage.class);
        Serializer.registerClass(ServerEntityDataMessage.class);
        Serializer.registerClass(ServerJoinMessage.class);
        Serializer.registerClass(SyncRigidBodyMessage.class);
        Serializer.registerClass(ServerPlayerDataMessage.class);
        Serializer.registerClass(ServerRemoveEntityMessage.class);
        Serializer.registerClass(ServerRemovePlayerMessage.class);
        Serializer.registerClass(StartGameMessage.class);
    }

}
