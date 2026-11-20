package com.example.automation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLEncoder

private const val TAG = "PhoneActionExecutor"

object PhoneActionExecutor {

    fun openWhatsAppMessage(context: Context, phoneOrRecipient: String, message: String): Result<String> {
        return try {
            val cleanPhone = phoneOrRecipient.filter { it.isDigit() || it == '+' }
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val intent = if (cleanPhone.length >= 7) {
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
                )
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    when {
                        isPackageInstalled(context, "com.whatsapp") -> setPackage("com.whatsapp")
                        isPackageInstalled(context, "com.whatsapp.w4b") -> setPackage("com.whatsapp.w4b")
                    }
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result.success("WhatsApp opened for confirmation")
        } catch (e: Exception) {
            Log.e(TAG, "WhatsApp launch failed", e)
            Result.failure(Exception("WhatsApp not installed or unavailable: ${e.localizedMessage}"))
        }
    }

    fun makePhoneCall(context: Context, phoneNumber: String): Result<String> {
        return try {
            val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
            if (cleanNumber.isBlank()) {
                return Result.failure(IllegalArgumentException("Enter a valid phone number."))
            }
            val hasCallPerm = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            val action = if (hasCallPerm) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(action, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val mode = if (hasCallPerm) "Direct call initiated" else "Dialer opened (CALL_PHONE not granted)"
            Result.success("$mode: $cleanNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Phone call failed", e)
            Result.failure(e)
        }
    }

    fun launchSystemCamera(context: Context, videoMode: Boolean = false): Result<String> {
        return try {
            val action = if (videoMode) {
                MediaStore.INTENT_ACTION_VIDEO_CAMERA
            } else {
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
            }
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val fallback = Intent(
                    if (videoMode) MediaStore.ACTION_VIDEO_CAPTURE else MediaStore.ACTION_IMAGE_CAPTURE
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(fallback)
            }
            Result.success(if (videoMode) "Video camera launched" else "Camera launched")
        } catch (e: Exception) {
            Log.e(TAG, "Camera launch failed", e)
            Result.failure(Exception("No camera app available: ${e.localizedMessage}"))
        }
    }

    fun createCameraCaptureUri(context: Context): Uri {
        val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val photoFile = File(cameraDir, "jarvis_capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    fun setFlashlight(context: Context, enable: Boolean): Result<Boolean> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val torchCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return Result.failure(IllegalStateException("No flashlight hardware found"))
            cameraManager.setTorchMode(torchCameraId, enable)
            Result.success(enable)
        } catch (e: Exception) {
            Log.e(TAG, "Torch toggle failed", e)
            Result.failure(e)
        }
    }

    fun searchYouTube(context: Context, query: String): Result<String> {
        return try {
            val encoded = URLEncoder.encode(query.ifBlank { "trending india" }, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encoded"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                if (isPackageInstalled(context, "com.google.android.youtube")) {
                    setPackage("com.google.android.youtube")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("YouTube opened for \"$query\"")
        } catch (e: Exception) {
            Log.e(TAG, "YouTube launch failed", e)
            Result.failure(e)
        }
    }

    fun searchWeb(context: Context, query: String): Result<String> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=$encoded")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Web search opened for \"$query\"")
        } catch (e: Exception) {
            Log.e(TAG, "Web search failed", e)
            Result.failure(e)
        }
    }

    fun openUrl(context: Context, rawUrl: String): Result<String> {
        return try {
            val normalized = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                rawUrl
            } else {
                "https://$rawUrl"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened $normalized")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sharePhotoToSocialApp(
        context: Context,
        targetPackage: String,
        appLabel: String,
        imageUri: Uri?,
        caption: String
    ): Result<String> {
        return try {
            if (caption.isNotBlank()) {
                copyToClipboard(context, "$appLabel Caption", caption)
            }
            val baseIntent = if (imageUri != null) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    if (caption.isNotBlank()) {
                        putExtra(Intent.EXTRA_TEXT, caption)
                    }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, caption)
                }
            }

            if (isPackageInstalled(context, targetPackage)) {
                baseIntent.setPackage(targetPackage)
                baseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(baseIntent)
                Result.success("$appLabel share opened (caption copied to clipboard)")
            } else {
                val chooser = Intent.createChooser(baseIntent, "Share via $appLabel").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                Result.success("$appLabel not installed — opened Android Share Sheet")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Social share failed", e)
            Result.failure(e)
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
