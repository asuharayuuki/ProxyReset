package org.eu.suisei.proxyreset

import android.content.Context
import android.content.SharedPreferences
import android.app.ActivityManager
import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.eu.suisei.proxyreset.ui.components.PowerButton
import org.eu.suisei.proxyreset.ui.theme.*
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var proxies: ProxyRepository
    private val worker = Executors.newSingleThreadExecutor()
    private val uiHandler = Handler(Looper.getMainLooper())
    private var hasPermission by mutableStateOf(false)
    private var isConnected by mutableStateOf(false)
    private var targetProxy by mutableStateOf("127.0.0.1:2080")
    private var busy by mutableStateOf(false)
    private val proxyObserver = object : ContentObserver(uiHandler) {
        override fun onChange(selfChange: Boolean) {
            if (!busy) refreshState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = getSharedPreferences("anywhere_proxy_prefs", Context.MODE_PRIVATE)
        proxies = ProxyRepository(this)

        setContent {
            AnywhereTheme {
                HomeScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        listOf(Settings.Global.HTTP_PROXY, "global_http_proxy_host", "global_http_proxy_port", "global_proxy_pac_url")
            .forEach { contentResolver.registerContentObserver(Settings.Global.getUriFor(it), false, proxyObserver) }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        refreshState()
    }

    override fun onStop() {
        contentResolver.unregisterContentObserver(proxyObserver)
        super.onStop()
    }

    override fun onDestroy() {
        worker.shutdownNow()
        uiHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
        if (isFinishing && !isChangingConfigurations) {
            // Task removal also lets Android kill cached/frozen processes. This handles a final
            // Activity destruction delivered to us, without a service or background polling.
            uiHandler.post {
                val manager = getSystemService(ActivityManager::class.java)
                if (manager.appTasks.none { (it.taskInfo?.numActivities ?: 0) > 0 }) {
                    manager.runningAppProcesses.orEmpty()
                        .filter { it.uid == Process.myUid() && it.pid != Process.myPid() }
                        .forEach { Process.killProcess(it.pid) }
                    Process.killProcess(Process.myPid())
                }
            }
        }
    }

    private fun refreshState() {
        hasPermission = proxies.hasPermission()
        val current = proxies.current()
        isConnected = current != null
        targetProxy = current?.toString()
            ?: ProxyEndpoint.parse(prefs.getString("target_proxy", "") ?: "")?.toString()
            ?: "127.0.0.1:2080"
        if (current != null && prefs.getString("target_proxy", null) != targetProxy) {
            prefs.edit().putString("target_proxy", targetProxy).commit()
        }
    }

    private fun toggleProxy() {
        if (busy) return
        refreshState()
        if (!hasPermission) return
        val endpoint = if (isConnected) null else ProxyEndpoint.parse(targetProxy)
        if (!isConnected && endpoint == null) return
        writeProxy(endpoint) { error ->
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
    }

    private fun saveProxy(input: String, onResult: (String?) -> Unit) {
        if (busy) return
        refreshState()
        if (!hasPermission) {
            onResult("Permission required")
            return
        }
        val endpoint = ProxyEndpoint.parse(input)
        if (endpoint == null) {
            onResult("Enter a valid host:port (port 1–65535)")
            return
        }
        if (isConnected) {
            writeProxy(endpoint, onResult)
        } else if (prefs.edit().putString("target_proxy", endpoint.toString()).commit()) {
            targetProxy = endpoint.toString()
            onResult(null)
        } else {
            onResult("Could not save the proxy address")
        }
    }

    private fun writeProxy(endpoint: ProxyEndpoint?, onResult: (String?) -> Unit) {
        busy = true
        worker.execute {
            val result = proxies.setAndVerify(endpoint)
            uiHandler.post {
                if (isDestroyed) return@post
                busy = false
                val saved = result.isSuccess && (endpoint == null ||
                    prefs.edit().putString("target_proxy", endpoint.toString()).commit())
                refreshState()
                onResult(when {
                    result.isFailure -> result.exceptionOrNull()?.message ?: "Could not apply the proxy"
                    !saved -> "Could not save the proxy address"
                    else -> null
                })
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HomeScreen() {
        var showingSheet by remember { mutableStateOf(false) }
        var sheetInputValue by remember { mutableStateOf(targetProxy) }
        var inputError by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(hasPermission) {
            if (!hasPermission) showingSheet = false
        }

        val isDark = isSystemInDarkTheme()
        val gradientStart by animateColorAsState(
            targetValue = when {
                isConnected && isDark -> GradientConnectedStartDark
                isConnected -> GradientConnectedStartLight
                isDark -> GradientDisconnectedStartDark
                else -> GradientDisconnectedStartLight
            },
            animationSpec = tween(600),
            label = "gradientStart"
        )
        val gradientEnd by animateColorAsState(
            targetValue = when {
                isConnected && isDark -> GradientConnectedEndDark
                isConnected -> GradientConnectedEndLight
                isDark -> GradientDisconnectedEndDark
                else -> GradientDisconnectedEndLight
            },
            animationSpec = tween(600),
            label = "gradientEnd"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top App Bar: Title Only
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Anywhere",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 22.sp
                        ),
                        color = if (isConnected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Power Button
                PowerButton(
                    isConnected = isConnected,
                    enabled = hasPermission && !busy,
                    onClick = ::toggleProxy
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Status Text
                Text(
                    text = when {
                        !hasPermission -> "Permission required"
                        busy -> "Applying…"
                        isConnected -> "Connected"
                        else -> "Not Connected"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (isConnected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                // Target Proxy Selector Card
                Card(
                    enabled = hasPermission && !busy,
                    onClick = {
                        sheetInputValue = targetProxy
                        inputError = null
                        showingSheet = true
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConnected) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cloud),
                            contentDescription = null,
                            tint = if (isConnected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = targetProxy,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (isConnected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_down),
                            contentDescription = null,
                            tint = if (isConnected) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Modal Bottom Sheet
            if (showingSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showingSheet = false },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 36.dp)
                    ) {
                        Text(
                            text = "Proxy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = sheetInputValue,
                            onValueChange = { sheetInputValue = it; inputError = null },
                            enabled = hasPermission && !busy,
                            isError = inputError != null,
                            supportingText = inputError?.let { error -> { Text(error) } },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showingSheet = false },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                enabled = hasPermission && !busy,
                                onClick = {
                                    saveProxy(sheetInputValue) { error ->
                                        inputError = error
                                        if (error == null) showingSheet = false
                                    }
                                },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}
