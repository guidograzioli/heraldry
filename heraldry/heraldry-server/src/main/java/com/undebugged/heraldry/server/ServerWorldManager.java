package com.undebugged.heraldry.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.undebugged.heraldry.core.PlayerData;
import com.undebugged.heraldry.core.WorldManager;
import com.undebugged.heraldry.messages.ServerAddEntityMessage;
import com.undebugged.heraldry.messages.ServerAddPlayerMessage;
import com.undebugged.heraldry.messages.ServerDisableEntityMessage;
import com.undebugged.heraldry.messages.ServerEffectMessage;
import com.undebugged.heraldry.messages.ServerEnableEntityMessage;
import com.undebugged.heraldry.messages.ServerEnterEntityMessage;
import com.undebugged.heraldry.messages.ServerEntityDataMessage;
import com.undebugged.heraldry.messages.ServerRemoveEntityMessage;
import com.undebugged.heraldry.messages.ServerRemovePlayerMessage;

public class ServerWorldManager extends WorldManager {

	private ServerPhysicsSyncManager syncManager;
	
    public ServerWorldManager(Application app, Node rootNode) {
        this.app = app;
        this.rootNode = rootNode;
        this.assetManager = app.getAssetManager();
        this.space = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();
        syncManager = app.getStateManager().getState(ServerPhysicsSyncManager.class);
    }
    
