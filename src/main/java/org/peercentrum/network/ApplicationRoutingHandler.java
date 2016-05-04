package org.peercentrum.network;

import org.peercentrum.application.BaseApplicationMessageHandler;
import org.peercentrum.core.ApplicationIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;

class ApplicationRoutingHandler extends ChannelInboundHandlerAdapter {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRoutingHandler.class);
	
	protected NetworkServer server;

	public ApplicationRoutingHandler(NetworkServer server) {
		this.server=server;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		MDC.put("NodeId", server.serverMain.getLocalIdentity().getIdentifier().toString());
		HeaderAndPayload currentHeaderAndPayload = (HeaderAndPayload) msg;
		ApplicationIdentifier appIdReceived = new ApplicationIdentifier(currentHeaderAndPayload.header.getDestinationApplicationId().toByteArray());
		BaseApplicationMessageHandler applicationHandler=server.serverMain.getApplicationHandler(appIdReceived);
		if(applicationHandler!=null){
			HeaderAndPayload response = applicationHandler.generateReponseFromQuery(ctx, currentHeaderAndPayload);
			if(response!=null){
				ctx.channel().writeAndFlush(response);
			}
		}
		else{
			LOGGER.error("Failed to find application "+appIdReceived);
		}
		ReferenceCountUtil.release(currentHeaderAndPayload.header);
		ReferenceCountUtil.release(msg);
		MDC.remove("NodeId");
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent e = (IdleStateEvent) evt;
			if (e.state() == IdleState.READER_IDLE) {
        LOGGER.debug("Reader idle, closing connection");
				ctx.close();
			} else if (e.state() == IdleState.WRITER_IDLE) {
				//				ctx.writeAndFlush(new PingMessage());
				LOGGER.debug("Writer idle");
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}