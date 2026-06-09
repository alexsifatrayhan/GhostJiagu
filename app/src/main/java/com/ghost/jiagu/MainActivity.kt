package com.ghost.jiagu

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghost.jiagu.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Color constant shortcuts
val CyberDarkBase = Color(0xFF0D0D15)
val CyberCyan = Color(0xFF00F0FF)
val CyberPurple = Color(0xFFBD00FF)
val CyberCardBg = Color(0x1BFFFFFF)  // Translucent white bg-white/10 equivalent
val CyberBorder = Color(0x33FFFFFF)  // thin white/20 equivalent
val CyberGrayText = Color(0xFF8E8E9F)
val CyberGreen = Color(0xFF00FF66)
val CyberRed = Color(0xFFFF0D55)
val CyberYellow = Color(0xFFFFD600)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .background(CyberDarkBase)
        ) { innerPadding ->
          Surface(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
            color = Color.Transparent
          ) {
            JiaguSuiteApp()
          }
        }
      }
    }
  }
}

// Data Classes & States
data class FileState(
  val name: String,
  val size: String,
  val extension: String, // "apk", "apks", "xapk"
  val detectedSignature: String = "Unknown / Unscanned",
  val hasShell: Boolean = false,
  val integrityScore: Float = 100f,
  val shellType: String = "None"
)

enum class TabItem(val title: String, val icon: ImageVector) {
  PROTECTOR("Protector", Icons.Default.Shield),
  REMOVER("Protection Remover", Icons.Default.Terminal),
  DECOMPILER("APK Decompiler", Icons.Default.Code),
  ANTI_SPLIT("Anti-Split", Icons.Default.Merge),
  ARCH_CONTRACTS("TS SDK Contract", Icons.Default.SettingsEthernet)
}

@Composable
fun JiaguSuiteApp() {
  var activeTab by remember { mutableStateOf(TabItem.PROTECTOR) }
  var uploadedFile by remember { mutableStateOf<FileState?>(null) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val uriHandler = LocalUriHandler.current

  // Ambient scrolling cyberpunk glows drawn behind the glassmorphism layout
  val transition = rememberInfiniteTransition(label = "cyber_glow")
  val glowOffsetX by transition.animateFloat(
    initialValue = -300f,
    targetValue = 1200f,
    animationSpec = infiniteRepeatable(
      animation = tween(8000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow_x"
  )
  val glowOffsetY by transition.animateFloat(
    initialValue = -200f,
    targetValue = 1600f,
    animationSpec = infiniteRepeatable(
      animation = tween(12000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow_y"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .drawBehind {
        // Draw cyber blobs in background
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(CyberCyan.copy(alpha = 0.2f), Color.Transparent),
            center = Offset(glowOffsetX, glowOffsetY),
            radius = 600f
          ),
          radius = 600f,
          center = Offset(glowOffsetX, glowOffsetY)
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(CyberPurple.copy(alpha = 0.25f), Color.Transparent),
            center = Offset(size.width - glowOffsetX, size.height - glowOffsetY),
            radius = 700f
          ),
          radius = 700f,
          center = Offset(size.width - glowOffsetX, size.height - glowOffsetY)
        )
      }
      .background(CyberDarkBase.copy(alpha = 0.85f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      // Header Section
      HeaderSection()

      Spacer(modifier = Modifier.height(16.dp))

      // Tab Navigation Row
      TabNavigation(activeTab = activeTab, onTabSelected = { activeTab = it })

      Spacer(modifier = Modifier.height(16.dp))

      // File Status HUD Panel
      FileHUDPanel(
        fileState = uploadedFile,
        onFileSelect = { name, size, ext ->
          // Auto detect properties
          val (shell, nameSig, integrity) = when {
            name.contains("jiagu", ignoreCase = true) || name.contains("360") ->
              Triple(true, "360加固 (360 Jiagu VM Shell v4.1.8)", 18.5f)
            name.contains("pairip", ignoreCase = true) ->
              Triple(true, "PairIP Anti-Reverse Packer Hook", 12.0f)
            name.contains("google", ignoreCase = true) || name.contains("play") ->
              Triple(true, "Google/Play Integrity Stub Loader v2.10", 35.0f)
            name.contains("arm", ignoreCase = true) || name.contains("secshell") ->
              Triple(true, "ARM SecShell Native Code Packer", 15.0f)
            else ->
              Triple(false, "No Protective Shell Detected", 100f)
          }

          uploadedFile = FileState(
            name = name,
            size = size,
            extension = ext,
            detectedSignature = nameSig,
            hasShell = shell,
            integrityScore = integrity,
            shellType = nameSig.substringBefore(" ")
          )
        },
        onReset = {
          uploadedFile = null
        }
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Multi-Pane Display Content Based on Selected Tab
      Crossfade(targetState = activeTab, label = "tab_switch") { currentTab ->
        when (currentTab) {
          TabItem.PROTECTOR -> ProtectorTab(uploadedFile)
          TabItem.REMOVER -> UnpackerTab(uploadedFile)
          TabItem.DECOMPILER -> DecompilerTab(uploadedFile)
          TabItem.ANTI_SPLIT -> AntiSplitTab()
          TabItem.ARCH_CONTRACTS -> ArchitectureContractsTab()
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      // Mandatory Branding Footer
      BrandingFooter(onFooterClick = {
        try {
          val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://alexsifatrayhan.github.io/about-me/"))
          context.startActivity(browserIntent)
        } catch (_: Exception) {
          uriHandler.openUri("https://alexsifatrayhan.github.io/about-me/")
        }
      })
    }
  }
}

@Composable
fun HeaderSection() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
      .background(CyberCardBg)
      .padding(16.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Brush.linearGradient(listOf(CyberCyan, CyberPurple)))
          .padding(2.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberDarkBase),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Code,
            contentDescription = "Logo",
            tint = CyberCyan,
            modifier = Modifier.size(24.dp)
          )
        }
      }

      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "GHOST",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = CyberCyan
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "JIAGU SUITE",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            fontSize = 18.sp,
            color = CyberPurple
          )
        }
        Text(
          text = "Premium APK Packer, Automated Unpacker & DEX Modding Space",
          fontSize = 12.sp,
          color = CyberGrayText
        )
      }
    }
  }
}

