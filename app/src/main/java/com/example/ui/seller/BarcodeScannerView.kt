package com.example.ui.seller

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.IceBackground
import com.example.ui.theme.IceContainer
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerCard(
  onBarcodeDetected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var isScannerPaused by remember { mutableStateOf(false) }
  var isTorchOn by remember { mutableStateOf(false) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasCameraPermission = granted
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    color = PureWhite,
    shadowElevation = 4.dp,
  ) {
    if (!hasCameraPermission) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.horizontalGradient(listOf(IceBackground, PureWhite))
          )
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_empty_scan),
          contentDescription = "Illustration scan code-barres",
          modifier = Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, IceContainer, RoundedCornerShape(14.dp)),
          contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Scanner Code-Barres Direct",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Scannez un produit avec la caméra pour l'ajouter instantanément au panier.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
            modifier = Modifier.testTag("enable_camera_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.CameraAlt,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Activer la caméra",
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    } else {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(185.dp)
          .clip(RoundedCornerShape(22.dp))
          .background(Color(0xFF071426))
      ) {
        if (!isScannerPaused) {
          CameraBarcodePreview(
            isTorchOn = isTorchOn,
            onBarcodeDetected = onBarcodeDetected,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "Scanner en pause",
              style = MaterialTheme.typography.titleMedium,
              color = PureWhite.copy(alpha = 0.85f),
            )
          }
        }

        ScannerOverlayFrame(
          isScanning = !isScannerPaused,
          modifier = Modifier.fillMaxSize(),
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = RoyalBlue.copy(alpha = 0.88f),
          ) {
            Text(
              text = if (isScannerPaused) "PAUSE" else "SCAN ACTIF",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = PureWhite,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.45f),
            ) {
              IconButton(
                onClick = { isTorchOn = !isTorchOn },
                modifier = Modifier.size(40.dp),
              ) {
                Icon(
                  imageVector = if (isTorchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                  contentDescription = if (isTorchOn) "Éteindre le flash" else "Allumer le flash",
                  tint = if (isTorchOn) CyanGlow else PureWhite,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
            Surface(
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.45f),
            ) {
              IconButton(
                onClick = { isScannerPaused = !isScannerPaused },
                modifier = Modifier.size(40.dp),
              ) {
                Icon(
                  imageVector = if (isScannerPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                  contentDescription = if (isScannerPaused) "Reprendre le scan" else "Mettre en pause",
                  tint = PureWhite,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ScannerOverlayFrame(
  isScanning: Boolean,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep")
  val laserFraction by infiniteTransition.animateFloat(
    initialValue = 0.22f,
    targetValue = 0.78f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "laser_y",
  )

  Canvas(modifier = modifier) {
    val boxW = size.width * 0.72f
    val boxH = size.height * 0.62f
    val left = (size.width - boxW) / 2f
    val top = (size.height - boxH) / 2f
    val right = left + boxW
    val bottom = top + boxH
    val cornerLen = 26.dp.toPx()
    val strokeW = 3.5.dp.toPx()

    // Corner brackets
    val cornerColor = CyanGlow
    // Top-left
    drawLine(cornerColor, Offset(left, top), Offset(left + cornerLen, top), strokeW, StrokeCap.Round)
    drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLen), strokeW, StrokeCap.Round)
    // Top-right
    drawLine(cornerColor, Offset(right - cornerLen, top), Offset(right, top), strokeW, StrokeCap.Round)
    drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLen), strokeW, StrokeCap.Round)
    // Bottom-left
    drawLine(cornerColor, Offset(left, bottom - cornerLen), Offset(left, bottom), strokeW, StrokeCap.Round)
    drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW, StrokeCap.Round)
    // Bottom-right
    drawLine(cornerColor, Offset(right, bottom - cornerLen), Offset(right, bottom), strokeW, StrokeCap.Round)
    drawLine(cornerColor, Offset(right - cornerLen, bottom), Offset(right, bottom), strokeW, StrokeCap.Round)

    if (isScanning) {
      val yPos = size.height * laserFraction
      drawLine(
        brush = Brush.horizontalGradient(
          colors = listOf(Color.Transparent, CyanGlow, PureWhite, CyanGlow, Color.Transparent),
          startX = left,
          endX = right,
        ),
        start = Offset(left + 8f, yPos),
        end = Offset(right - 8f, yPos),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
      )
    }
  }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraBarcodePreview(
  isTorchOn: Boolean,
  onBarcodeDetected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
  val scanner = remember {
    val options = BarcodeScannerOptions.Builder()
      .setBarcodeFormats(
        Barcode.FORMAT_CODE_128,
        Barcode.FORMAT_EAN_13,
        Barcode.FORMAT_EAN_8,
        Barcode.FORMAT_UPC_A,
        Barcode.FORMAT_UPC_E,
        Barcode.FORMAT_QR_CODE,
        Barcode.FORMAT_CODE_39,
      )
      .build()
    BarcodeScanning.getClient(options)
  }

  var boundCamera by remember { mutableStateOf<Camera?>(null) }

  DisposableEffect(isTorchOn, boundCamera) {
    runCatching {
      if (boundCamera?.cameraInfo?.hasFlashUnit() == true) {
        boundCamera?.cameraControl?.enableTorch(isTorchOn)
      }
    }
    onDispose {}
  }

  DisposableEffect(Unit) {
    onDispose {
      runCatching { scanner.close() }
      cameraExecutor.shutdown()
    }
  }

  AndroidView(
    factory = { ctx ->
      val previewView = PreviewView(ctx).apply {
        scaleType = PreviewView.ScaleType.FILL_CENTER
      }
      val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
      cameraProviderFuture.addListener(
        {
          val cameraProvider = runCatching { cameraProviderFuture.get() }.getOrNull() ?: return@addListener
          val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
          }
          val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

          imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
              val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees,
              )
              scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                  val detected = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                  if (detected != null) {
                    ContextCompat.getMainExecutor(ctx).execute {
                      onBarcodeDetected(detected)
                    }
                  }
                }
                .addOnFailureListener { e ->
                  Log.w("BarcodeScanner", "Scan frame failed", e)
                }
                .addOnCompleteListener {
                  imageProxy.close()
                }
            } else {
              imageProxy.close()
            }
          }

          runCatching {
            cameraProvider.unbindAll()
            boundCamera = cameraProvider.bindToLifecycle(
              lifecycleOwner,
              CameraSelector.DEFAULT_BACK_CAMERA,
              preview,
              imageAnalysis,
            )
          }.onFailure { e ->
            Log.e("BarcodeScanner", "Camera binding failed", e)
          }
        },
        ContextCompat.getMainExecutor(ctx),
      )
      previewView
    },
    modifier = modifier,
  )
}
