/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.preferences.core.edit
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import coil3.request.allowHardware
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import moe.ruelz.ruelzmusify.appicon.RestoreLauncherEntryUseCase
import moe.ruelz.ruelzmusify.backup.PendingDatabaseRestore
import moe.ruelz.ruelzmusify.canvas.StartCanvasPolicyUseCase
import moe.ruelz.ruelzmusify.constants.*
import moe.ruelz.ruelzmusify.downloads.DownloadedArtworkRepository
import moe.ruelz.ruelzmusify.extensions.*
import moe.ruelz.ruelzmusify.gatekeeper.GatekeeperResult
import moe.ruelz.ruelzmusify.gatekeeper.RunGatekeeperCheckUseCase
import moe.ruelz.ruelzmusify.innertube.PlaybackAuthState
import moe.ruelz.ruelzmusify.innertube.YouTube
import moe.ruelz.ruelzmusify.innertube.models.YouTubeLocale
import moe.ruelz.ruelzmusify.kugou.KuGou
import moe.ruelz.ruelzmusify.lastfm.LastFM
import moe.ruelz.ruelzmusify.lrcred.LrcRed
import moe.ruelz.ruelzmusify.paxsenix.PaxsenixLyrics
import moe.ruelz.ruelzmusify.playback.stream.ResolveAudioStreamUseCase
import moe.ruelz.ruelzmusify.playback.stream.YoutubeiStreamRepository
import moe.ruelz.ruelzmusify.scrobbling.LastFmServiceConfig
import moe.ruelz.ruelzmusify.storage.StorageFolderKind
import moe.ruelz.ruelzmusify.storage.StorageLocationRepository
import moe.ruelz.ruelzmusify.ui.screens.settings.ThemePalettes
import moe.ruelz.ruelzmusify.ui.theme.ThemeSeedPalette
import moe.ruelz.ruelzmusify.ui.theme.ThemeSeedPaletteCodec
import moe.ruelz.ruelzmusify.utils.PlaylistCoverInterceptor
import moe.ruelz.ruelzmusify.utils.PreferenceStore
import moe.ruelz.ruelzmusify.utils.ProxyUtils
import moe.ruelz.ruelzmusify.utils.YTPlayerUtils
import moe.ruelz.ruelzmusify.utils.clearPlaybackAuthSession
import moe.ruelz.ruelzmusify.utils.clearPlaybackWebAuthSession
import moe.ruelz.ruelzmusify.utils.dataStore
import moe.ruelz.ruelzmusify.utils.get
import moe.ruelz.ruelzmusify.utils.potoken.BotGuardTokenGenerator
import moe.ruelz.ruelzmusify.utils.putAccountSessionIndex
import moe.ruelz.ruelzmusify.utils.reportException
import moe.ruelz.ruelzmusify.utils.toPlaybackAuthState
import okhttp3.Dns
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter
import java.net.Proxy
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.system.exitProcess

