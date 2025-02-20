/*
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.Address
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.launch
import org.microg.nlp.api.Constants.ACTION_GEOCODER_BACKEND
import org.microg.nlp.api.GeocoderBackendService
import org.microg.nlp.backend.nominatim.BackendService
import org.microg.nlp.service.api.Constants
import java.io.PrintWriter
import java.util.concurrent.CopyOnWriteArrayList

private const val TAG = "GeocodeFuser"

class GeocodeFuser(private val context: Context, private val lifecycle: Lifecycle) : LifecycleOwner {

    companion object {
        @JvmStatic
        public val backendHelpers = CopyOnWriteArrayList<GeocodeBackendHelper>()
    }

    suspend fun reset() {
        unbind()
        backendHelpers.clear()
        for (backend in Constants.GEOCODE_BACKENDS) {
            if (backend.equals("org.microg.nlp.backend.nominatim.BackendService")) {
                backendHelpers.add(GeocodeBackendHelper(context, lifecycle, BackendService(context), null))
            }
        }
        sendMessageToActivity()
    }

    private fun sendMessageToActivity() {
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("LocationGeocodeBackendUpdated"))
    }

    fun bind() {
        for (backendHelper in backendHelpers) {
            backendHelper.bind()
        }
    }

    suspend fun unbind() {
        for (backendHelper in backendHelpers) {
            backendHelper.unbind()
        }
    }

    private fun unbindNow() {
        for (backendHelper in backendHelpers) {
            backendHelper.unbindNow()
        }
    }

    fun destroy() {
        unbindNow()
        backendHelpers.clear()
    }

    fun dump(writer: PrintWriter?) {
        writer?.println("${backendHelpers.size} backends:")
        for (helper in backendHelpers) {
            helper?.dump(writer)
        }
    }

    suspend fun getFromLocation(latitude: Double, longitude: Double, maxResults: Int, locale: String): List<Address>? {
        if (backendHelpers.isEmpty())
            return null
        val result = ArrayList<Address>()
        for (backendHelper in backendHelpers) {
            val backendResult = backendHelper.getFromLocation(latitude, longitude, maxResults, locale)
            result.addAll(backendResult)
        }
        return result
    }

    fun getFromLocationSync(latitude: Double, longitude: Double, maxResults: Int, locale: String): List<Address>? {
        if (backendHelpers.isEmpty())
            return null
        val result = ArrayList<Address>()
        for (backendHelper in backendHelpers) {
            val backendResult = backendHelper.getFromLocationSync(latitude, longitude, maxResults, locale)
            result.addAll(backendResult)
        }
        return result
    }

    suspend fun getFromLocationName(locationName: String, maxResults: Int, lowerLeftLatitude: Double, lowerLeftLongitude: Double, upperRightLatitude: Double, upperRightLongitude: Double, locale: String): List<Address>? {
        if (backendHelpers.isEmpty())
            return null
        val result = ArrayList<Address>()
        for (backendHelper in backendHelpers) {
            val backendResult = backendHelper.getFromLocationName(locationName, maxResults, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale)
            result.addAll(backendResult)
        }
        return result
    }

    fun getFromLocationNameSync(locationName: String, maxResults: Int, lowerLeftLatitude: Double, lowerLeftLongitude: Double, upperRightLatitude: Double, upperRightLongitude: Double, locale: String): List<Address>? {
        if (backendHelpers.isEmpty())
            return null
        val result = ArrayList<Address>()
        for (backendHelper in backendHelpers) {
            val backendResult = backendHelper.getFromLocationNameSync(locationName, maxResults, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale)
            result.addAll(backendResult)
        }
        return result
    }

    override fun getLifecycle(): Lifecycle = lifecycle
}

class GeocodeBackendHelper(context: Context, lifecycle: Lifecycle, serviceIntent: GeocoderBackendService, signatureDigest: String?) : AbstractBackendHelper(TAG, context, lifecycle, serviceIntent, signatureDigest) {
    public var backend: AsyncGeocoderBackend = AsyncGeocoderBackend(serviceIntent, serviceIntent.toString() + "-geocoder-backend")

    suspend fun getFromLocation(latitude: Double, longitude: Double, maxResults: Int,
                                locale: String): List<Address> {
        if (backend == null) {
            Log.d(TAG, "Not (yet) bound.")
            return emptyList()
        }
        try {
            return backend!!.getFromLocation(latitude, longitude, maxResults, locale)
        } catch (e: Exception) {
            Log.w(TAG, e)
            unbind()
            return emptyList()
        }
    }

    fun getFromLocationSync(latitude: Double, longitude: Double, maxResults: Int,
                            locale: String): List<Address> {
        if (backend == null) {
            Log.d(TAG, "Not (yet) bound.")
            return emptyList()
        }
        try {
            return backend!!.getFromLocationSync(latitude, longitude, maxResults, locale)
        } catch (e: Exception) {
            Log.w(TAG, e)
            lifecycleScope.launch { unbind() }
            return emptyList()
        }
    }

    suspend fun getFromLocationName(locationName: String, maxResults: Int,
                                    lowerLeftLatitude: Double, lowerLeftLongitude: Double,
                                    upperRightLatitude: Double, upperRightLongitude: Double,
                                    locale: String): List<Address> {
        if (backend == null) {
            Log.d(TAG, "Not (yet) bound.")
            return emptyList()
        }
        try {
            return backend!!.getFromLocationName(locationName, maxResults, lowerLeftLatitude,
                lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale)
        } catch (e: Exception) {
            Log.w(TAG, e)
            unbind()
            return emptyList()
        }
    }

    fun getFromLocationNameSync(locationName: String, maxResults: Int,
                                lowerLeftLatitude: Double, lowerLeftLongitude: Double,
                                upperRightLatitude: Double, upperRightLongitude: Double,
                                locale: String): List<Address> {
        if (backend == null) {
            Log.d(TAG, "Not (yet) bound.")
            return emptyList()
        }
        try {
            return backend!!.getFromLocationNameSync(locationName, maxResults, lowerLeftLatitude,
                lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale)
        } catch (e: Exception) {
            Log.w(TAG, e)
            lifecycleScope.launch { unbind() }
            return emptyList()
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        super.onServiceConnected(name, service)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        super.onServiceDisconnected(name)
        //backend = null
    }

    @Throws(RemoteException::class)
    public override suspend fun close() {
        backend!!.close()
    }

    public override fun hasBackend(): Boolean {
        return backend != null
    }
}
