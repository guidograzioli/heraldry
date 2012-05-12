package com.undebugged.heraldry.network;

import com.undebugged.heraldry.messages.PhysicsSyncMessage;

public interface SyncMessageValidator {
	public boolean checkMessage(PhysicsSyncMessage message);
}
