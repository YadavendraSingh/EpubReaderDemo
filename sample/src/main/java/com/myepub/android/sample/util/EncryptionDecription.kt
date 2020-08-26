package com.myepub.android.sample.util

import android.content.Context
import android.content.SharedPreferences
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.myepub.android.sample.util.Util.Companion.KEY_ALIAS
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal
import kotlin.collections.ArrayList


object EncryptionDecription {

    private const val RSA_MODE = "RSA/ECB/PKCS1Padding"
    lateinit var keyStore: KeyStore
    private val algorithm = "AES"
    val AndroidKeyStore = "AndroidKeyStore"
    lateinit var yourKey: SecretKey

    init {
        keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null);
    }


    @Throws(Exception::class)
    fun rsaEncrypt(secret: ByteArray): ByteArray {
        val privateKeyEntry: KeyStore.PrivateKeyEntry =
            keyStore.getEntry(Util.KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        // Encrypt the text
        val inputCipher: Cipher = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL")
        inputCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.getCertificate().getPublicKey())
        val outputStream = ByteArrayOutputStream()
        val cipherOutputStream =
            CipherOutputStream(outputStream, inputCipher)
        cipherOutputStream.write(secret)
        cipherOutputStream.close()
        return outputStream.toByteArray()
    }

    @Throws(Exception::class)
    fun rsaDecrypt(encrypted: ByteArray): ByteArray {
        val privateKeyEntry: KeyStore.PrivateKeyEntry =
            keyStore.getEntry(Util.KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        val output: Cipher = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL")
        output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey())
        val cipherInputStream = CipherInputStream(
            ByteArrayInputStream(encrypted), output
        )
        val values: ArrayList<Byte> = ArrayList()
        var nextByte: Int = 0
        while (cipherInputStream.read().also({ nextByte = it }) != -1) {
            values.add(nextByte.toByte())
        }
        val bytes = ByteArray(values.size)
        for (i in bytes.indices) {
            bytes[i] = values[i].toByte()
        }
        return bytes
    }

    fun getSecretKey(context: Context): SecretKey {
        val pref: SharedPreferences =
            context.getSharedPreferences(Util.SHARED_PREFENCE_NAME, Context.MODE_PRIVATE)
        var enryptedKeyB64: String? = pref.getString(Util.ENCRYPTED_KEY, null)
        if (enryptedKeyB64 == null) {
            val key = ByteArray(16)
            val secureRandom = SecureRandom()
            secureRandom.nextBytes(key)
            val encryptedKey: ByteArray = rsaEncrypt(key)
            enryptedKeyB64 = Base64.encodeToString(encryptedKey, Base64.DEFAULT)
            val edit: SharedPreferences.Editor = pref.edit()
            edit.putString(Util.ENCRYPTED_KEY, enryptedKeyB64)
            edit.commit()

        }
        val encryptedKey =
            Base64.decode(enryptedKeyB64, Base64.DEFAULT)
        val key: ByteArray = rsaDecrypt(encryptedKey)
        return SecretKeySpec(key, "AES")

    }

    //-----
    @Throws(java.lang.Exception::class)
    fun encodeFile(yourKey: SecretKey, fileData: ByteArray?): ByteArray? {
        var encrypted: ByteArray? = null
        val data: ByteArray = yourKey.getEncoded()
        val skeySpec = SecretKeySpec(data, 0, data.size, algorithm)
        val cipher: Cipher = Cipher.getInstance(algorithm)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            skeySpec,
            IvParameterSpec(ByteArray(cipher.getBlockSize()))
        )
        encrypted = cipher.doFinal(fileData)
        return encrypted
    }

    @Throws(java.lang.Exception::class)
    fun decodeFile(yourKey: SecretKey?, fileData: ByteArray?): ByteArray? {
        var decrypted: ByteArray? = null
        val cipher: Cipher = Cipher.getInstance(algorithm)
        cipher.init(
            Cipher.DECRYPT_MODE,
            yourKey,
            IvParameterSpec(ByteArray(cipher.getBlockSize()))
        )
        decrypted = cipher.doFinal(fileData)
        return decrypted
    }

    //Encryption and decryption
    @Throws(NoSuchAlgorithmException::class)
    fun getNGenerateSecretKey(context: Context): SecretKey {
        val pref: SharedPreferences =
            context.getSharedPreferences(Util.SHARED_PREFENCE_NAME, Context.MODE_PRIVATE)
        var enryptedKeyB64: String? = pref.getString(Util.ENCRYPTED_KEY, null)
        // decode the base64 encoded string

        if(enryptedKeyB64==null){
            // Generate a 256-bit key
            val outputKeyLength = 256
            val secureRandom = SecureRandom()
            // Do *not* seed secureRandom! Automatically seeded from system entropy.
            val keyGenerator: KeyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(outputKeyLength, secureRandom)
             yourKey = keyGenerator.generateKey()
            enryptedKeyB64 = Base64.encodeToString(yourKey.encoded, Base64.DEFAULT)
            val edit: SharedPreferences.Editor = pref.edit()
            edit.putString(Util.ENCRYPTED_KEY, enryptedKeyB64)
            edit.commit()
        }
        else{
            // decode the base64 encoded string
            val decodedKey: ByteArray = Base64.decode(enryptedKeyB64, Base64.DEFAULT)
            // rebuild key using SecretKeySpec
            yourKey = SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")

        }

        return yourKey
    }
}