@Composable
fun TabNavigation(activeTab: TabItem, onTabSelected: (TabItem) -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF13131A))
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    TabItem.values().forEach { tab ->
      val isSelected = activeTab == tab
      val animatedBg by animateColorAsState(
        targetValue = if (isSelected) CyberCardBg else Color.Transparent,
        animationSpec = tween(250),
        label = "tab_bg"
      )
      val animatedColor by animateColorAsState(
        targetValue = if (isSelected) CyberCyan else CyberGrayText,
        animationSpec = tween(250),
        label = "tab_label_color"
      )

      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .background(animatedBg)
          .border(
            width = if (isSelected) 1.dp else 0.dp,
            color = if (isSelected) CyberBorder else Color.Transparent,
            shape = RoundedCornerShape(8.dp)
          )
          .clickable { onTabSelected(tab) }
          .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = tab.icon,
            contentDescription = tab.title,
            tint = animatedColor,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = tab.title,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = animatedColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
fun FileHUDPanel(
  fileState: FileState?,
  onFileSelect: (String, String, String) -> Unit,
  onReset: () -> Unit
) {
  // Preset simulation files
  val mockFiles = listOf(
    Pair("original_game_clean.apk", "48.2 MB"),
    Pair("tiktok_360jiagu_shelled.apk", "112.5 MB"),
    Pair("whatsapp_pairip_protected.xapk", "142.1 MB"),
    Pair("arm_secshell_stub_compiled.apks", "24.9 MB"),
    Pair("google_integrity_check.apk", "18.3 MB")
  )

  var expandedDropdown by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
    colors = CardDefaults.cardColors(containerColor = CyberCardBg)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "ACTIVE HIGH-FIDELITY FILE STATE",
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = CyberCyan
        )

        if (fileState != null) {
          Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(
              containerColor = CyberRed.copy(alpha = 0.2f),
              contentColor = CyberRed
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp),
            shape = RoundedCornerShape(4.dp)
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Detach", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (fileState == null) {
        // Drag and Drop Simulator Box
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0AFFFFFF))
            .border(
              width = 1.5.dp,
              brush = Brush.sweepGradient(listOf(CyberCyan, CyberPurple, CyberCyan)),
              shape = RoundedCornerShape(12.dp)
            )
            .clickable { expandedDropdown = !expandedDropdown }
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.CloudUpload,
              contentDescription = "Upload Icon",
              tint = CyberCyan,
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "DRAG & DROP REAL OR MOCK APK HERE",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Tap to choose high-fidelity demo packages (APK / APKS / XAPK)",
              fontSize = 10.sp,
              color = CyberGrayText,
              textAlign = TextAlign.Center
            )
          }
        }

        // Expanded Preset list dropdown for Android interactive convenience
        if (expandedDropdown) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Select high-fidelity demo package to load:",
            fontSize = 11.sp,
            color = CyberPurple,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF13131F))
              .border(0.5.dp, CyberBorder, RoundedCornerShape(8.dp))
          ) {
            mockFiles.forEach { (name, size) ->
              val ext = name.substringAfterLast(".")
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    onFileSelect(name, size, ext)
                    expandedDropdown = false
                  }
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = when (ext) {
                      "xapk" -> Icons.Default.FolderZip
                      "apks" -> Icons.Default.GridGoldenratio
                      else -> Icons.Default.Android
                    },
                    contentDescription = null,
                    tint = if (name.contains("shelled") || name.contains("protected")) CyberPurple else CyberCyan,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(name, fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                }
                Text(size, fontSize = 10.sp, color = CyberGrayText)
              }
              HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            }
          }
        }
      } else {
        // Elevated HUD with file details
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF151522)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = when (fileState.extension) {
                  "xapk" -> Icons.Default.FolderZip
                  "apks" -> Icons.Default.GridGoldenratio
                  else -> Icons.Default.Android
                },
                contentDescription = null,
                tint = if (fileState.hasShell) CyberPurple else CyberCyan,
                modifier = Modifier.size(28.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = fileState.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Size: ${fileState.size}",
                  fontSize = 11.sp,
                  color = CyberGrayText
                )
                Text(
                  text = "│",
                  fontSize = 11.sp,
                  color = Color.White.copy(alpha = 0.2f)
                )
                Text(
                  text = "Format: ${fileState.extension.uppercase()}",
                  fontSize = 11.sp,
                  color = CyberCyan,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

          // Scanned characteristics
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("PROTECTION SIGNATURE STATUS:", fontSize = 9.sp, color = CyberGrayText)
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (fileState.hasShell) CyberYellow else CyberGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = fileState.detectedSignature,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (fileState.hasShell) CyberYellow else CyberGreen,
                  fontFamily = FontFamily.Monospace
                )
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Text("REVERSIBILITY RATING:", fontSize = 9.sp, color = CyberGrayText)
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = if (fileState.hasShell) "HIGH (DEX DUMPABLE)" else "FULLY READABLE",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (fileState.hasShell) CyberCyan else CyberGreen,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }

          // Static signature markers
          if (fileState.hasShell) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CyberYellow.copy(alpha = 0.1f))
                .border(0.5.dp, CyberYellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(8.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = null,
                  tint = CyberYellow,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = "Packed with native protector. Unpacking will restore headers and dump plaintext DEX chunks.",
                  fontSize = 10.sp,
                  color = CyberYellow
                )
              }
            }
          }
        }
      }
    }
  }
}

