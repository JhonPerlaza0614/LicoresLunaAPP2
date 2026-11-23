package com.licoresluna2.views

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.licoresluna2.LicoresLunaApp
import com.licoresluna2.utils.SessionManager

abstract class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val scale = (newBase.applicationContext as LicoresLunaApp)
            .sessionManager
            .getFontScale()
            .coerceIn(SessionManager.FONT_SCALE_SMALL, SessionManager.FONT_SCALE_LARGE)

        val configuration = newBase.resources.configuration
        configuration.fontScale = scale
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }
}
