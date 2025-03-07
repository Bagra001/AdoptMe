package de.grabelus.adoptme.utils

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKeys
import java.security.KeyStore
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class PasswordCrypter : Service() {

    lateinit var context: Context

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

    companion object {
        val KEY_ALIAS: String = "PasswordAdoptMeKey_${UUID.randomUUID().toString()}"
        var sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
            "secure_prefs",
            MasterKey.Builder().getOrCreate(MasterKeys.AES256_GCM_SPEC),
            applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_GCM,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        init {

            with(sharedPreferences.edit()) {
                putString("key_alias", KEY_ALIAS)
                apply()
            }
        }

        fun encryptPassword(password: String): String? {
            try {
                val secretKey: SecretKey = getSecretKey()
                val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv: ByteArray = cipher.iv
                val encryption: ByteArray = cipher.doFinal(password.toByteArray())
                val combined = ByteArray(iv.size + encryption.size)
                System.arraycopy(iv, 0, combined, 0, iv.size)
                System.arraycopy(encryption, 0, combined, iv.size, encryption.size)
                return Base64.getEncoder().encodeToString(combined)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        fun decryptPassword(encryptedPassword: String): String? {
            try {
                val secretKey: SecretKey = getSecretKey()
                val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val decoded: ByteArray = Base64.getDecoder().decode(encryptedPassword)
                val iv = ByteArray(12) // GCM iv size = 12
                System.arraycopy(decoded, 0, iv, 0, iv.size)
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                val encryptedData = ByteArray(decoded.size - iv.size)
                System.arraycopy(decoded, iv.size, encryptedData, 0, encryptedData.size)
                val decryptedBytes: ByteArray = cipher.doFinal(encryptedData)
                return String(decryptedBytes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        private fun getSecretKey(): SecretKey {
            val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }

        private fun generateKey() {
            try {
                val keyGenerator: KeyGenerator =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val keyGenParameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            } catch (e: Exception) {
                e.printStackTrace();
            }
        }
    }
}