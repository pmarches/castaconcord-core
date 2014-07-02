package org.castaconcord.h2pk;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.castaconcord.core.ProtocolBuffer.H2PKDBSyncQuery;
import org.castaconcord.core.ProtocolBuffer.H2PKDBSyncResponse;
import org.castaconcord.core.ProtocolBuffer.H2PKDBSyncUnit;
import org.castaconcord.core.ProtocolBuffer.HashToPublicKeyMessage;
import org.junit.Test;

public class HashToPublicKeyDBTest {

	@Test
	public void testGenerateDbSyncResponse() throws Exception {
		BazarroHashPointerSignature signature=new BazarroHashPointerSignature();
		BazarroHashIdentifier address0=new BazarroHashIdentifier(new byte[32]);
		BazarroHashIdentifier address1=new BazarroHashIdentifier();
		byte[] address1Bytes = address1.getBytes().clone();
		address1Bytes[0]++; //Ensure address1 is smaller than address2
		BazarroHashIdentifier address2=new BazarroHashIdentifier(address1Bytes);
		BazarroPublicKeyIdentifier publicKey1=new BazarroPublicKeyIdentifier();
		BazarroPublicKeyIdentifier publicKey2=new BazarroPublicKeyIdentifier();
		BazarroPublicKeyIdentifier publicKey3=new BazarroPublicKeyIdentifier();

		HashToPublicKeyDB db = new HashToPublicKeyDB();
		db.applyOneTransaction(new HashToPublicKeyTransaction(address0, publicKey1, true, signature));
		db.applyOneTransaction(new HashToPublicKeyTransaction(address1, publicKey1, true, signature));
		db.applyOneTransaction(new HashToPublicKeyTransaction(address1, publicKey2, true, signature));
		db.applyOneTransaction(new HashToPublicKeyTransaction(address1, publicKey3, true, signature));
		db.incrementVersion();
		db.applyOneTransaction(new HashToPublicKeyTransaction(address2, publicKey1, true, signature));
		db.incrementVersion();

		
		H2PKDBSyncQuery.Builder dbSyncQuery=H2PKDBSyncQuery.newBuilder();
		dbSyncQuery.setBeginDbVersionNumber(0);
		
		HashToPublicKeyMessage.Builder appLevelResponseBuilder=HashToPublicKeyMessage.newBuilder();
		db.generateDbSyncResponse(dbSyncQuery.build(), appLevelResponseBuilder);
		
		H2PKDBSyncResponse dbSyncResponse = appLevelResponseBuilder.getDbSyncResponse();
		assertEquals(3, dbSyncResponse.getLastDbVersionNumber());
		List<H2PKDBSyncUnit> dataUnits = dbSyncResponse.getSyncUnitsList();
		assertEquals(3, dataUnits.size());

		H2PKDBSyncUnit dataUnit1=dataUnits.get(1);
		assertArrayEquals(address1.getBytes(), dataUnit1.getAddress().toByteArray());
		assertEquals(3, dataUnit1.getPublicKeysRegisteredCount());

		H2PKDBSyncUnit dataUnit2=dataUnits.get(2);
		assertArrayEquals(address2.getBytes(), dataUnit2.getAddress().toByteArray());
		assertEquals(1, dataUnit2.getPublicKeysRegisteredCount());
		
		HashToPublicKeyDB<HashToPublicKeyTransaction> targetDB = new HashToPublicKeyDB<HashToPublicKeyTransaction>();
		targetDB.applyOneTransaction(new HashToPublicKeyTransaction(address2, publicKey1, true, signature));
		targetDB.incrementVersion();
		targetDB.integrateSyncUnit(dbSyncResponse);
		assertEquals(db, targetDB);

		db.close();
		targetDB.close();
	}

}
