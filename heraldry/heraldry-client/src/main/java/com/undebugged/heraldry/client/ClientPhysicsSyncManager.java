package com.undebugged.heraldry.client;

import java.util.Iterator;
import java.util.concurrent.Callable;

import com.jme3.app.Application;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageConnection;
import com.jme3.network.MessageListener;
import com.undebugged.heraldry.messages.PhysicsSyncMessage;
import com.undebugged.heraldry.network.PhysicsSyncManager;

public abstract class ClientPhysicsSyncManager extends PhysicsSyncManager implements MessageListener<MessageConnection> {

	private Client client;
	Application app;

	public ClientPhysicsSyncManager(Application app, Client client) {
		this.app = app;
		this.client = client;
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
		for (Iterator<PhysicsSyncMessage> it = messageQueue.iterator(); it.hasNext();) {
			PhysicsSyncMessage message = it.next();
			if (message.time >= time + offset) {
				doMessage(message);
				it.remove();
			}
		}
	}

	/**
	 * registers the types of messages this PhysicsSyncManager listens to
	 * 
	 * @param classes
	 */
	public void setMessageTypes(@SuppressWarnings("rawtypes") Class... classes) {
		client.removeMessageListener(this);
		client.addMessageListener(this, classes);
	}

	public Client getClient() {
		return client;
	}

	@Override
	public void messageReceived(MessageConnection source, final Message message) {
        assert (message instanceof PhysicsSyncMessage);
        app.enqueue(new Callable<Void>() {
            public Void call() throws Exception {
                enqueueMessage((PhysicsSyncMessage) message);
                return null;
            }
        });
	}

}
