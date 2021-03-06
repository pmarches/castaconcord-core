package org.peercentrum.core;

import java.io.File;

import org.junit.Test;
import org.peercentrum.core.nodegossip.NodeGossipConfig;

public class ServerMainTest {

	@Test
	public void test() throws Exception {
		TopLevelConfig configNode1=TopLevelConfig.loadFromFile(new File("-topConfig.yaml"));
		configNode1.setListenPort(0);
		configNode1.setEnableNAT(false);
		ServerMain node1 = new ServerMain(configNode1);
		NodeGossipConfig node1Gossipconfig=(NodeGossipConfig) configNode1.getAppConfig(NodeGossipConfig.class);
		node1Gossipconfig.setBootstrapEndpoint(null);
		node1.run();

		TopLevelConfig configNode2=TopLevelConfig.loadFromFile(new File("-topConfig.yaml"));
		configNode2.setListenPort(0);
		configNode2.setEnableNAT(false);
		NodeGossipConfig gossipConfigNode2=(NodeGossipConfig) configNode2.getAppConfig(NodeGossipConfig.class);
		gossipConfigNode2.setBootstrapEndpoint("localhost:"+node1.networkServer.getListeningPort());
		ServerMain node2 = new ServerMain(configNode2);
		node2.run();
	}

}
