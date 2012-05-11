package com.undebugged.heraldry.server;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.Application;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageConnection;
import com.jme3.network.Server;
import com.jme3.scene.Spatial;
import com.undebugged.heraldry.messages.PhysicsSyncMessage;
import com.undebugged.heraldry.messages.SyncCharacterMessage;
import com.undebugged.heraldry.messages.SyncRigidBodyMessage;
import com.undebugged.heraldry.network.PhysicsSyncManager;
import com.undebugged.heraldry.network.SyncMessageValidator;

public class ServerPhysicsSyncManager extends PhysicsSyncManager {

	private Server server;
	Application app;

	public ServerPhysicsSyncManager(Application app, Server server) {
		this.app = app;
		this.server = server;
	}

	/**
	 * updates the PhysicsSyncManager, executes messages on client and sends
	 * sync info on the server.
	 * 
	 * @param tpf
	 */
	@Override
	public void update(float tpf) {
		time += tpf;
		if (time < 0) {
			// TODO: overflow
			time = 0;
		}
		syncTimer += tpf;
		if (syncTimer >= syncFrequency) {
			sendSyncData();
			syncTimer = 0;
		}
	}

	/**
	 * sends sync data for all active physics objects
	 */
	protected void sendSyncData() {
		for (Iterator<Entry<Long, Object>> it = syncObjects.entrySet().iterator(); it.hasNext();) {
			Entry<Long, Object> entry = it.next();
			if (entry.getValue() instanceof Spatial) {
				Spatial spat = (Spatial) entry.getValue();
				PhysicsRigidBody body = spat.getControl(RigidBodyControl.class);
				if (body == null) {
					body = spat.getControl(VehicleControl.class);
				}
				if (body != null && body.isActive()) {
					SyncRigidBodyMessage msg = new SyncRigidBodyMessage(entry.getKey(), body);
					broadcast(msg);
					continue;
				}
				CharacterControl control = spat.getControl(CharacterControl.class);
				if (control != null) {
					SyncCharacterMessage msg = new SyncCharacterMessage(entry.getKey(), control);
					broadcast(msg);
				}
			}
		}
	}

	/**
	 * use to broadcast physics control messages if server, applies timestamp to
	 * PhysicsSyncMessage, call from OpenGL thread!
	 * 
	 * @param msg
	 */
	public void broadcast(PhysicsSyncMessage msg) {
		if (server == null) {
			Logger.getLogger(ServerPhysicsSyncManager.class.getName()).log(Level.SEVERE,
					"Broadcasting message on client {0}", msg);
			return;
		}
		msg.time = time;
		server.broadcast(msg);
	}

	/**
	 * send data to a specific client
	 * 
	 * @param client
	 * @param msg
	 */
	public void send(int client, PhysicsSyncMessage msg) {
		if (server == null) {
			Logger.getLogger(ServerPhysicsSyncManager.class.getName()).log(Level.SEVERE,
					"Broadcasting message on client {0}", msg);
			return;
		}
		send(server.getConnection(client), msg);
	}

	/**
	 * send data to a specific client
	 * 
	 * @param client
	 * @param msg
	 */
	public void send(HostedConnection client, PhysicsSyncMessage msg) {
		msg.time = time;
		if (client == null) {
			Logger.getLogger(ServerPhysicsSyncManager.class.getName()).log(Level.SEVERE,
					"Client null when sending: {0}", client);
			return;
		}
		client.send(msg);
	}

	/**
	 * registers the types of messages this PhysicsSyncManager listens to
	 * 
	 * @param classes
	 */
	public void setMessageTypes(@SuppressWarnings("rawtypes") Class... classes) {
			server.removeMessageListener(this);
			server.addMessageListener(this, classes);
	}


	public Server getServer() {
		return server;
	}

	@Override
	public void messageReceived(MessageConnection source, final Message message) {
        assert (message instanceof PhysicsSyncMessage);
        app.enqueue(new Callable<Void>() {

            public Void call() throws Exception {
                for (Iterator<SyncMessageValidator> it = validators.iterator(); it.hasNext();) {
                    SyncMessageValidator syncMessageValidator = it.next();
                    if (!syncMessageValidator.checkMessage((PhysicsSyncMessage) message)) {
                        return null;
                    }
                }
                broadcast((PhysicsSyncMessage) message);
                doMessage((PhysicsSyncMessage) message);
                return null;
            }
        });
	}

}
