package com.undebugged;

import java.io.IOException;

import org.heraldry.core.Heraldry;

import com.jme3.app.SimpleApplication;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.system.JmeContext;

public class HeraldryServer extends SimpleApplication {

	public static void main(String[] args) {
//		ServerMain app = new ServerMain();
//	    app.start(JmeContext.Type.Headless);
	}

	@Override
	public void simpleInitApp() {
		
		Server myServer;
		try {
			myServer = Network.createServer(
					Heraldry.VERSION, Network.DEFAULT_VERSION, Heraldry.DEFAULT_PORT_TCP, Heraldry.DEFAULT_PORT_UDP);
			myServer.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	}
}