// Module A Component: Protector
@Composable
fun ProtectorTab(fileState: FileState?) {
  var shellVmpOption by remember { mutableStateOf("360") } // "360", "PairIP", "Google", "ARM"
  var dexConfusion by remember { mutableStateOf(true) }
  var stringEncryption by remember { mutableStateOf(true) }
  var classObfuscation by remember { mutableStateOf(false) }
  var resConfusion by remember { mutableStateOf(true) }
  var manifestEncrypt by remember { mutableStateOf(false) }

  var processingState by remember { mutableStateOf("IDLE") } // IDLE, PROCESSING, SUCCESS
  var progressFloat by remember { mutableFloatStateOf(0f) }
  val consoleLogs = remember { mutableStateListOf<String>() }
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
    colors = CardDefaults.cardColors(containerColor = CyberCardBg)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "MODULE A: APK PROTECTOR (加固 ENGINE)",
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = CyberCyan
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Select protection matrices inspired by professional mobile hardening security platforms.",
        fontSize = 11.sp,
        color = CyberGrayText
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Section 1: Shelling stub options
      Text(
        text = "VMP / Hard-Shelling Options (壳 selection):",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(
          Triple("360", "360加固", CyberCyan),
          Triple("PairIP", "PairIP", CyberPurple),
          Triple("Google", "Play Stub", CyberGreen),
          Triple("ARM", "ARM Sec", CyberYellow)
        ).forEach { (id, label, color) ->
          val active = shellVmpOption == id
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(if (active) color.copy(alpha = 0.2f) else Color(0xFF13131A))
              .border(
                width = 1.dp,
                color = if (active) color else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
              )
              .clickable { shellVmpOption = id }
              .padding(8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = label,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (active) color else Color.White
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Section 2: DEX Protection matrices
      Text(
        text = "DEX Hardening Matrix:",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
      Spacer(modifier = Modifier.height(6.dp))
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CheckboxRow(
          checked = dexConfusion,
          onCheckedChange = { dexConfusion = it },
          title = "DEX Confusion (Dex 混淆 - Proguard++ Pack)",
          desc = "Flattens control-flow graphs and scatters instruction logs."
        )
        CheckboxRow(
          checked = stringEncryption,
          onCheckedChange = { stringEncryption = it },
          title = "String / Constant Array Encryption",
          desc = "Scrambles credentials and system endpoint strings into byte ranges."
        )
        CheckboxRow(
          checked = classObfuscation,
          onCheckedChange = { classObfuscation = it },
          title = "Class & Direct Method Renaming",
          desc = "Renames package namespaces into randomized Cyrillic alphabets."
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Section 3: Advanced resource scrambling
      Text(
        text = "Advanced Packing Layers:",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
      Spacer(modifier = Modifier.height(6.dp))
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CheckboxRow(
          checked = resConfusion,
          onCheckedChange = { resConfusion = it },
          title = "Resource Protection (Res 混淆 / Scrambling)",
          desc = "Shrinks resource ID maps and renames structural layouts to duplicate chars."
                )
        CheckboxRow(
          checked = manifestEncrypt,
          onCheckedChange = { manifestEncrypt = it },
          title = "Binary Manifest & Raw Assets Encryptor",
          desc = "Encrypts AndroidManifest & asset binaries into custom block headers."
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Run / Progress Actions
      if (processingState == "IDLE") {
        Button(
          onClick = {
            if (fileState == null) {
              consoleLogs.clear()
              consoleLogs.add("[!] WARNING: No source APK loaded. Initializing demonstration shell...")
            }
            scope.launch {
              processingState = "PROCESSING"
              progressFloat = 0.0f
              consoleLogs.clear()
              consoleLogs.add("[+] Initializing Jiagu Matrix compiler block...")
              delay(300)
              consoleLogs.add("[~] Loading target: ${fileState?.name ?: "demo_dummy.apk"}")
              delay(400)
              progressFloat = 0.15f
              consoleLogs.add("[*] Scrambling direct string mappings into encrypted decryptor subroutines...")
              delay(500)
              progressFloat = 0.40f
              consoleLogs.add("[*] Executing DEX Obfuscation and Control-Flow Flattening...")
              delay(600)
              progressFloat = 0.65f
              consoleLogs.add("[*] Packaging custom stub assembly shell based on: [${shellVmpOption.uppercase()}]")
              delay(700)
              progressFloat = 0.85f
              consoleLogs.add("[*] Compiling resources maps, obfuscating layout paths...")
              delay(400)
              progressFloat = 1.0f
              consoleLogs.add("[✔] Integration Complete! Shelled output generated: protected_${fileState?.name ?: "app_vmp_compiled.apk"}")
              processingState = "SUCCESS"
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(if (fileState != null) 3.dp else 0.dp, ambientColor = CyberCyan, spotColor = CyberCyan),
          colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (fileState != null) "PROTECT ACTIVE APK" else "SIMULATE JUMP MATRIX COMPILE",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontSize = 12.sp
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (processingState == "PROCESSING") "APPLYING DEFENSIVE HARDENING MATRIX..." else "APK SUCCESSFULLY HARDENED!",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (processingState == "PROCESSING") CyberCyan else CyberGreen
            )
            Text(
              text = "${(progressFloat * 100).toInt()}%",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (processingState == "PROCESSING") CyberCyan else CyberGreen
            )
          }

          LinearProgressIndicator(
            progress = { progressFloat },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = if (processingState == "PROCESSING") CyberCyan else CyberGreen,
            trackColor = Color(0xFF1B1B25),
          )

          // Live scrolling logger view
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "PROCESS FLOW MONITOR:",
            fontSize = 9.sp,
            color = CyberGrayText,
            fontWeight = FontWeight.Bold
          )
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(130.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF07070B))
              .border(0.5.dp, CyberBorder, RoundedCornerShape(8.dp))
              .padding(8.dp)
          ) {
            LazyColumn(
              state = listState,
              verticalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              items(consoleLogs) { log ->
                Text(
                  text = log,
                  fontSize = 10.sp,
                  fontFamily = FontFamily.Monospace,
                  color = when {
                    log.startsWith("[✔]") -> CyberGreen
                    log.startsWith("[!]") -> CyberRed
                    log.startsWith("[~]") -> CyberYellow
                    else -> Color.White
                  }
                )
              }
            }

            // Auto-scroll logic helper
            LaunchedEffect(consoleLogs.size) {
              if (consoleLogs.isNotEmpty()) {
                listState.animateScrollToItem(consoleLogs.size - 1)
              }
            }
          }

          if (processingState == "SUCCESS") {
            Button(
              onClick = {
                processingState = "IDLE"
                progressFloat = 0f
                consoleLogs.clear()
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C24)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Reset & Harden Another", fontSize = 11.sp, color = CyberCyan)
            }
          }
        }
      }
    }
  }
}

// Module B Component: APK Unpacker (脱壳 Engine)
@Composable
fun UnpackerTab(fileState: FileState?) {
  var dumpDexChoice by remember { mutableStateOf(true) }
  var pairIPStrip by remember { mutableStateOf(true) }
  var integrityBypass by remember { mutableStateOf(true) }
  var headerRecon by remember { mutableStateOf(true) }
  var stringDecryption by remember { mutableStateOf(true) }

  var unpackingState by remember { mutableStateOf("IDLE") } // IDLE, SCANNING, UNPACKING, DONE
  var progressFloat by remember { mutableFloatStateOf(0f) }
  val unpackLogs = remember { mutableStateListOf<String>() }
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
    colors = CardDefaults.cardColors(containerColor = CyberCardBg)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "MODULE B: PROTECTION REMOVER (脱壳 / SHELL EXTRACTOR)",
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = CyberPurple
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Scan target packages in modern sandbox memory layers to dump and decrypt compiled DEX streams.",
        fontSize = 11.sp,
        color = CyberGrayText
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Section 1: Signatures Found Auto-detection HUD
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF13131F))
          .border(0.5.dp, CyberBorder, RoundedCornerShape(10.dp))
          .padding(12.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "ENGINE SHELL DETECTION STATUS:",
              fontSize = 9.sp,
              color = CyberGrayText,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = if (fileState?.hasShell == true) "SIGNATURE MATCHED" else "NO SHELL STUB MATCHED",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (fileState?.hasShell == true) CyberYellow else CyberCyan,
              fontFamily = FontFamily.Monospace
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = if (fileState?.hasShell == true) Icons.Default.Terminal else Icons.Default.CheckCircle,
              contentDescription = null,
              tint = if (fileState?.hasShell == true) CyberYellow else CyberGreen,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = fileState?.detectedSignature ?: "Demo Mode - No active APK file loaded.",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Section 2: Stripping Options
      Text(
        text = "Select Unpacking & Dumping Toggles:",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
      Spacer(modifier = Modifier.height(6.dp))
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CheckboxRow(
          checked = dumpDexChoice,
          onCheckedChange = { dumpDexChoice = it },
          title = "Automated DEX Dumping (脱壳 360 / ARM)",
          desc = "Intercepts system process load hooks to dump decrypted DEX streams."
        )
        CheckboxRow(
          checked = pairIPStrip,
          onCheckedChange = { pairIPStrip = it },
          title = "PairIP / SecShell Stripper Engine",
          desc = "Reconstructs wrapper code definitions and extracts hidden zip assemblies."
        )
        CheckboxRow(
          checked = integrityBypass,
          onCheckedChange = { integrityBypass = it },
          title = "Integrity Verification Stub Bypass",
          desc = "Modifies signature verification return values in JNI libraries automatically."
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Post-Processing Fixers:",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
      Spacer(modifier = Modifier.height(6.dp))
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CheckboxRow(
          checked = headerRecon,
          onCheckedChange = { headerRecon = it },
          title = "DEX Header Reconstruction (Fixer)",
          desc = "Repairs magic signature offsets, checksums, and string count indices of dumped files."
        )
        CheckboxRow(
          checked = stringDecryption,
          onCheckedChange = { stringDecryption = it },
          title = "Inline Decrypted String Replacer",
          desc = "Scans Java classes and converts obfuscated dynamic arrays into flat literal strings."
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Trigger Actions
      if (unpackingState == "IDLE") {
        Button(
          onClick = {
            scope.launch {
              unpackingState = "SCANNING"
              progressFloat = 0.0f
              unpackLogs.clear()
              unpackLogs.add("[+] Scanning payload entropy of target apk...")
              delay(500)
              val matchedShell = fileState?.hasShell ?: false
              val shellName = fileState?.shellType ?: "None"
              if (matchedShell) {
                unpackLogs.add("[!] MATCHED KNOWN STUB PATTERNS: [$shellName]")
              } else {
                unpackLogs.add("[~] Alert: Target package has custom layout flags. Initializing dynamic VM analysis...")
              }
              delay(300)
              unpackingState = "UNPACKING"
              progressFloat = 0.2f
              unpackLogs.add("[+] Allocating sandboxed virtual environments for process spawning...")
              delay(400)
              progressFloat = 0.45f
              unpackLogs.add("[+] Loading dynamic library loader hook vectors...")
              delay(500)
              progressFloat = 0.65f
              unpackLogs.add("[*] Found active memory DEX page at offset 0xF5DE28. Dumping content...")
              delay(600)
              progressFloat = 0.85f
              unpackLogs.add("[*] Correcting DEX headers (setting magic number to 'dex\\n035')...")
              delay(400)
              progressFloat = 1.0f
              unpackLogs.add("[✔] COMPLETE: 3 decrypted DEX files extracted to workspace root!")
              unpackingState = "DONE"
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(if (fileState != null) 3.dp else 0.dp, ambientColor = CyberPurple, spotColor = CyberPurple),
          colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (fileState?.hasShell == true) "LAUNCH PROTECTION REMOVER ON STUB" else "DYNAMIC DUMP SIMULATOR",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 11.sp
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (unpackingState == "SCANNING") "VIRTUAL SANDBOX SIGNATURE ANALYSIS..." else if (unpackingState == "UNPACKING") "DUMPING DEX FROM PACKED MEMORY CHUNKS..." else "UNPACK COMPLETE!",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (unpackingState == "DONE") CyberGreen else CyberPurple
            )
            Text(
              text = "${(progressFloat * 100).toInt()}%",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (unpackingState == "DONE") CyberGreen else CyberPurple
            )
          }

          LinearProgressIndicator(
            progress = { progressFloat },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = if (unpackingState == "DONE") CyberGreen else CyberPurple,
            trackColor = Color(0xFF1B1B25),
          )

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "EXTRACTOR TERMINAL STREAM:",
            fontSize = 9.sp,
            color = CyberGrayText,
            fontWeight = FontWeight.Bold
          )
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(130.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF07070B))
              .border(0.5.dp, CyberBorder, RoundedCornerShape(8.dp))
              .padding(8.dp)
          ) {
            LazyColumn(
              state = listState,
              verticalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              items(unpackLogs) { log ->
                Text(
                  text = log,
                  fontSize = 10.sp,
                  fontFamily = FontFamily.Monospace,
                  color = when {
                    log.startsWith("[✔]") -> CyberGreen
                    log.startsWith("[!]") -> CyberRed
                    log.startsWith("[~]") -> CyberYellow
                    else -> Color.White
                  }
                )
              }
            }

            LaunchedEffect(unpackLogs.size) {
              if (unpackLogs.isNotEmpty()) {
                listState.animateScrollToItem(unpackLogs.size - 1)
              }
            }
          }

          if (unpackingState == "DONE") {
            Button(
              onClick = {
                unpackingState = "IDLE"
                progressFloat = 0f
                unpackLogs.clear()
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C24)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Reset & Analyze Next Payload", fontSize = 11.sp, color = CyberPurple)
            }
          }
        }
      }
    }
  }
}

