package org.heraldry.core;

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

}