@HiltAndroidApp
class App :
    Application(),
    SingletonImageLoader.Factory {
    @Inject
    lateinit var runGatekeeperCheckUseCase: RunGatekeeperCheckUseCase

    @Inject
    lateinit var downloadedArtworkRepository: DownloadedArtworkRepository

    @Inject
    lateinit var playlistCoverInterceptor: PlaylistCoverInterceptor

    @Inject
    lateinit var youtubeiStreamRepository: YoutubeiStreamRepository

    @Inject
    lateinit var resolveAudioStream: ResolveAudioStreamUseCase

    @Inject
    lateinit var startCanvasPolicy: StartCanvasPolicyUseCase

    @Inject
    lateinit var restoreLauncherEntry: RestoreLauncherEntryUseCase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile private var isInitialized = false
    private val didRunImageCacheTrim = AtomicBoolean(false)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (currentProcessName() == packageName) {
            PendingDatabaseRestore.install(this)
        }
    }

    private fun currentProcessName(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            val pid = android.os.Process.myPid()
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            activityManager
                ?.runningAppProcesses
                ?.firstOrNull { it.pid == pid }
                ?.processName
        }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this
        if (currentProcessName()?.endsWith(":crash") == true) {
            Timber.plant(Timber.DebugTree())
            return
        }
        BotGuardTokenGenerator.initialize(this)
        PreferenceStore.start(this)
        Timber.plant(Timber.DebugTree())
        try {
            Timber.plant(
                moe.ruelz.ruelzmusify.utils
                    .GlobalLogTree(),
            )
        } catch (_: Exception) {
        }

        initializeGatekeeper()
        initializeCriticalSync()
        initializeDeferredAsync()
        initializeLauncherEntry()
    }

    private fun initializeLauncherEntry() {
        if (BuildConfig.DEVICE == "automotive") return
        applicationScope.launch(Dispatchers.IO) {
            try {
                restoreLauncherEntry()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Timber.e(error, "Unable to restore launcher entry")
                reportException(error)
            }
        }
    }

    private fun initializeGatekeeper() {
        applicationScope.launch(Dispatchers.IO) {
            while (isActive) {
                val result = runGatekeeperCheckUseCase()
                when (result) {
                    GatekeeperResult.Allowed -> return@launch
                    GatekeeperResult.Unavailable -> Unit
                    is GatekeeperResult.Blocked -> if (!result.retryable) return@launch
                }
                delay(GATEKEEPER_RETRY_INTERVAL_MILLIS)
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        youtubeiStreamRepository.trimMemory(level)
    }

    private fun initializeCriticalSync() {
        startCanvasPolicy.start(applicationScope)
        PaxsenixLyrics.setUserAgent("RuelzMusify", BuildConfig.VERSION_NAME)
        LrcRed.setUserAgent("RuelzMusify", BuildConfig.VERSION_NAME)

        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "")
        YouTube.locale =
            YouTubeLocale(
                gl = locale.country.takeIf { it in CountryCodeToName } ?: "US",
                hl =
                    locale.language.takeIf { it in LanguageCodeToName }
                        ?: languageTag.takeIf { it in LanguageCodeToName }
                        ?: "en",
            )
        if (languageTag == "zh-TW") {
            KuGou.useTraditionalChinese = true
        }
        LastFM.initialize(
            apiKey = BuildConfig.LASTFM_API_KEY,
            secret = BuildConfig.LASTFM_SECRET,
        )
    }

    private fun initializeDeferredAsync() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                youtubeiStreamRepository.preWarm()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Timber.tag("YoutubeiResolver").w(throwable, "youtubei.js runtime prewarm failed")
            }
        }

        applicationScope.launch(Dispatchers.IO) {
            try {
                val prefs = dataStore.data.first()

                prefs[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }?.let { country ->
                    YouTube.locale = YouTube.locale.copy(gl = country)
                }
                prefs[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }?.let { lang ->
                    YouTube.locale = YouTube.locale.copy(hl = lang)
                }

                PaxsenixLyrics.setApiKey(prefs[PaxsenixApiKeyKey].orEmpty())
                LastFmServiceConfig.fromPreferences(prefs).apply(prefs[LastFMSessionKey])

                ProxyUtils.applyYouTubeProxy(
                    enabled = prefs[ProxyEnabledKey] == true,
                    type = prefs[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                    host = prefs[ProxyHostKey],
                    port = prefs[ProxyPortKey],
                    username = prefs[ProxyUsernameKey],
                    password = prefs[ProxyPasswordKey],
                )
                YouTube.streamBypassProxy = YouTube.proxy != null && prefs[StreamBypassProxyKey] == true

                if (prefs[UseLoginForBrowse] != false) {
                    YouTube.useLoginForBrowse = true
                }

                // Apply random theme on startup if enabled
                if (prefs[RandomThemeOnStartupKey] == true) {
                    val randomPalette = ThemePalettes.generateRandomPalette()
                    val seedPalette =
                        ThemeSeedPalette(
                            primary = randomPalette.primary,
                            secondary = randomPalette.secondary,
                            tertiary = randomPalette.tertiary,
                            neutral = randomPalette.neutral,
                        )
                    val encodedPalette = ThemeSeedPaletteCodec.encodeForPreference(seedPalette, "Random")
                    dataStore.edit { settings ->
                        settings[CustomThemeColorKey] = encodedPalette
                    }
                }

                isInitialized = true
            } catch (e: Exception) {
                Timber.e(e, "Error during deferred initialization")
                reportException(e)
            }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map {
                    Triple(
                        it[EnableDnsOverHttpsKey] ?: false,
                        it[DnsOverHttpsProviderKey] ?: "Cloudflare",
                        it[CustomDnsUrlKey] ?: "https://",
                    )
                }.distinctUntilChanged()
                .collect { (enabled, provider, customUrl) ->
                    if (enabled) {
                        val dnsProviderUrls =
                            mapOf(
                                "Google" to "https://dns.google/dns-query",
                                "Cloudflare" to "https://cloudflare-dns.com/dns-query",
                                "AdGuard" to "https://dns.adguard.com/dns-query",
                                "Quad9" to "https://dns.quad9.net/dns-query",
                            )
                        val url = if (provider == "Custom") customUrl else dnsProviderUrls[provider]
                        if (!url.isNullOrBlank() && url.startsWith("https://")) {
                            runCatching {
                                YouTube.dns = YouTube.createDnsOverHttps(url)
                            }
                        } else {
                            YouTube.dns = Dns.SYSTEM
                        }
                    } else {
                        YouTube.dns = Dns.SYSTEM
                    }
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it.toPlaybackAuthState() }
                .distinctUntilChanged()
                .collect { authState ->
                    YouTube.authState = authState
                    resolveAudioStream.clear()
                    YTPlayerUtils.clearPlaybackAuthCaches()
                    BotGuardTokenGenerator.invalidateAll()
                    youtubeiStreamRepository.invalidateSessions()
                    YTPlayerUtils.preWarmYoutubeiPoTokens(authState)
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it.toPlaybackAuthState().visitorData }
                .distinctUntilChanged()
                .collect { visitorData ->
                    if (!visitorData.isNullOrBlank()) return@collect
                    YouTube
                        .visitorData()
                        .onFailure {
                            reportException(it)
                        }.getOrNull()
                        ?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        }
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { preferences ->
                    preferences
                        .toPlaybackAuthState()
                        .takeIf { it.hasLoginCookie && it.accountIndex == null }
                        ?.cookie
                }.distinctUntilChanged()
                .collectLatest { cookie ->
                    if (cookie == null) return@collectLatest
                    val accountIndex =
                        YouTube
                            .accountSessionIndex(PlaybackAuthState(cookie = cookie))
                            .onFailure { failure ->
                                if (failure is CancellationException) throw failure
                                reportException(failure)
                            }.getOrNull()
                            ?: return@collectLatest
                    dataStore.edit { settings ->
                        val persisted = settings.toPlaybackAuthState()
                        if (persisted.cookie == cookie && persisted.accountIndex == null) {
                            settings.putAccountSessionIndex(accountIndex)
                        }
                    }
                }
        }

        if (BuildConfig.DEVICE != "automotive") {
            try {
                Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                    try {
                        val sw = StringWriter()
                        val pw = PrintWriter(sw)
                        throwable.printStackTrace(pw)
                        val stack = sw.toString()

                        val intent =
                            Intent(this@App, DebugActivity::class.java).apply {
                                putExtra(DebugActivity.EXTRA_STACK_TRACE, stack)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                        startActivity(intent)
                        try {
                            Thread.sleep(100)
                        } catch (_: InterruptedException) {
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    } finally {
                        android.os.Process.killProcess(android.os.Process.myPid())
                        exitProcess(2)
                    }
                }
            } catch (e: Exception) {
                reportException(e)
            }
        }
        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { prefs ->
                    LastFmServiceConfig.fromPreferences(prefs) to prefs[LastFMSessionKey]
                }.distinctUntilChanged()
                .collect { (serviceConfig, sessionKey) ->
                    serviceConfig.apply(sessionKey)
                }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val smartTrimmer = dataStore[SmartTrimmerKey] ?: false
        val imageCacheConfig = resolveImageDiskCacheConfig(dataStore[MaxImageCacheSizeKey])

        val diskCache =
            DiskCache
                .Builder()
                .directory(StorageLocationRepository.cacheDirectory(this, StorageFolderKind.IMAGE_CACHE))
                .maxSizeBytes(imageCacheConfig.maxSizeBytes)
                .build()

        if (smartTrimmer && imageCacheConfig.policy == CachePolicy.ENABLED && didRunImageCacheTrim.compareAndSet(false, true)) {
            applicationScope.launch(Dispatchers.IO) { trimImageDiskCache(diskCache) }
        }

        return ImageLoader
            .Builder(this)
            .crossfade(true)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            .diskCache(diskCache)
            .diskCachePolicy(imageCacheConfig.policy)
            .components {
                add(playlistCoverInterceptor)
                add(downloadedArtworkRepository.coilMapper())
            }
            .build()
    }

    private fun trimImageDiskCache(diskCache: DiskCache) {
        try {
            val limitBytes = diskCache.maxSize
            if (limitBytes <= 0L || limitBytes == Long.MAX_VALUE) return

            val dir = java.io.File(diskCache.directory.toString())
            if (!dir.exists()) return

            val files =
                dir
                    .walkTopDown()
                    .filter { it.isFile }
                    .sortedBy { it.lastModified() }
                    .toList()
            var currentSize = files.sumOf { it.length() }
            if (currentSize <= limitBytes) return

            for (file in files) {
                if (currentSize <= limitBytes) break
                val size = file.length()
                if (runCatching { file.delete() }.getOrDefault(false)) currentSize -= size
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val GATEKEEPER_RETRY_INTERVAL_MILLIS = 30_000L

        lateinit var instance: App
            private set

        fun forgetAccount(
            context: Context,
            clearWebAuthSession: Boolean = true,
        ) {
            if (clearWebAuthSession) {
                clearPlaybackWebAuthSession(context)
            }
            CoroutineScope(Dispatchers.IO).launch {
                context.dataStore.edit { settings ->
                    settings.clearPlaybackAuthSession()
                }
            }
        }
    }
}

internal data class ImageDiskCacheConfig(
    val policy: CachePolicy,
    val maxSizeBytes: Long,
)

internal fun resolveImageDiskCacheConfig(maxImageCacheSizeMb: Int?): ImageDiskCacheConfig {
    val sizeMb = maxImageCacheSizeMb ?: 512
    if (sizeMb == 0) return ImageDiskCacheConfig(policy = CachePolicy.DISABLED, maxSizeBytes = 1L)
    if (sizeMb < 0) return ImageDiskCacheConfig(policy = CachePolicy.ENABLED, maxSizeBytes = Long.MAX_VALUE)
    val bytesPerMb = 1024L * 1024L
    val safeSizeMb = sizeMb.toLong().coerceAtMost(Long.MAX_VALUE / bytesPerMb)
    return ImageDiskCacheConfig(policy = CachePolicy.ENABLED, maxSizeBytes = safeSizeMb * bytesPerMb)
}