// Module C Component: Decompiler
@Composable
fun DecompilerTab(fileState: FileState?) {
  var selectedFileIndex by remember { mutableStateOf(0) }
  var viewCodeLanguage by remember { mutableStateOf("Smali") } // "Smali", "Java"

  // High-fidelity smali files preview list
  val codeFiles = listOf(
    Pair(
      "MainActivity.smali",
      """.class public Lcom/sr7mods/bypass/MainActivity;
.super Landroidx/activity/ComponentActivity;
.source "MainActivity.kt"

# direct methods
.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Landroidx/activity/ComponentActivity;-><init>()V
    return-void
.end method

# virtual methods
.method protected onCreate(Landroid/os/Bundle;)V
    .registers 4
    invoke-super {p0, p1}, Landroidx/activity/ComponentActivity;->onCreate(Landroid/os/Bundle;)V
    const-string v0, "SR7 Mods: Bypass Shield loader successfully injected!"
    const/4 v1, 0x1
    invoke-static {p0, v0, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    return-void
.end method"""
    ),
    Pair(
      "StubApplication.java",
      """package com.jiagu;

import android.app.Application;
import android.content.Context;
import java.io.File;

public class StubApplication extends Application {
    private static final String PACK_TAG = "360JiaguVM";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            // Dynamic Decryptor Routine
            String shellBinary = base.getCacheDir().getAbsolutePath() + "/libjiagu.so";
            System.load(shellBinary);
            // Dynamic DEX decryption stub triggers here
            initNativeDecryption(shellBinary);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private native void initNativeDecryption(String libPath);
}"""
    ),
    Pair(
      "pairip_anti_cheat.smali",
      """.class public Lcom/pairip/AntiCheat;
.super Ljava/lang/Object;

.method public static checkSignature(Landroid/content/Context;)Z
    .registers 3
    # Dump static signature offsets
    const-string v0, "HEX_SIGNATURE_STUB: 4a9bc8d50e1811a21bc9e056d819932ad"
    invoke-virtual {p0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;
    move-result-object v1
    # Verify return registers
    const/4 v0, 0x1
    return v0
.end method"""
    )
  )

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    // Subsection 1: Jadx/NP-style Client-Side Smali Viewer
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
      colors = CardDefaults.cardColors(containerColor = CyberCardBg)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "APK DECOMPILER VIEWPORT",
              fontFamily = FontFamily.Monospace,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = CyberCyan
            )
            Text(
              text = "Client-side Jadx source inspector.",
              fontSize = 10.sp,
              color = CyberGrayText
            )
          }

          // Smali / Java toggle tabs
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF13131F))
              .padding(2.dp)
          ) {
            listOf("Smali", "Java").forEach { lang ->
              val active = viewCodeLanguage == lang
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (active) CyberCyan.copy(alpha = 0.2f) else Color.Transparent)
                  .clickable { viewCodeLanguage = lang }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = lang,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (active) CyberCyan else CyberGrayText
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Files Tree side list (Interactive Tabs)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Left Navigator file list
          Column(
            modifier = Modifier
              .weight(0.4f)
              .fillMaxHeight()
              .background(Color(0xFF0D0D15))
              .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              "WORK DISK FILES:",
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = CyberPurple,
              modifier = Modifier.padding(4.dp)
            )

            codeFiles.forEachIndexed { idx, pair ->
              val isSel = selectedFileIndex == idx
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (isSel) Color(0x2200F0FF) else Color.Transparent)
                  .border(
                    width = if (isSel) 0.5.dp else 0.dp,
                    color = if (isSel) CyberCyan else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                  )
                  .clickable { selectedFileIndex = idx }
                  .padding(8.dp)
              ) {
                Column {
                  Text(
                    text = pair.first,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isSel) Color.White else CyberGrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = if (pair.first.contains(".smali")) "Smali Assembly" else "Java Source",
                    fontSize = 8.sp,
                    color = CyberGrayText
                  )
                }
              }
            }
          }

          // Right editor panel (Highlighted smali blocks)
          Box(
            modifier = Modifier
              .weight(0.6f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF07070B))
              .border(0.5.dp, CyberBorder, RoundedCornerShape(8.dp))
              .padding(8.dp)
              .horizontalScroll(rememberScrollState())
              .verticalScroll(rememberScrollState())
          ) {
            val codeTxt = codeFiles[selectedFileIndex].second
            val formattedCode = buildAnnotatedString {
              val lines = codeTxt.split("\n")
              lines.forEach { line ->
                val trimmed = line.trim()
                when {
                  trimmed.startsWith(".class") || trimmed.startsWith(".super") || trimmed.startsWith(".source") -> {
                    withStyle(style = SpanStyle(color = CyberPurple, fontWeight = FontWeight.Bold)) {
                      append(line)
                    }
                  }
                  trimmed.startsWith("#") || trimmed.startsWith("//") -> {
                    withStyle(style = SpanStyle(color = CyberGrayText)) {
                      append(line)
                    }
                  }
                  trimmed.startsWith(".method") || trimmed.startsWith(".end method") -> {
                    withStyle(style = SpanStyle(color = CyberCyan, fontWeight = FontWeight.Bold)) {
                      append(line)
                    }
                  }
                  trimmed.contains("const-string") || trimmed.contains("String ") -> {
                    withStyle(style = SpanStyle(color = CyberYellow)) {
                      append(line)
                    }
                  }
                  trimmed.contains("invoke-") -> {
                    withStyle(style = SpanStyle(color = CyberGreen, fontFamily = FontFamily.Monospace)) {
                      append(line)
                    }
                  }
                  else -> {
                    withStyle(style = SpanStyle(color = Color.White)) {
                      append(line)
                    }
                  }
                }
                append("\n")
              }
            }

            Text(
              text = formattedCode,
              fontFamily = FontFamily.Monospace,
              fontSize = 9.sp,
              color = Color.White
            )
          }
        }
      }
    }
  }
}

