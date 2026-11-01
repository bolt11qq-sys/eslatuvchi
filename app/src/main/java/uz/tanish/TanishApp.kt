package uz.tanish

import android.app.Application
import uz.tanish.log.ErrorLog

class TanishApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ErrorLog.init(this)
    }
}
