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

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.network.Client;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
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

	private Client client;
	
    public ClientWorldManager(Application app, Node rootNode) {
        this.app = app;
        this.rootNode = rootNode;
        this.assetManager = app.getAssetManager();
        this.space = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();
        this.client = app.getStateManager().getState(ClientPhysicsSyncManager.class).getClient();
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
     * creates the nav mesh for the loaded level
     */
//    public void createNavMesh() {
//
//        Mesh mesh = new Mesh();
//
//        //version a: from mesh
//        GeometryBatchFactory.mergeGeometries(findGeometries(worldRoot, new LinkedList<Geometry>()), mesh);
//        Mesh optiMesh = generator.optimize(mesh);
//
//        navMesh.loadFromMesh(optiMesh);
//
//        //TODO: navmesh only for debug
//        Geometry navGeom = new Geometry("NavMesh");
//        navGeom.setMesh(optiMesh);
//        Material green = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
//        green.setColor("Color", ColorRGBA.Green);
//        green.getAdditionalRenderState().setWireframe(true);
//        navGeom.setMaterial(green);
//
//        worldRoot.attachChild(navGeom);
//    }

    private List<Geometry> findGeometries(Node node, List<Geometry> geoms) {
        for (Iterator<Spatial> it = node.getChildren().iterator(); it.hasNext();) {
            Spatial spatial = it.next();
            if (spatial instanceof Geometry) {
                geoms.add((Geometry) spatial);
            } else if (spatial instanceof Node) {
                findGeometries((Node) spatial, geoms);
            }
        }
        return geoms;
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
            removeTransientControls(curEntitySpat);
            removeAIControls(curEntitySpat);
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
                if (groupId == getMyGroupId()) { //only true on clients
                    makeManualControl(entityId, client);
                    //move controls for local user to new spatial
                    if (playerId == getMyPlayerId()) {
                        addUserControls(spat);
                    }
                } else {
                    makeManualControl(entityId, null);
                }
            } else {
                if (groupId == getMyGroupId()) { //only true on clients
                    makeAutoControl(entityId, client);
                    addAIControls(playerId, entityId);
                } else {
                    makeAutoControl(entityId, null);
                }
            }
        }
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
}