// Module D Component: Anti-Split / APKS Merger
@Composable
fun AntiSplitTab() {
  val mergerFilesUploaded = remember { mutableStateListOf<String>() }
  var mergingProgress by remember { mutableStateOf("IDLE") } // IDLE, RUNNING, FINISHED
  val mergeLogs = remember { mutableStateListOf<String>() }
  val scope = rememberCoroutineScope()

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
      colors = CardDefaults.cardColors(containerColor = CyberCardBg)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "ANTI-SPLIT UTILITY (XAPK/APKS MERGER)",
          fontFamily = FontFamily.Monospace,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = CyberCyan
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Combine multi-config split APK segments into one unified standalone offline package.",
          fontSize = 11.sp,
          color = CyberGrayText
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Multi Split Files Select box
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF13131F))
            .border(0.5.dp, CyberBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "SPLIT APK SEGMENTS TO MERGE:",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = CyberCyan
            )

            Button(
              onClick = {
                if (mergerFilesUploaded.size < 5) {
                  val count = mergerFilesUploaded.size + 1
                  mergerFilesUploaded.add("config.split_abi_$count.apk")
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.15f), contentColor = CyberCyan),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(28.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Split", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (mergerFilesUploaded.isEmpty()) {
            Text(
              "No configuration splits attached. Tap 'Add Split' to compile virtual splits.",
              fontSize = 10.sp,
              color = CyberGrayText,
              textAlign = TextAlign.Center,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
            )
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              mergerFilesUploaded.forEachIndexed { index, fileName ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F0F17))
                    .padding(8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(14.dp))
                    Text(fileName, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                  }

                  IconButton(
                    onClick = { mergerFilesUploaded.removeAt(index) },
                    modifier = Modifier.size(20.dp)
                  ) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = CyberRed, modifier = Modifier.size(14.dp))
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (mergingProgress == "IDLE") {
          Button(
            onClick = {
              if (mergerFilesUploaded.isEmpty()) {
                mergerFilesUploaded.add("base.apk")
                mergerFilesUploaded.add("config.arm64_v8a.apk")
                mergerFilesUploaded.add("config.xxxhdpi.apk")
              }
              scope.launch {
                mergingProgress = "RUNNING"
                mergeLogs.clear()
                mergeLogs.add("[~] Initiating split resource dependency tree scan...")
                delay(400)
                mergeLogs.add("[+] Found base.apk - validating layout configuration tables...")
                delay(500)
                mergeLogs.add("[+] Merging ABI binaries from target: arm64_v8a...")
                delay(600)
                mergeLogs.add("[+] Merging resources matching target: xxxhdpi...")
                delay(600)
                mergeLogs.add("[+] Re-indexing resource identifiers and correcting string pools...")
                delay(500)
                mergeLogs.add("[✔] Consolidated standalone package compiled: standalone_merged.apk")
                mergingProgress = "FINISHED"
              }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
          ) {
            Icon(Icons.Default.Merge, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "BUILD CONSOLIDATED STANDALONE APK",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color.Black,
              fontFamily = FontFamily.Monospace
            )
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (mergingProgress == "RUNNING") "COMBINING SPLIT MODULES..." else "MERGE COMPLETE!",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (mergingProgress == "FINISHED") CyberGreen else CyberCyan
              )
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF07070B))
                .border(0.5.dp, CyberBorder, RoundedCornerShape(8.dp))
                .padding(8.dp)
            ) {
              LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
              ) {
                items(mergeLogs) { log ->
                  Text(
                    text = log,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (log.startsWith("[✔]")) CyberGreen else Color.White
                  )
                }
              }
            }

            if (mergingProgress == "FINISHED") {
              Button(
                onClick = {
                  mergingProgress = "IDLE"
                  mergerFilesUploaded.clear()
                  mergeLogs.clear()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13131F)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Compile Another Standalone", fontSize = 11.sp, color = CyberCyan)
              }
            }
          }
        }
      }
    }
  }
}

