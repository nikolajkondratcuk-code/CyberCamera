package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.*
import com.example.data.AppDatabase
import com.example.data.RecordedClip
import com.example.data.RecordedClipRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecordedClipRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RecordedClipRepository(database.recordedClipDao())
    }

    // Clip History StateFlow
    val clipHistory: StateFlow<List<RecordedClip>> = repository.allClips
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Cloud Collaborative Chat State (Blackmagic style)
    private val _isChatVisible = MutableStateFlow(false)
    val isChatVisible: StateFlow<Boolean> = _isChatVisible.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "Sarah",
                role = "DIRECTOR",
                text = "Welcome to the cloud production stream! We're shooting Scene 4, 'Cyber Entrance'. Make sure the exposure is set nicely.",
                timestamp = "10:14 PM",
                avatarColor = 0xFFFB7185L
            ),
            ChatMessage(
                sender = "Damian",
                role = "GAFFER",
                text = "Ambient lighting is dialled to 45% matching our background neon strip. ISO 400 seems sharp on the secondary monitor.",
                timestamp = "10:15 PM",
                avatarColor = 0xFFFBBF24L
            ),
            ChatMessage(
                sender = "Ava",
                role = "SCRIPT",
                text = "Slate logged: Scene 4, Take 3. Let's get a clean frame sync this time.",
                timestamp = "10:16 PM",
                avatarColor = 0xFF38BDF8L
            ),
            ChatMessage(
                sender = "Marcus",
                role = "BOOM",
                text = "Audio levels check: Stereo mic reads -12dB peak. Quiet on set!",
                timestamp = "10:17 PM",
                avatarColor = 0xFF34D399L
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    fun toggleChat() {
        _isChatVisible.value = !_isChatVisible.value
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val curTime = sdf.format(Date())
        val myMessage = ChatMessage(
            sender = "You",
            role = "DP",
            text = text,
            timestamp = curTime,
            isMe = true,
            avatarColor = 0xFF22D3EEL
        )
        _chatMessages.value = _chatMessages.value + myMessage

        // Automated responses simulation to make it feel extremely interactive!
        viewModelScope.launch {
            delay(1500)
            val reply = getSimulatedResponse(text)
            _chatMessages.value = _chatMessages.value + reply
        }
    }

    private fun getSimulatedResponse(msg: String): ChatMessage {
        val lower = msg.lowercase()
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val curTime = sdf.format(Date())

        return when {
            lower.contains("roll") || lower.contains("start") || lower.contains("rec") || lower.contains("🎬") || lower.contains("запуск") || lower.contains("снимаем") -> {
                ChatMessage(
                    sender = "Sarah",
                    role = "DIRECTOR",
                    text = "Speed! Action! 🎬 Make sure your focus tracking is locked.",
                    timestamp = curTime,
                    avatarColor = 0xFFFB7185L
                )
            }
            lower.contains("wrap") || lower.contains("снято") || lower.contains("стоп") -> {
                ChatMessage(
                    sender = "Ava",
                    role = "SCRIPT",
                    text = "Wonderful. Logging Take 3 as complete. Prepping logs for Scene 5.",
                    timestamp = curTime,
                    avatarColor = 0xFF38BDF8L
                )
            }
            lower.contains("light") || lower.contains("exposure") || lower.contains("dark") || lower.contains("💡") || lower.contains("свет") || lower.contains("темно") -> {
                ChatMessage(
                    sender = "Damian",
                    role = "GAFFER",
                    text = "Adjusting the fill light now. How does the live histogram look on your end?",
                    timestamp = curTime,
                    avatarColor = 0xFFFBBF24L
                )
            }
            lower.contains("audio") || lower.contains("sound") || lower.contains("mic") || lower.contains("🎤") || lower.contains("звук") || lower.contains("микро") -> {
                ChatMessage(
                    sender = "Marcus",
                    role = "BOOM",
                    text = "Clear and clean! Sound continues to roll on our external XLR channels.",
                    timestamp = curTime,
                    avatarColor = 0xFF34D399L
                )
            }
            lower.contains("zebra") || lower.contains("wave") || lower.contains("peak") || lower.contains("luts") || lower.contains("зебра") || lower.contains("гисто") -> {
                ChatMessage(
                    sender = "Sarah",
                    role = "DIRECTOR",
                    text = "Understood. The telemetry overlays are looking incredibly crisp from our remote portal.",
                    timestamp = curTime,
                    avatarColor = 0xFFFB7185L
                )
            }
            else -> {
                val responses = listOf(
                    ChatMessage("Sarah", "DIRECTOR", "Excellent work, crew. Let's make sure the composition is cinematic.", curTime, false, 0xFFFB7185L),
                    ChatMessage("Ava", "SCRIPT", "Received. Adding this to the project metadata.", curTime, false, 0xFF38BDF8L),
                    ChatMessage("Damian", "GAFFER", "Got it. Let me know if that changes the lighting requirements.", curTime, false, 0xFFFBBF24L),
                    ChatMessage("Marcus", "BOOM", "Understood. Maintaining channel levels.", curTime, false, 0xFF34D399L)
                )
                responses.random()
            }
        }
    }

    // Manual Settings
    private val _shutterSpeed = MutableStateFlow("1/48")
    val shutterSpeed: StateFlow<String> = _shutterSpeed.asStateFlow()

    private val _iso = MutableStateFlow(400)
    val iso: StateFlow<Int> = _iso.asStateFlow()

    private val _whiteBalanceTemp = MutableStateFlow(5600) // Kelvin
    val whiteBalanceTemp: StateFlow<Int> = _whiteBalanceTemp.asStateFlow()

    private val _tint = MutableStateFlow(0) // -100 to +100
    val tint: StateFlow<Int> = _tint.asStateFlow()

    private val _focalDistance = MutableStateFlow(0.0f) // 0.0f = Continuous Auto Focus (CAF), 0.1f..1.0f = Manual Focus
    val focalDistance: StateFlow<Float> = _focalDistance.asStateFlow()

    private val _ev = MutableStateFlow(0.0f) // Exposure Value -3.0 to +3.0
    val ev: StateFlow<Float> = _ev.asStateFlow()

    // Professional Tools Toggles
    private val _isFocusPeaking = MutableStateFlow(false)
    val isFocusPeaking: StateFlow<Boolean> = _isFocusPeaking.asStateFlow()

    private val _isFalseColor = MutableStateFlow(false)
    val isFalseColor: StateFlow<Boolean> = _isFalseColor.asStateFlow()

    private val _isZebra = MutableStateFlow(false)
    val isZebra: StateFlow<Boolean> = _isZebra.asStateFlow()

    private val _framingGrid = MutableStateFlow(FramingGridStyle.NONE)
    val framingGrid: StateFlow<FramingGridStyle> = _framingGrid.asStateFlow()

    private val _selectedLUT = MutableStateFlow(CyberLUT.NEUTRAL)
    val selectedLUT: StateFlow<CyberLUT> = _selectedLUT.asStateFlow()

    // Recording Specs
    private val _codec = MutableStateFlow(VideoCodec.H264)
    val codec: StateFlow<VideoCodec> = _codec.asStateFlow()

    private val _resolution = MutableStateFlow(VideoResolution.FHD_1080P)
    val resolution: StateFlow<VideoResolution> = _resolution.asStateFlow()

    private val _frameRate = MutableStateFlow(FrameRate.FPS_24)
    val frameRate: StateFlow<FrameRate> = _frameRate.asStateFlow()

    // Audio Controls
    private val _audioSource = MutableStateFlow(AudioSource.STEREO_MIC)
    val audioSource: StateFlow<AudioSource> = _audioSource.asStateFlow()

    private val _audioGainDb = MutableStateFlow(0f) // -12dB to 12dB
    val audioGainDb: StateFlow<Float> = _audioGainDb.asStateFlow()

    // Telemetry and Recording State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _selectedMode = MutableStateFlow(CameraMode.PHOTO)
    val selectedMode: StateFlow<CameraMode> = _selectedMode.asStateFlow()

    private val _selectedLens = MutableStateFlow(CameraLens.BACK)
    val selectedLens: StateFlow<CameraLens> = _selectedLens.asStateFlow()

    private val _zoomLevel = MutableStateFlow(1.0f) // 0.5f, 1.0f, 2.0f, 5.0f
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    fun setCameraLens(lens: CameraLens) {
        if (!_isRecording.value && !_isCapturingPhoto.value) {
            _selectedLens.value = lens
        }
    }

    fun cycleCameraLens() {
        if (!_isRecording.value && !_isCapturingPhoto.value) {
            _selectedLens.value = when (_selectedLens.value) {
                CameraLens.BACK -> CameraLens.FRONT
                CameraLens.FRONT -> CameraLens.DUAL
                CameraLens.DUAL -> CameraLens.BACK
            }
        }
    }

    fun setCameraMode(mode: CameraMode) {
        if (!_isRecording.value && !_isCapturingPhoto.value) {
            _selectedMode.value = mode
        }
    }

    // Photo capturing state
    private val _isCapturingPhoto = MutableStateFlow(false)
    val isCapturingPhoto: StateFlow<Boolean> = _isCapturingPhoto.asStateFlow()

    private val _photoCaptureProgress = MutableStateFlow(0f)
    val photoCaptureProgress: StateFlow<Float> = _photoCaptureProgress.asStateFlow()

    private val _photoCaptureStatus = MutableStateFlow("")
    val photoCaptureStatus: StateFlow<String> = _photoCaptureStatus.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private val _batteryLevel = MutableStateFlow(0.88f)
    val batteryLevel: StateFlow<Float> = _batteryLevel.asStateFlow()

    private val _simulationMode = MutableStateFlow(true) // Default to cyber simulator because it responds beautiful
    val simulationMode: StateFlow<Boolean> = _simulationMode.asStateFlow()

    // Dynamic calculated storage limit
    private val _availableStorageBytes = MutableStateFlow(48_320_000_000L) // 48.3 GB
    val availableStorageBytes: StateFlow<Long> = _availableStorageBytes.asStateFlow()

    private var recordingJob: Job? = null
    private var telemetryJob: Job? = null

    init {
        // Start telemetry simulation
        startTelemetrySimulation()
    }

    private fun startTelemetrySimulation() {
        telemetryJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                // Battery decays very slowly
                _batteryLevel.update { (it - 0.001f).coerceAtLeast(0.01f) }
                // Storage fluctuates slowly at random unless recording
                if (!_isRecording.value) {
                    _availableStorageBytes.update { it + (-50_000..50_000).random() }
                }
            }
        }
    }

    // Setters for manual settings
    fun setShutterSpeed(speed: String) { _shutterSpeed.value = speed }
    fun setIso(value: Int) { _iso.value = value }
    fun setWhiteBalanceTemp(kelvin: Int) { _whiteBalanceTemp.value = kelvin }
    fun setTint(value: Int) { _tint.value = value }
    fun setFocalDistance(dist: Float) { _focalDistance.value = dist }
    fun setEv(value: Float) { _ev.value = value }
    fun setZoomLevel(level: Float) { _zoomLevel.value = level }

    // Toggle monitors
    fun toggleFocusPeaking() { _isFocusPeaking.value = !_isFocusPeaking.value }
    fun toggleFalseColor() { _isFalseColor.value = !_isFalseColor.value }
    fun toggleZebra() { _isZebra.value = !_isZebra.value }
    fun setFramingGrid(style: FramingGridStyle) { _framingGrid.value = style }
    fun setLUT(lut: CyberLUT) { _selectedLUT.value = lut }

    // Video recording spec adjustments
    fun setCodec(vCodec: VideoCodec) { if (!_isRecording.value) _codec.value = vCodec }
    fun setResolution(vRes: VideoResolution) { if (!_isRecording.value) _resolution.value = vRes }
    fun setFrameRate(vFps: FrameRate) { if (!_isRecording.value) _frameRate.value = vFps }

    // Audio properties
    fun setAudioSource(source: AudioSource) { _audioSource.value = source }
    fun setAudioGain(db: Float) { _audioGainDb.value = db }

    fun toggleSimulationMode() { _simulationMode.value = !_simulationMode.value }
    fun setSimulationMode(enabled: Boolean) { _simulationMode.value = enabled }

    // Action: Start/Stop Recording
    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        _isRecording.value = true
        _recordingSeconds.value = 0
        recordingJob = viewModelScope.launch {
            val format = SimpleDateFormat("HHmmss", Locale.getDefault())
            val dateStr = format.format(Date())
            while (_isRecording.value) {
                delay(1000)
                _recordingSeconds.update { it + 1 }
                // Deduct storage based on selected Codec & Resolution & Frame rate bytes consumption
                val consumedPerSec = calculateApproximateBytesPerSec()
                _availableStorageBytes.update { (it - consumedPerSec).coerceAtLeast(0L) }
            }
        }
    }

    private fun stopRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        val elapsed = _recordingSeconds.value
        if (elapsed > 0) {
            saveRecordedClip(elapsed)
        }
    }

    private fun saveRecordedClip(durationSeconds: Int) {
        viewModelScope.launch {
            val count = (clipHistory.value.size + 1)
            val extension = if (_selectedMode.value == CameraMode.CINEMATIC) "mov" else _codec.value.extension
            val fileIndex = String.format("%03d", count)
            val lensTag = when (_selectedLens.value) {
                CameraLens.FRONT -> "_SELFIE"
                CameraLens.DUAL -> "_DUAL"
                else -> ""
            }
            
            val filename = when (_selectedMode.value) {
                CameraMode.TIME_LAPSE -> "TL_FUSION_${fileIndex}${lensTag}.mp4"
                CameraMode.SLO_MO -> "SLOMO_${fileIndex}${lensTag}.mp4"
                CameraMode.CINEMATIC -> "CINEMATIC_${fileIndex}${lensTag}.mov"
                else -> {
                    val resName = when (_resolution.value) {
                        VideoResolution.UHD_4K -> "4K"
                        VideoResolution.FHD_1080P -> "1080"
                        VideoResolution.HD_720P -> "720"
                    }
                    "CYBER_${fileIndex}${lensTag}_${resName}p_${_frameRate.value.fps}.$extension"
                }
            }

            val codecLabel = when (_selectedLens.value) {
                CameraLens.DUAL -> "Dual Lens Split Streamer"
                else -> when (_selectedMode.value) {
                    CameraMode.TIME_LAPSE -> "Time Plus Interval Compiler"
                    CameraMode.SLO_MO -> "Slo-Mo High FPS Capture (240fps)"
                    CameraMode.CINEMATIC -> "ProRes Cinematic (Depth-fused)"
                    else -> _codec.value.label
                }
            }

            val resolutionLabel = when (_selectedLens.value) {
                CameraLens.DUAL -> "Dual Stream (2x FHD 1920x1080)"
                else -> when (_selectedMode.value) {
                    CameraMode.TIME_LAPSE -> "4K UHD Time-lapse"
                    CameraMode.SLO_MO -> "1080p Slo-Mo (120 FPS)"
                    CameraMode.CINEMATIC -> "4K Cinematic Dolby Master"
                    else -> when (_resolution.value) {
                        VideoResolution.UHD_4K -> "3840x2160"
                        VideoResolution.FHD_1080P -> "1920x1080"
                        VideoResolution.HD_720P -> "1280x720"
                    }
                }
            }

            val fpsValue = when (_selectedMode.value) {
                CameraMode.TIME_LAPSE -> 30
                CameraMode.SLO_MO -> 120
                CameraMode.CINEMATIC -> 24
                else -> _frameRate.value.fps
            }

            val totalBytes = calculateApproximateBytesPerSec() * durationSeconds

            val newClip = RecordedClip(
                filename = filename,
                durationSeconds = durationSeconds,
                timestamp = System.currentTimeMillis(),
                sizeBytes = totalBytes,
                resolution = resolutionLabel,
                frameRate = fpsValue,
                codec = codecLabel,
                isoUsed = _iso.value,
                shutterUsed = _shutterSpeed.value,
                whiteBalanceUsed = _whiteBalanceTemp.value,
                tintUsed = _tint.value,
                lutUsed = _selectedLUT.value.label,
                focalDistUsed = _focalDistance.value,
                isPhoto = false,
                cameraMode = _selectedMode.value.name
            )
            repository.insert(newClip)
        }
    }

    fun captureFusedPhoto() {
        if (_isCapturingPhoto.value || _isRecording.value) return

        viewModelScope.launch {
            _isCapturingPhoto.value = true
            _photoCaptureProgress.value = 0.0f
            
            val statusSteps = if (_selectedLens.value == CameraLens.DUAL) {
                listOf(
                    "INITIALIZING SYSTEM DUAL-SENSOR STREAM...",
                    "ACQUIRING FRONT & REAR ALIGNED PATHS...",
                    "EXPOSING FRONT DEPTH-FACE MATRIX...",
                    "EXPOSING REAR WIDE-ANGLE DEEP RAW...",
                    "FUSING FRONT AND BACK IMAGE PLANES...",
                    "APPLYING SPLIT-VIEW COMPILER...",
                    "SAVING DUAL FUSION HDR MASTER..."
                )
            } else {
                when (_selectedMode.value) {
                    CameraMode.PORTRAIT -> listOf(
                        "EXTRACTING DEPTH SCENE HEURISTICS...",
                        "SEGMENTING FOREGROUND DEPTH MAP...",
                        "CALCULATING BILATERAL EDGE MASKS...",
                        "COMPILING HIGH-FIDELITY PORTRAIT DEFOCUS...",
                        "APPLYING SHIFTED STUDIO GLASS BLUR...",
                        "FUSING FOCUS DEPTH PLANE COALITION...",
                        "FINE TUNING SKIN SURFACE COMPRESSION...",
                        "CONSOLIDATING 48MP PORTRAIT MASTER..."
                    )
                    CameraMode.NIGHT -> listOf(
                        "STABILIZING DIGITAL GYROSCOPE...",
                        "APPLYING NEURAL SHUTTER INTEGRATION...",
                        "CONSOLIDATING MULTI-FRAME GAIN...",
                        "ACCUMULATING AMBIENT LOW-LIGHT LUMANCE...",
                        "COMBINING 9 RAW NOISE PROFILE SAMPLES...",
                        "RESOLVING SHADOW HEADROOM DENSITY...",
                        "APPLYING SHADOW NOISE COALESCENCE...",
                        "CONSOLIDATING 48MP NIGHT HDR MASTER..."
                    )
                    CameraMode.PANORAMA -> listOf(
                        "INITIATING HORIZONTAL PAN SWEEP...",
                        "SWEEPING: COMPILING FRAME 1/4...",
                        "REAL-TIME GYROSCOPE CALIBRATION...",
                        "SWEEPING: COMPILING FRAME 2/4...",
                        "LOCATING COHERENT SPATIAL DESCRIPTORS...",
                        "SWEEPING: COMPILING FRAME 3/4...",
                        "SWEEPING: COMPILING FRAME 4/4...",
                        "STITCHING 63MP ULTRA-WIDE MASTER COALITION..."
                    )
                    else -> listOf(
                        "FRAME 1/9 EXPOSURE FUSED...",
                        "FRAME 2/9 NOISE PROFILE COALITION...",
                        "FRAME 3/9 CHROMATIC PARALLAX ADJUSTMENT...",
                        "FRAME 4/9 MULTI-BAND BILATERAL GRAPH...",
                        "FRAME 5/9 SUB-PIXEL EDGE ALIGNMENT...",
                        "FRAME 6/9 SHADOW SPECTRUM EXTRAPOLATION...",
                        "FRAME 7/9 HIGHLIGHT AREA PRESERVATION...",
                        "FRAME 8/9 HEVC COMPRESSION QUANTIZATION...",
                        "FRAME 9/9 IPHONE DEEP HDR FUSION ACQUIRED...",
                        "CONSOLIDATING 48MP FUSED MASTER HDR IMAGE..."
                    )
                }
            }

            val stepCount = statusSteps.size
            for (step in 1..stepCount) {
                _photoCaptureStatus.value = statusSteps[step - 1]
                _photoCaptureProgress.value = (step - 1) / stepCount.toFloat()
                // Calibrated to exactly 111ms per RAW frame to capture exactly 9 frames per second
                val frameDelay = if (_selectedMode.value == CameraMode.NIGHT) 400L else 111L
                delay(frameDelay)
            }

            _photoCaptureStatus.value = "RECORDING TO CACHE MODULE..."
            _photoCaptureProgress.value = 0.95f
            delay(400) // post-processing synthesis phase

            saveFusedPhotoRecord()

            _photoCaptureProgress.value = 1.0f
            _isCapturingPhoto.value = false
            _photoCaptureStatus.value = ""
        }
    }

    private suspend fun saveFusedPhotoRecord() {
        val count = (clipHistory.value.count { it.isPhoto } + 1)
        val fileIndex = String.format("%03d", count)
        val lensTag = when (_selectedLens.value) {
            CameraLens.FRONT -> "_SELFIE"
            CameraLens.DUAL -> "_DUAL"
            else -> ""
        }
        
        val filename = when (_selectedMode.value) {
            CameraMode.PORTRAIT -> "IMG_PORTRAIT_${fileIndex}${lensTag}.heic"
            CameraMode.NIGHT -> "IMG_NIGHT_${fileIndex}${lensTag}.heic"
            CameraMode.PANORAMA -> "IMG_PANO_${fileIndex}${lensTag}.heic"
            else -> "IMG_HDR_FUSION_${fileIndex}${lensTag}.heic"
        }
        
        val resolutionName = when (_selectedLens.value) {
            CameraLens.DUAL -> "2x 12MP Front + Back Dual Canvas (4032x3024)"
            else -> when (_selectedMode.value) {
                CameraMode.PORTRAIT -> "48MP Apple Portrait Depth-RAW (8064x6048)"
                CameraMode.NIGHT -> "48MP Night Mode HDR Master (8064x6048)"
                CameraMode.PANORAMA -> "63MP Panoramic Stitch (15000x4200)"
                else -> "48MP Apple ProRAW (8064x6048)"
            }
        }

        val codecName = when (_selectedLens.value) {
            CameraLens.DUAL -> "Dual Stream Master Fusion"
            else -> when (_selectedMode.value) {
                CameraMode.PORTRAIT -> "Apple Depth Engine (A17 Pro)"
                CameraMode.NIGHT -> "Night Sight Neural Integration"
                CameraMode.PANORAMA -> "iPhone Horizontal Sweep Stitch"
                else -> "Pro Image Stack (A17 Deep)"
            }
        }
        
        // Deduct storage
        val photoSize = when (_selectedMode.value) {
            CameraMode.PANORAMA -> 24_800_000L // Panoramic is larger
            else -> 12_400_000L // ~12.4 MB
        }
        _availableStorageBytes.update { (it - photoSize).coerceAtLeast(0L) }

        val newPhoto = RecordedClip(
            filename = filename,
            durationSeconds = 0,
            timestamp = System.currentTimeMillis(),
            sizeBytes = photoSize,
            resolution = resolutionName,
            frameRate = 0,
            codec = codecName,
            isoUsed = _iso.value,
            shutterUsed = _shutterSpeed.value,
            whiteBalanceUsed = _whiteBalanceTemp.value,
            tintUsed = _tint.value,
            lutUsed = _selectedLUT.value.label,
            focalDistUsed = _focalDistance.value,
            isPhoto = true,
            cameraMode = _selectedMode.value.name
        )
        repository.insert(newPhoto)
    }

    fun deleteClip(clip: RecordedClip) {
        viewModelScope.launch {
            repository.delete(clip)
        }
    }

    fun clearAllClips() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    private fun calculateApproximateBytesPerSec(): Long {
        // Approximate Mbps per resolution & codec
        // ProRes ~ 220 Mbps (27.5 MB/s)
        // H265 ~ 25 Mbps (3.1 MB/s)
        // H264 ~ 40 Mbps (5.0 MB/s)
        val codecFactor = when (_codec.value) {
            VideoCodec.PRORES -> 8.0f
            VideoCodec.HEVC -> 0.7f
            VideoCodec.H264 -> 1.0f
        }
        val resFactor = _resolution.value.bitrateMultiplier
        val fpsFactor = when (_frameRate.value) {
            FrameRate.FPS_24 -> 1.0f
            FrameRate.FPS_30 -> 1.25f
            FrameRate.FPS_60 -> 2.2f
            FrameRate.FPS_120 -> 4.0f
        }
        val baseMegaBytes = 4.0f * codecFactor * resFactor * fpsFactor
        return (baseMegaBytes * 1024 * 1024).toLong()
    }

    override fun onCleared() {
        super.onCleared()
        telemetryJob?.cancel()
        recordingJob?.cancel()
    }
}
