package com.undebugged.heraldry.network;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.state.AbstractAppState;
import com.jme3.network.Message;
import com.jme3.network.MessageConnection;
import com.jme3.network.MessageListener;
import com.undebugged.heraldry.messages.PhysicsSyncMessage;

public abstract class PhysicsSyncManager 
	extends AbstractAppState implements MessageListener<MessageConnection> {

	protected float syncFrequency = 0.25f;
	protected LinkedList<SyncMessageValidator> validators = new LinkedList<SyncMessageValidator>();
	protected HashMap<Long, Object> syncObjects = new HashMap<Long, Object>();
	protected double time = 0;
	protected double offset = Double.MIN_VALUE;
	protected double maxDelay = 0.50;
	protected float syncTimer = 0;
	protected LinkedList<PhysicsSyncMessage> messageQueue = new LinkedList<PhysicsSyncMessage>();

	/**
	 * add an object to the list of objects managed by this sync manager
	 * 
	 * @param id
	 * @param object
	 */
	public void addObject(long id, Object object) {
		syncObjects.put(id, object);
	}

	/**
	 * removes an object from the list of objects managed by this sync manager
	 * 
	 * @param object
	 */
	public void removeObject(Object object) {
		for (Iterator<Entry<Long, Object>> it = syncObjects.entrySet().iterator(); it.hasNext();) {
			Entry<Long, Object> entry = it.next();
			if (entry.getValue() == object) {
				it.remove();
				return;
			}
		}
	}

	/**
	 * removes an object from the list of objects managed by this sync manager
	 * 
	 * @param id
	 */
	public void removeObject(long id) {
		syncObjects.remove(id);
	}

	public void clearObjects() {
		syncObjects.clear();
	}

	/**
	 * executes a message immediately
	 * 
	 * @param message
	 */
	protected void doMessage(PhysicsSyncMessage message) {
		Object object = syncObjects.get(message.syncId);
		if (object != null) {
			message.applyData(object);
		} else {
			Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.WARNING,
					"Cannot find physics object for: ({0}){1}",
					new Object[] { message.syncId, message });
		}
	}

	/**
	 * enqueues the message and updates the offset of the sync manager based on
	 * the time stamp
	 * 
	 * @param message
	 */
	protected void enqueueMessage(PhysicsSyncMessage message) {
		if (offset == Double.MIN_VALUE) {
			offset = this.time - message.time;
			Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.INFO, "Initial offset {0}",
					offset);
		}
		double delayTime = (message.time + offset) - time;
		if (delayTime > maxDelay) {
			offset -= delayTime - maxDelay;
			Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.INFO,
					"Decrease offset due to high delaytime ({0})", delayTime);
		} else if (delayTime < 0) {
			offset -= delayTime;
			Logger.getLogger(PhysicsSyncManager.class.getName()).log(Level.INFO,
					"Increase offset due to low delaytime ({0})", delayTime);
		}
		messageQueue.add(message);
	}

	public void messageSent(Message message) {	}

	public void objectReceived(Object object) {	}

	public void objectSent(Object object) {	}

	public void addMessageValidator(SyncMessageValidator validator) {
		validators.add(validator);
	}

	public void removeMessageValidator(SyncMessageValidator validator) {
		validators.remove(validator);
	}

	public double getMaxDelay() {
		return maxDelay;
	}

	public void setMaxDelay(double maxDelay) {
		this.maxDelay = maxDelay;
	}

	public float getSyncFrequency() {
		return syncFrequency;
	}

	public void setSyncFrequency(float syncFrequency) {
		this.syncFrequency = syncFrequency;
	}

	@Override
	public abstract void messageReceived(MessageConnection source, final Message message);

	@Override
	public abstract void update(float tpf);


	/**
	 * registers the types of messages this PhysicsSyncManager listens to
	 * 
	 * @param classes
	 */
	public abstract void setMessageTypes(@SuppressWarnings("rawtypes") Class... classes);

}
