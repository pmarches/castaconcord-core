package org.peercentrum.blob;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import org.peercentrum.blob.P2PBlobRangeSet.DiscreteIterator;
import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.PB.GenericResponse;
import org.peercentrum.core.PB.HeaderMessage;
import org.peercentrum.core.PB.P2PBlobBlobRequestMsg;
import org.peercentrum.core.PB.P2PBlobRequestMsg;
import org.peercentrum.core.PB.P2PBlobResponseMsg;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.peercentrum.h2pk.HashIdentifier;
import org.peercentrum.network.BaseApplicationMessageHandler;
import org.peercentrum.network.HeaderAndPayload;
import org.peercentrum.network.NetworkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2PBlobApplication extends BaseApplicationMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(P2PBlobApplication.class);
	public static final ApplicationIdentifier APP_ID=new ApplicationIdentifier(P2PBlobApplication.class.getSimpleName().getBytes());
	public static final int BLOCK_SIZE = 256*1024;

	protected P2PBlobRepository blobRepository;
	
	public P2PBlobApplication(NetworkServer clientOrServer, P2PBlobRepository blobRepository) {
		super(clientOrServer);
		this.blobRepository=blobRepository;
		clientOrServer.addApplicationHandler(this);
	}

	@Override
	public ApplicationIdentifier getApplicationId() {
		return APP_ID;
	}

	@Override
	public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedRequest) {
		try {
			PB.P2PBlobRequestMsg request=(P2PBlobRequestMsg)ProtobufByteBufCodec.decodeNoLengthPrefix(receivedRequest.payload, P2PBlobRequestMsg.class);
			PB.P2PBlobResponseMsg.Builder appLevelResponse=P2PBlobResponseMsg.newBuilder();
			
			if(request.getBlobRequestCount()>1){
			  throw new Exception("Not implemented"); //TODO send back multiple blobs if requested
			}
			for(PB.P2PBlobBlobRequestMsg blobReq:request.getBlobRequestList()){
	      GenericResponse.Builder genericResponse=null;
	      if(blobReq.hasRequestedHash()==false){
	        LOGGER.error("request {} is missing the blob hash", request);
	        genericResponse=GenericResponse.newBuilder();
	        genericResponse.setErrorCode(1);
	        genericResponse.setErrorMessage("Missing requested blob hash");
	        appLevelResponse.setGenericResponse(genericResponse);
	      }
	      if(genericResponse!=null){
	        ByteBuf payload=ProtobufByteBufCodec.encodeNoLengthPrefix(appLevelResponse);
	        return new HeaderAndPayload(newResponseHeaderForRequest(receivedRequest), payload);
	      }
	      //The request looks sane...
	      completeResponse(blobReq, appLevelResponse);
			}

			ByteBuf appSpecificResponseBytes=ProtobufByteBufCodec.encodeNoLengthPrefix(appLevelResponse);
			HeaderMessage.Builder responseHeader = newResponseHeaderForRequest(receivedRequest);
			return new HeaderAndPayload(responseHeader, appSpecificResponseBytes);
		} catch (Exception e) {
			LOGGER.error("Failed to generateResponseFromQuery", e);
			return null;
		}
	}

	private void completeResponse(P2PBlobBlobRequestMsg blobReq, PB.P2PBlobResponseMsg.Builder appLevelResponseBuilder) throws Exception {
		HashIdentifier blobHash=new HashIdentifier(blobReq.getRequestedHash().toByteArray());
		P2PBlobStoredBlob storedBlob=blobRepository.getStoredBlob(blobHash);
		LOGGER.debug("Got a request for blob hash {}", blobHash);
		
		if(storedBlob==null){ //BLOB not found
      throw new RuntimeException("Not implemented");
		}
		
		if(storedBlob.isBlobDownloadComplete()==false){
			throw new RuntimeException("Not implemented"); //We should be able to seed other nodes with the partial bytes we have...
		}

		if(blobReq.hasRequestMetaData() && blobReq.getRequestMetaData()){
		  PB.P2PBlobMetaDataMsg.Builder metaDataMsg=PB.P2PBlobMetaDataMsg.newBuilder();
		  metaDataMsg.setHashList(storedBlob.getHashList().toHashListMsg());
		  metaDataMsg.setBlobLength(storedBlob.getBlobLength());
		  
      appLevelResponseBuilder.setMetaData(metaDataMsg);
		}

		P2PBlobRangeSet requestedRanges;
		if(blobReq.getRequestedRangesCount()==0){
      requestedRanges=storedBlob.getLocalRange(); //Provide everything we have
		}
		else{
      requestedRanges=new P2PBlobRangeSet(blobReq.getRequestedRangesList());
      requestedRanges.intersectionThis(storedBlob.getLocalRange());
		}
		
		DiscreteIterator di = requestedRanges.discreteIterator();
		while(di.hasNext()){
			int blockIndex=di.next();
			long offset=blockIndex*BLOCK_SIZE;
			PB.P2PBlobBlockMsg.Builder blockMsg=PB.P2PBlobBlockMsg.newBuilder();
			blockMsg.setBlockIndex(blockIndex);
			blockMsg.setBlobBytes(storedBlob.getBytesRange(offset, BLOCK_SIZE));
			appLevelResponseBuilder.addBlobBytes(blockMsg);
		}

		/////////TODO enable all this..///////
//		if(request.hasMaximumBlobLength()){
//			long maximumBlobLength=request.getMaximumBlobLength();
//			throw new RuntimeException("Not implemented");
//		}
	}
}