// Module D: Technical specs & TS System architecture interface contract
@Composable
fun ArchitectureContractsTab() {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
    colors = CardDefaults.cardColors(containerColor = CyberCardBg)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "DEVELOPER SDK: TYPESCRIPT CORE ARCHITECTURE",
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = CyberCyan
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "This technical contract definitions file represents the underlying binary and state-management system specifications.",
        fontSize = 11.sp,
        color = CyberGrayText
      )

      Spacer(modifier = Modifier.height(12.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(350.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF07070B))
          .border(0.5.dp, CyberBorder, RoundedCornerShape(8.dp))
          .padding(8.dp)
          .horizontalScroll(rememberScrollState())
          .verticalScroll(rememberScrollState())
      ) {
        val tsInterfaces = """/**
 * @file ApkProcessorContracts.ts
 * @details Core TypeScript architectural interfaces for Apk Protector (Jiagu)
 * and Automated Reversing Workspace modules.
 */

export type ApkExtensionType = 'apk' | 'apks' | 'xapk';

export interface FileState {
  id: string;
  name: string;
  bytes: number;
  extension: ApkExtensionType;
  signature?: ProtectionSignature;
}

export interface ProtectionSignature {
  hasShell: boolean;
  shellType: '360加固' | 'PairIP' | 'GoogleStub' | 'ARMSec' | 'None';
  entropy: number;
  dexFilesCount: number;
  unpackedHeadersFixed: boolean;
}

export interface JiaguOptions {
  vmpType: '360' | 'PairIP' | 'Google' | 'ARM';
  densityScrambling: boolean;
  dexConfusion: boolean;
  stringEncryption: boolean;
  classObfuscation: boolean;
  resourceScrambling: boolean;
  manifestEncryption: boolean;
}

/**
 * High-performance Asynchronous Handlers
 */
export async function handleJiaguProtection(
  file: FileState,
  options: JiaguOptions,
  onProgress: (percent: number, logMessage: string) => void
): Promise<FileState> {
  // Sandbox matrix hardener logic mock contract
  return new Promise((resolve) => {
    // 1. Compile protector stubs
    // 2. Encrypt constant tables
    // 3. Inject native unpacking VM stubs v4
  });
}

export async function handleUnpackingDump(
  file: FileState,
  onProgress: (percent: number, status: string) => void
): Promise<{ decryptedDexes: string[]; headersFixed: boolean }> {
  // Automated Memory Hook Dumping routine simulation contract
  return {
    decryptedDexes: ['classes.dex', 'classes2.dex', 'classes3.dex'],
    headersFixed: true
  };
}

export async function handleSplitMerge(
  splitApkFiles: FileState[],
  onProgress: (log: string) => void
): Promise<FileState> {
  // Consolidated single package builder mockup
  return {} as FileState;
}"""

        val formattedCode = buildAnnotatedString {
          val lines = tsInterfaces.split("\n")
          lines.forEach { line ->
            val trimmed = line.trim()
            when {
              trimmed.startsWith("export ") || trimmed.startsWith("import ") || trimmed.startsWith("interface ") || trimmed.startsWith("type ") -> {
                withStyle(style = SpanStyle(color = CyberCyan, fontWeight = FontWeight.Bold)) {
                  append(line)
                }
              }
              trimmed.startsWith("*") || trimmed.startsWith("/**") || trimmed.startsWith("*/") || trimmed.startsWith("//") -> {
                withStyle(style = SpanStyle(color = CyberGrayText)) {
                  append(line)
                }
              }
              trimmed.contains("async ") || trimmed.contains("Promise<") || trimmed.contains("return ") -> {
                withStyle(style = SpanStyle(color = CyberPurple, fontWeight = FontWeight.Bold)) {
                  append(line)
                }
              }
              trimmed.contains("'") || trimmed.contains("\"") -> {
                withStyle(style = SpanStyle(color = CyberYellow)) {
                  append(line)
                }
              }
              else -> {
                withStyle(style = SpanStyle(color = Color.White)) {
                  append(line)
                }
              }
            }
            append("\n")
          }
        }

        Text(
          text = formattedCode,
          fontFamily = FontFamily.Monospace,
          fontSize = 9.sp,
          color = Color.White
        )
      }
    }
  }
}

