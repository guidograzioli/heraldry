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
package com.undebugged.heraldry.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.undebugged.heraldry.controls.ManualCharacterControl;
import com.undebugged.heraldry.controls.ManualVehicleControl;
import com.undebugged.heraldry.controls.MovementControl;
import com.undebugged.heraldry.messages.PhysicsSyncMessage;
import com.undebugged.heraldry.network.PhysicsSyncManager;
import com.undebugged.heraldry.network.SyncMessageValidator;

/**
 * Base game entity managing class, stores and loads the entities,
 * used on server and on client. Automatically sends changes via network when
 * running on server, used to apply network data on client and server.
 * @author normenhansen
 */
public abstract class WorldManager extends AbstractAppState implements SyncMessageValidator {

    protected long myPlayerId = -2;
    protected long myGroupId = -2;
    //private NavMesh navMesh = new NavMesh();
    protected Node rootNode;
    protected Node worldRoot;
    protected HashMap<Long, Spatial> entities = new HashMap<Long, Spatial>();
    protected int newId = 0;
    protected Application app;
    protected AssetManager assetManager;
    //private NavMeshGenerator generator = new NavMeshGenerator();
    protected PhysicsSpace space;
    protected List<Control> userControls = new LinkedList<Control>();
    protected PhysicsSyncManager syncManager;

    /**
     * adds a control to the list of controls that are added to the spatial
     * currently controlled by the user (chasecam, ui control etc.)
     * @param control
     */
    public void addUserControl(Control control) {
        userControls.add(control);
    }

    public long getMyPlayerId() {
        return myPlayerId;
    }

    public void setMyPlayerId(long myPlayerId) {
        this.myPlayerId = myPlayerId;
    }

    /**
     * get the NavMesh of the currently loaded level
     * @return
     */
//    public NavMesh getNavMesh() {
//        return navMesh;
//    }

    /**
     * get the world root node (not necessarily the application rootNode!)
     * @return
     */
    public Node getWorldRoot() {
        return worldRoot;
    }

    public PhysicsSyncManager getSyncManager() {
        return syncManager;
    }

    public PhysicsSpace getPhysicsSpace() {
        return space;
    }

    /**
     * loads the specified level node
     * @param name
     */
    public void loadLevel(String name) {
        worldRoot = (Node) assetManager.loadModel(name);
    }

    /**
     * detaches the level and clears the cache
     */
    public void closeLevel() {

    }

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

    /**
     * attaches the level node to the rootnode
     */
    public void attachLevel() {
        space.addAll(worldRoot);
        rootNode.attachChild(worldRoot);
    }

    /**
     * adds a new player with new id (used on server only)
     * @param id
     * @param groupId
     * @param name
     * @param aiId
     */
    public long addNewPlayer(int groupId, String name, int aiId) {
        long playerId = PlayerData.getNew(name);
        addPlayer(playerId, groupId, name, aiId);
        return playerId;
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
        PlayerData.remove(id);
    }

    /**
     * gets the entity with the specified id
     * @param id
     * @return
     */
    public Spatial getEntity(long id) {
        return entities.get(id);
    }

    /**
     * gets the entity belonging to a PhysicsCollisionObject
     * @param object
     * @return
     */
    public Spatial getEntity(PhysicsCollisionObject object) {
        Object obj = object.getUserObject();
        if (obj instanceof Spatial) {
            Spatial spatial = (Spatial) obj;
            if (entities.containsValue(spatial)) {
                return spatial;
            }
        }
        return null;
    }

    /**
     * finds the entity id of a given spatial if there is one
     * @param entity
     * @return
     */
    public long getEntityId(Spatial entity) {
        for (Iterator<Entry<Long, Spatial>> it = entities.entrySet().iterator(); it.hasNext();) {
            Entry<Long, Spatial> entry = it.next();
            if (entry.getValue() == entity) {
                return entry.getKey();
            }
        }
        return -1;
    }

    /**
     * gets the entity belonging to a PhysicsCollisionObject
     * @param object
     * @return
     */
    public long getEntityId(PhysicsCollisionObject object) {
        Object obj = object.getUserObject();
        if (obj instanceof Spatial) {
            Spatial spatial = (Spatial) obj;
            if (spatial != null) {
                return getEntityId(spatial);
            }
        }
        return -1;
    }