    /**
     * disables an entity so that it is not displayed
     * @param id
     */
    public void disableEntity(long id) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Disabling entity: {0}", id);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Broadcast removing entity: {0}", id);
        syncManager.broadcast(new ServerDisableEntityMessage(id));
        Spatial spat = getEntity(id);
        spat.removeFromParent();
        space.removeAll(spat);
    }
    
    
    
    @Override
	public ServerPhysicsSyncManager getSyncManager() {
		return syncManager;
	}

	/**
     * handle player entering entity (sends message if server)
     * @param playerId
     * @param entityId
     */
    public void enterEntity(long playerId, long entityId) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Player {0} entering entity {1}", new Object[]{playerId, entityId});
        syncManager.broadcast(new ServerEnterEntityMessage(playerId, entityId));
        long curEntity = PlayerData.getLongData(playerId, "entity_id");
        int groupId = PlayerData.getIntData(playerId, "group_id");
        //reset current entity
        if (curEntity != -1) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Player {0} exiting current entity {1}", new Object[]{playerId, curEntity});
            Spatial curEntitySpat = getEntity(curEntity);
            curEntitySpat.setUserData("player_id", -1l);
            curEntitySpat.setUserData("group_id", -1);
            if (playerId == myPlayerId) {
                removeUserControls(curEntitySpat);
            }
        }
        PlayerData.setData(playerId, "entity_id", entityId);
        //if we entered an entity, configure its controls, id -1 means enter no entity
        if (entityId != -1) {
            Spatial spat = getEntity(entityId);
            spat.setUserData("player_id", playerId);
            spat.setUserData("group_id", groupId);
            if (PlayerData.isHuman(playerId)) {
            	makeManualControl(entityId, null);
            }
        }
    }
    
    /**
     * removes the entity with the specified id, exits player if inside
     * (sends message if server)
     * @param id
     */
    public void removeEntity(long id) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Removing entity: {0}", id);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Broadcast removing entity: {0}", id);
        syncManager.broadcast(new ServerRemoveEntityMessage(id));
        syncManager.removeObject(id);
        Spatial spat = entities.remove(id);
        if (spat == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "try removing entity thats not there: {0}", id);
            return;
        }
        Long playerId = (Long) spat.getUserData("player_id");
        if (playerId == myPlayerId) {
            removeUserControls(spat);
        }
        if (playerId != -1) {
            PlayerData.setData(playerId, "entity_id", -1);
        }
        spat.removeFromParent();
        space.removeAll(spat);
    }
    
    /**
     * adds a player (sends message if server)
     * @param id
     * @param groupId
     * @param name
     * @param aiId
     */
    public void addPlayer(long id, int groupId, String name, int aiId) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Adding player: {0}", id);
        syncManager.broadcast(new ServerAddPlayerMessage(id, name, groupId, aiId));
        PlayerData player = null;
        player = new PlayerData(id, groupId, name, aiId);
        PlayerData.add(id, player);
    }

    /**
     * removes a player
     * @param id
     */
    public void removePlayer(long id) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Removing player: {0}", id);
        //TODO: remove other (AI) entities if this is a human client..
        syncManager.broadcast(new ServerRemovePlayerMessage(id));
        long entityId = PlayerData.getLongData(id, "entity_id");
        if (entityId != -1) {
            enterEntity(id, -1);
        }
        long characterId = PlayerData.getLongData(id, "character_entity_id");
        removeEntity(characterId);
        PlayerData.remove(id);
    }
    
    /**
     * add an entity (vehicle, immobile house etc), always related to a spatial
     * with specific userdata like hp, maxhp etc. (sends message if server)
     * @param id
     * @param modelIdentifier
     * @param location
     * @param rotation
     */
    public void addEntity(long id, String modelIdentifier, Vector3f location, Quaternion rotation) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Adding entity: {0}", id);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Broadcast adding entity: {0}", id);
        syncManager.broadcast(new ServerAddEntityMessage(id, modelIdentifier, location, rotation));
        Node entityModel = (Node) assetManager.loadModel(modelIdentifier);
        setEntityTranslation(entityModel, location, rotation);
        if (entityModel.getControl(CharacterControl.class) != null) {
            entityModel.getControl(CharacterControl.class).setFallSpeed(55);
            entityModel.getControl(CharacterControl.class).setJumpSpeed(15);
        }
        entityModel.setUserData("player_id", -1l);
        entityModel.setUserData("group_id", -1);
        entityModel.setUserData("entity_id", id);
        entities.put(id, entityModel);
        syncManager.addObject(id, entityModel);
        space.addAll(entityModel);
        worldRoot.attachChild(entityModel);
    }
    
    /**
     * reenables an entity after it has been disabled
     * @param id
     * @param location
     * @param rotation
     */
    public void enableEntity(long id, Vector3f location, Quaternion rotation) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Enabling entity: {0}", id);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Broadcast removing entity: {0}", id);
        syncManager.broadcast(new ServerEnableEntityMessage(id, location, rotation));
        Spatial spat = getEntity(id);
        setEntityTranslation(spat, location, rotation);
        worldRoot.attachChild(spat);
        space.addAll(spat);
    }

    public void playWorldEffect(String effectName, Vector3f location, float time) {
        Quaternion rotation = new Quaternion();
        playWorldEffect(-1, effectName, location, rotation, location, rotation, time);
    }

    public void playWorldEffect(String effectName, Vector3f location, Quaternion rotation, float time) {
        playWorldEffect(-1, effectName, location, rotation, location, rotation, time);
    }

    public void playWorldEffect(long id, String effectName, Vector3f location, Quaternion rotation, float time) {
        playWorldEffect(id, effectName, location, rotation, location, rotation, time);
    }

    public void playWorldEffect(long id, String effectName, Vector3f location, Quaternion rotation, Vector3f endLocation, Quaternion endRotation, float time) {
        syncManager.broadcast(new ServerEffectMessage(id, effectName, location, rotation, endLocation, endRotation, time));
    }

    /**
     * set user data of specified entity (sends message if server)
     * @param id
     * @param name
     * @param data
     */
    public void setEntityUserData(long id, String name, Object data) {
        syncManager.broadcast(new ServerEntityDataMessage(id, name, data));
        getEntity(id).setUserData(name, data);
    }

	@Override
	public void playEntityAnimation(long entityId, String animationName, int channel) {}

	@Override
	public void playClientEffect(long id, String effectName, Vector3f location, Vector3f endLocation,
			Quaternion rotation, Quaternion endRotation, float time) {}
}
