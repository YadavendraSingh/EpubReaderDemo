package com.myepub.android.sample.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


class Util {

    companion object{
        val SHARED_PREFENCE_NAME = "ESharedPreference"
        val ENCRYPTED_KEY = "ENCRYPTED KEY"
        val KEY_ALIAS = "KEY_ALIAS"
        val Url = "https://testing-xamidea.s3-ap-southeast-1.amazonaws.com/Chapter-13.epub"

    }
}