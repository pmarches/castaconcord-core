package org.peercentrum.network;

import io.netty.util.internal.EmptyArrays;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.X509TrustManager;

import org.peercentrum.core.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckSelfSignedNodeIdTrustManager implements X509TrustManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(CheckSelfSignedNodeIdTrustManager.class);
  
  private NodeIdentifier expectedNodeId;

  public CheckSelfSignedNodeIdTrustManager(NodeIdentifier acceptedNodeId) {
    this.expectedNodeId=acceptedNodeId;
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    LOGGER.debug("Check Client trusted "+Arrays.asList(chain)+" "+authType);
    verifyCertificate(chain);
  }

  private void verifyCertificate(X509Certificate[] chain) throws CertificateException {
    if(chain.length!=1){
      throw new CertificateException("Was expecting a single self-signed certificate, got "+chain.length+" instead");
    }
    try {
      chain[0].checkValidity();
      chain[0].verify(chain[0].getPublicKey(), "BC"); //Ensure the certificate has been self-signed

      if(expectedNodeId!=null){
        NodeIdentifier nodeIdOnCertificate=new NodeIdentifier(chain[0].getPublicKey().getEncoded());
        if(expectedNodeId.equals(nodeIdOnCertificate)==false){
          throw new CertificateException("The certificate is valid, but we were expecting to connect to node "+expectedNodeId);
        }
      }
    } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException e) {
      throw new CertificateException(e);
    }
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    LOGGER.debug("Client will check if server can be trusted "+Arrays.asList(chain)+" "+authType);
    if(expectedNodeId==null){
      throw new CertificateException("The client needs to know in advance what is the expected node id");
    }
    verifyCertificate(chain);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return EmptyArrays.EMPTY_X509_CERTIFICATES;
  }
}