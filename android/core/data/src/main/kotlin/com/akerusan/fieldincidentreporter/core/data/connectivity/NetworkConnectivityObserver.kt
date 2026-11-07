package com.akerusan.fieldincidentreporter.core.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import com.akerusan.fieldincidentreporter.core.domain.ConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Online means the default network has internet capability: the same condition WorkManager's
 * CONNECTED constraint uses, so "offline" in the UI matches "sync is waiting for a network".
 */
class NetworkConnectivityObserver @Inject constructor(@ApplicationContext private val context: Context) :
    ConnectivityObserver {

    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = checkNotNull(context.getSystemService<ConnectivityManager>())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        val current = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        trySend(current?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .conflate()
}