    /**
     * adds a new entity (only used on server)
     * @param modelIdentifier
     * @param location
     * @param rotation
     * @return
     */
    public long addNewEntity(String modelIdentifier, Vector3f location, Quaternion rotation) {
//        long id = 0;
//        while (entities.containsKey(id)) {
//            id++;
//        }
        newId++;
        addEntity(newId, modelIdentifier, location, rotation);
        return newId;
    }

    /**
     * add an entity (vehicle, immobile house etc), always related to a spatial
     * with specific userdata like hp, maxhp etc. (sends message if server)
     * @param id
     * @param modelIdentifier
     * @param location
     * @param rotation
     */
    public abstract void addEntity(long id, String modelIdentifier, Vector3f location, Quaternion rotation);

    public long getMyGroupId() {
		return myGroupId;
	}

	public void setMyGroupId(long myGroupId) {
		this.myGroupId = myGroupId;
	}

	/**
     * removes the entity with the specified id, exits player if inside
     * (sends message if server)
     * @param id
     */
    public void removeEntity(long id) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Removing entity: {0}", id);
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
     * disables an entity so that it is not displayed
     * @param id
     */
    public void disableEntity(long id) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Disabling entity: {0}", id);
        Spatial spat = getEntity(id);
        spat.removeFromParent();
        space.removeAll(spat);
    }

    /**
     * reenables an entity after it has been disabled
     * @param id
     * @param location
     * @param rotation
     */
    public void enableEntity(long id, Vector3f location, Quaternion rotation) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Enabling entity: {0}", id);
        Spatial spat = getEntity(id);
        setEntityTranslation(spat, location, rotation);
        worldRoot.attachChild(spat);
        space.addAll(spat);
    }

    /**
     * sets the translation of an entity based on its type
     * @param entityModel
     * @param location
     * @param rotation
     */
    protected void setEntityTranslation(Spatial entityModel, Vector3f location, Quaternion rotation) {
        if (entityModel.getControl(RigidBodyControl.class) != null) {
            entityModel.getControl(RigidBodyControl.class).setPhysicsLocation(location);
            entityModel.getControl(RigidBodyControl.class).setPhysicsRotation(rotation.toRotationMatrix());
        } else if (entityModel.getControl(CharacterControl.class) != null) {
            entityModel.getControl(CharacterControl.class).setPhysicsLocation(location);
            entityModel.getControl(CharacterControl.class).setViewDirection(rotation.mult(Vector3f.UNIT_Z).multLocal(1, 0, 1).normalizeLocal());
        } else if (entityModel.getControl(VehicleControl.class) != null) {
            entityModel.getControl(VehicleControl.class).setPhysicsLocation(location);
            entityModel.getControl(VehicleControl.class).setPhysicsRotation(rotation.toRotationMatrix());
        } else {
            entityModel.setLocalTranslation(location);
            entityModel.setLocalRotation(rotation);
        }
    }

    /**
     * handle player entering entity
     * @param playerId
     * @param entityId
     */
    public abstract void enterEntity(long playerId, long entityId);
    
    /**
     * makes the specified entity ready to be manually controlled by adding
     * a ManualControl based on the entity type (vehicle etc)
     */
    protected void makeManualControl(long entityId, Client client) {
        Spatial spat = getEntity(entityId);
        if (spat.getControl(CharacterControl.class) != null) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Make manual character control for entity {0} ", entityId);
            if (client != null) {
                //add net sending for users own manual control
                //TODO: using group id as client id
                if ((Integer) spat.getUserData("group_id") == myGroupId) {
                    spat.addControl(new ManualCharacterControl(client, entityId));
                } else {
                    spat.addControl(new ManualCharacterControl());
                }
            } else {
                spat.addControl(new ManualCharacterControl());
            }
        } else if (spat.getControl(VehicleControl.class) != null) {
            Logger.getLogger(this.getClass().getName())
            	.log(Level.INFO, "Make manual vehicle control for entity {0} ", entityId);
            if (client != null) {
                //TODO: using group id as client id
                if ((Integer) spat.getUserData("group_id") == myGroupId) {
                    spat.addControl(new ManualVehicleControl(client, entityId));
                } else {
                    spat.addControl(new ManualVehicleControl());
                }
            } else {
                spat.addControl(new ManualVehicleControl());
            }
        }
    }


    /**
     * adds the user controls for human user to the spatial
     */
    protected void addUserControls(Spatial spat) {
        for (Iterator<Control> it = userControls.iterator(); it.hasNext();) {
            Control control = it.next();
            spat.addControl(control);
        }
    }

    /**
     * removes the user controls for human user to the spatial
     */
    protected void removeUserControls(Spatial spat) {
        for (Iterator<Control> it = userControls.iterator(); it.hasNext();) {
            Control control = it.next();
            spat.removeControl(control);
        }
    }

    /**
     * set user data of specified entity (sends message if server)
     * @param id
     * @param name
     * @param data
     */
    public void setEntityUserData(long id, String name, Object data) {
        getEntity(id).setUserData(name, data);
    }

    /**
     * play animation on specified entity
     * @param entityId
     * @param animationName
     * @param channel
     */
    public abstract void playEntityAnimation(long entityId, String animationName, int channel);
    
    public abstract void playClientEffect(long id, String effectName, Vector3f location, Vector3f endLocation, Quaternion rotation, Quaternion endRotation, float time);

    /**
     * does a ray test that starts at the entity location and extends in its
     * view direction by length, stores collision location in supplied
     * storeLocation vector, if collision object is an entity, returns entity
     * @param entity
     * @param length
     * @param storeVector
     * @return
     */
    public Spatial doRayTest(Spatial entity, float length, Vector3f storeLocation) {
        MovementControl control = entity.getControl(MovementControl.class);
        Vector3f startLocation = control.getLocation();
        Vector3f endLocation = startLocation.add(control.getAimDirection().normalize().multLocal(length));
        List<PhysicsRayTestResult> results = getPhysicsSpace().rayTest(startLocation, endLocation);
        Spatial found = null;
        float dist = Float.MAX_VALUE;
        for (Iterator<PhysicsRayTestResult> it = results.iterator(); it.hasNext();) {
            PhysicsRayTestResult physicsRayTestResult = it.next();
            Spatial object = getEntity(physicsRayTestResult.getCollisionObject());
            if (object == entity) {
                continue;
            }
            if (physicsRayTestResult.getHitFraction() < dist) {
                dist = physicsRayTestResult.getHitFraction();
                if (storeLocation != null) {
                    FastMath.interpolateLinear(physicsRayTestResult.getHitFraction(), startLocation, endLocation, storeLocation);
                }
                found = object;
            }
        }
        return found;
    }

    /**
     * does a ray test, stores collision location in supplied storeLocation vector, if collision
     * object is an entity, returns entity
     * @param storeLocation
     * @return
     */
    public Spatial doRayTest(Vector3f startLocation, Vector3f endLocation, Vector3f storeLocation) {
        List<PhysicsRayTestResult> results = getPhysicsSpace().rayTest(startLocation, endLocation);
        //TODO: sorting of results
        Spatial found = null;
        float dist = Float.MAX_VALUE;
        for (Iterator<PhysicsRayTestResult> it = results.iterator(); it.hasNext();) {
            PhysicsRayTestResult physicsRayTestResult = it.next();
            Spatial object = getEntity(physicsRayTestResult.getCollisionObject());
            if (physicsRayTestResult.getHitFraction() < dist) {
                dist = physicsRayTestResult.getHitFraction();
                if (storeLocation != null) {
                    FastMath.interpolateLinear(physicsRayTestResult.getHitFraction(), startLocation, endLocation, storeLocation);
                }
                found = object;
            }
        }
        return found;
    }

    /**
     * validates messages before they are sent, called from PhysicsSyncManager
     * @param message
     * @return
     */
    public boolean checkMessage(PhysicsSyncMessage message) {
        //TODO: add checks for maximum acceleration etc.
        if (message.syncId >= 0 && getEntity(message.syncId) == null) {
            return false;
        }
        return true;
    }

    @Override
    public void update(float tpf) {
    }
}
