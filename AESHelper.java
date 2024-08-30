
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

@Service
public final class AESHelper implements IAESHelper{

    final private static SecureRandom secureRandom = new SecureRandom();

    // Configs to Generate Secret Key
    final private String secretKeyGenAlgo = "PBKDF2WithHmacSHA256";
    final private int secretKeyIterations = 65536;
    final private int secretKeyLen = 128;
    final private String keyGenForAlgo = "AES";

    // Configs for Encryption and Decryption
    private String encryptAlgo; 

    private final SecretKey secretKey;

    private final Cipher encryptCipher;

    private final Cipher decryptCipher;

    final byte[] saltBytes;

    final int ivBlockSize = 16;

    Logger log = LoggerFactory.getLogger(AESHelper.class);


    public AESHelper(@Value("${app.aes.secret}") String secret, @Value("${app.aes.salt}") String saltB64,  @Value("${app.aes.algo}") String encryptAlgo){

        // Salt Initialization
        this.saltBytes = Base64.getDecoder().decode(saltB64);
        this.encryptAlgo = encryptAlgo;

        // Encrypt Cipher Initialize
        encryptCipher = getInstance();
        secretKey = getKeyFromPassword(secret, saltBytes);

        // Decrypt Cipher Initialize
        decryptCipher = getInstance();

    }

    // Can be used to generate random salt and IV (Initialization Vector)
    public static byte[] generateSecureBytes(int noOfBytes){
        byte[] randSecret = new byte[noOfBytes];
        secureRandom.nextBytes(randSecret);
        return randSecret;
    }

    // To create SecretKey Object
    private SecretKey getKeyFromPassword(String password, byte[] salt) {
        SecretKey secret;
        try {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(secretKeyGenAlgo);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, secretKeyIterations, secretKeyLen);
        secret = new SecretKeySpec(factory.generateSecret(spec)
                .getEncoded(), keyGenForAlgo);
        } catch(NoSuchAlgorithmException | InvalidKeySpecException e){
            log.error("Exception at AESHelper - getKeyFromPassword: {}" ,e.getMessage());
            secret = null;
        }
        return secret;
    }

    // To get new instance of Cipher
    // Used only in constructor
    public Cipher getInstance(){
        Cipher cipher;
        try{
            cipher = Cipher.getInstance(encryptAlgo);
        } catch(NoSuchAlgorithmException | NoSuchPaddingException e){
            log.error("Exception at AESHelper - getInstance: {}" , e.getMessage());
            cipher = null;
        }
        return cipher;
    }

    // To init the cipher with secretkey and IV Parameter
    // To reinit the cipher on exception
    private void initCipher(Cipher cipher, int cipherMode, SecretKey s, IvParameterSpec i) {
        try {
            cipher.init(cipherMode, s, i);
        } catch(InvalidAlgorithmParameterException | InvalidKeyException e){
            log.error("Exception at initCipher: {}" , e.getMessage());
        }
    }

    @Override
    public String encrypt(Object input){
        if (input == null) return null;
        synchronized (encryptCipher) {
            String inputStr = input.toString();
            byte[] cipherText;
            try {
                byte[] ivBytes = initializeEncryptCipher();
                cipherText = encryptCipher.doFinal(inputStr.getBytes());

                return Base64.getEncoder().encodeToString(ivBytes) + "@IV@" +
                         Base64.getEncoder().encodeToString(cipherText);
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                log.error("Exception at encrypt - Reinit: {}",e.getMessage() );
                return null;
            } catch (Exception e) {
                log.error("Exception at encrypt: {}" , e.getMessage());
                return null;
            }
        }
    }

    @Override
    public String decrypt(String input){
        if(input == null) return null;
        synchronized (decryptCipher){
            byte[] decryptedBytes;
            try {

                String [] splitText = input.split("@IV@");
                if(splitText.length != 2){
                    throw new IllegalArgumentException("Invalid input text");
                }
                byte[] iv = Base64.getDecoder().decode(splitText[0]);
                byte[] cipherText = Base64.getDecoder().decode(splitText[1]);

                initializeDecryptCipher(iv);

                 decryptedBytes = decryptCipher.doFinal(cipherText);

                 return new String(decryptedBytes);
            } catch(IllegalBlockSizeException | BadPaddingException | IllegalArgumentException e){
                log.error("Exception at decrypt - Reinit:  {}" , e.getMessage());
                return null;
            } catch(Exception e){
                log.error("Exception at decrypt: {}" , e.getMessage());
                return null;
            }
        }
    }

    @Override
    public boolean encryptFiles(MultipartFile file, String filePath) {
        synchronized (encryptCipher) {
            boolean status;
            try {
                byte[] inputBytes = file.getBytes();

                byte[] ivBytes = initializeEncryptCipher();
                byte[] outputBytes = encryptCipher.doFinal(inputBytes);

                byte[] combinedBytes = new byte[ivBytes.length + outputBytes.length];

                System.arraycopy(ivBytes,0,combinedBytes,0, ivBytes.length);
                System.arraycopy(outputBytes,0,combinedBytes,ivBytes.length,outputBytes.length);

                Files.write(Paths.get(filePath), combinedBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                status = true;
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                log.error("Exception at encryptFiles - Reinit: {}" , e.getMessage());
                status = false;
            } catch (Exception e) {
                log.error("Exception at encryptFiles: {}" , e.getMessage());
                status = false;
            }
            return status;
        }
    }

    @Override
    public byte[] decryptFiles(String filePath) {
        synchronized (decryptCipher) {
            byte[] outputBytes;
            try {
                byte[] inputBytes = Files.readAllBytes(Paths.get(filePath));

                byte[] iv = Arrays.copyOfRange(inputBytes, 0, ivBlockSize);
                byte[] fileBytes = Arrays.copyOfRange(inputBytes, ivBlockSize, inputBytes.length);

                initializeDecryptCipher(iv);

                outputBytes = decryptCipher.doFinal(fileBytes);
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                log.error("Exception at decryptFiles - Reinit: {}" , e.getMessage());
                outputBytes = null;
            } catch (Exception e) {
                log.error("Exception at decryptFiles: {}" , e.getMessage());
                outputBytes = null;
            }
            return outputBytes;
        }
    }

    public void initializeDecryptCipher(byte[] iv) {
        try {
            decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        } catch(InvalidAlgorithmParameterException | InvalidKeyException e){
            log.error("Exception at DECRYPT_MODE initCipher: {}" , e.getMessage());
        }
    }

    public byte[] initializeEncryptCipher() {
        //Initialization Vector (IV) Initialization
        byte[] ivBytes = new byte[ivBlockSize];
        secureRandom.nextBytes(ivBytes);
        IvParameterSpec ivParamSpec = new IvParameterSpec(ivBytes);

        try {
            encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParamSpec);
        } catch(InvalidAlgorithmParameterException | InvalidKeyException e){
            log.error("Exception at ENCRYPT_MODE initCipher: {}" , e.getMessage());
        }

        return ivBytes;
    }

}
