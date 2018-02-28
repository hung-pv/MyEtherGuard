package test;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
        System.out.println(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     * @throws NoSuchAlgorithmException 
     */
    public void testApp() throws Exception
    {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		byte[] bytes = randomBytes(32);
		byte[] bytes2 = randomBytes(32);
		assertTrue(bytes.length == bytes2.length);
		SecureRandom.getInstanceStrong().nextBytes(bytes);
		SecureRandom.getInstanceStrong().nextBytes(bytes2);
		
		assertFalse(isArrayEq(bytes, bytes2));
		
		String str = Base64.getEncoder().encodeToString(bytes);
		System.out.println(str);
		assertTrue(isArrayEq(bytes, Base64.getDecoder().decode(str)));
    }
    
    private byte[] randomBytes(int size) throws NoSuchAlgorithmException {
    	byte[] bytes = new byte[size];
		SecureRandom.getInstanceStrong().nextBytes(bytes);
		return bytes;
    }
    
    private boolean isArrayEq(byte[] bytes, byte[] bytes2) {
		for(int i = 0; i < bytes.length; i++) {
			if (bytes[i] != bytes2[i]) {
				return false;
			}
		}
		return true;
    }
}