// Common checkbox helper item with premium visual effects
@Composable
fun CheckboxRow(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  title: String,
  desc: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF0F0F16))
      .clickable { onCheckedChange(!checked) }
      .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Checkbox(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = CheckboxDefaults.colors(
        checkedColor = CyberCyan,
        uncheckedColor = CyberGrayText,
        checkmarkColor = Color.Black
      ),
      modifier = Modifier.size(24.dp)
    )

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (checked) CyberCyan else Color.White
      )
      Text(
        text = desc,
        fontSize = 9.sp,
        color = CyberGrayText
      )
    }
  }
}

// Mandatory clickable visual branding targets required at bottom of layouts
@Composable
fun BrandingFooter(onFooterClick: () -> Unit) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .border(
        width = 1.dp,
        brush = Brush.horizontalGradient(listOf(CyberCyan, CyberPurple)),
        shape = RoundedCornerShape(12.dp)
      )
      .clickable(onClick = onFooterClick),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F16))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "MODDING SYSTEM COMPILER FRAMEWORK PROVIDED BY",
        fontFamily = FontFamily.Monospace,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        color = CyberGrayText
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "SR7 Mods",
        fontFamily = FontFamily.Monospace,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = CyberCyan
      )

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = "Tap to visit developer workspace on https://alexsifatrayhan.github.io/about-me/",
        fontSize = 9.sp,
        color = CyberPurple,
        textAlign = TextAlign.Center
      )
    }
  }
}
