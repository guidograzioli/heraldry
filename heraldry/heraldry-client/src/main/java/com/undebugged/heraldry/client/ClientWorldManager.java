/*
 * Copyright (c) 2009-2011 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.undebugged.heraldry.client;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.undebugged.heraldry.controls.CharacterAnimControl;
import com.undebugged.heraldry.core.PlayerData;
import com.undebugged.heraldry.core.WorldManager;
import com.undebugged.heraldry.network.PhysicsSyncManager;

/**
 * Base game entity managing class, stores and loads the entities,
 * used on server and on client. Automatically sends changes via network when
 * running on server, used to apply network data on client and server.
 * @author normenhansen
 */
public class ClientWorldManager extends WorldManager {
	
    public ClientWorldManager(Application app, Node rootNode) {
        this.app = app;
        this.rootNode = rootNode;
        this.assetManager = app.getAssetManager();
        this.space = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();
        syncManager = app.getStateManager().getState(PhysicsSyncManager.class);
    }

    /**
     * get the NavMesh of the currently loaded level
     * @return
     */
//    public NavMesh getNavMesh() {
//        return navMesh;
//    }


    /**
     * preloads the models with the given names
     * @param modelNames
     */
    public void preloadModels(String[] modelNames) {
        for (int i = 0; i < modelNames.length; i++) {
            String string = modelNames[i];
            assetManager.loadModel(string);
        }
    }


    /**
     * handle player entering entity (sends message if server)
     * @param playerId
     * @param entityId
     */
    public void enterEntity(long playerId, long entityId) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Player {0} entering entity {1}", new Object[]{playerId, entityId});
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
        }
    }
    
    public void addEntity(long id, String modelIdentifier, Vector3f location, Quaternion rotation) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Adding entity: {0}", id);
        Node entityModel = (Node) assetManager.loadModel(modelIdentifier);
        setEntityTranslation(entityModel, location, rotation);
        if (entityModel.getControl(CharacterControl.class) != null) {
            entityModel.addControl(new CharacterAnimControl());
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
     * play animation on specified entity
     * @param entityId
     * @param animationName
     * @param channel
     */
    public void playEntityAnimation(long entityId, String animationName, int channel) { }


    @Override
    public void update(float tpf) { }

	@Override
	public void playClientEffect(long id, String effectName, Vector3f location, Vector3f endLocation, Quaternion rotation, Quaternion endRotation, float time) {
		ClientEffectsManager manager = app.getStateManager().getState(ClientEffectsManager.class);
    	manager.playEffect(id, effectName, location, endLocation, rotation, endRotation, time);
	} 
}
