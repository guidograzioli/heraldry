package com.undebugged.heraldry.messages;

import com.jme3.network.serializing.Serializable;
import com.jme3.network.AbstractMessage;

@Serializable()
public abstract class PhysicsSyncMessage extends AbstractMessage {

    public long syncId = -1;
    public double time;

    public PhysicsSyncMessage() {
    }

    public PhysicsSyncMessage(long id) {
        this.syncId = id;
    }

    public abstract void applyData(Object object);
}
