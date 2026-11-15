package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object LawyerContact {
    const val ADVISOR_NAME = "Luật gia Nguyễn Linh Giang"
    const val ADVISOR_CREDENTIALS =
        "Nguyên Chấp hành viên trung cấp, Thẩm tra viên chính, Trưởng phòng Kiểm tra và giải quyết khiếu nại - tố cáo Cục Thi hành án dân sự"
    const val ZALO_PHONE = "0915855111"
    const val ZALO_URL = "https://zalo.me/0915855111"
    const val FACEBOOK_FANPAGE_NAME = "Mr.Giang Law"
    const val FACEBOOK_FANPAGE_URL = "https://www.facebook.com/mrgiang.law"
    val HOTLINES = listOf("0848477070", "0978477070", "0915855111")

    const val BANK_NAME = "Ngân hàng Agribank"
    const val BANK_BIN = "970405"
    const val BANK_ACCOUNT_NUMBER = "4801215011146"
    const val BANK_ACCOUNT_HOLDER = "Nguyễn Linh Giang"

    fun buildAgribankVietQrUrl(amountVnd: Long, transferNote: String): String {
        val encodedNote = URLEncoder.encode(transferNote, "UTF-8")
        val encodedName = URLEncoder.encode("NGUYEN LINH GIANG", "UTF-8")
        return "https://img.vietqr.io/image/$BANK_BIN-$BANK_ACCOUNT_NUMBER-compact2.png" +
            "?amount=$amountVnd&addInfo=$encodedNote&accountName=$encodedName"
    }
}

fun openExternalUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Không tìm thấy ứng dụng để mở liên kết", Toast.LENGTH_SHORT).show()
    }
}

fun dialPhoneNumber(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Không thể mở trình quay số", Toast.LENGTH_SHORT).show()
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Đã sao chép: $text", Toast.LENGTH_SHORT).show()
}
