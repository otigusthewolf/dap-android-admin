package com.example.dap // ALLINEATO

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d("AdminApp", "Amministratore dispositivo ABILITATO")
        Toast.makeText(context, "Amministratore dispositivo abilitato", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w("AdminApp", "AmministratORE DISPOSITIVO DISABILITATO")
        Toast.makeText(context, "Amministratore dispositivo disabilitato", Toast.LENGTH_SHORT).show()
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Intercetta l'azione principale e passala al genitore
        if (intent.action == ACTION_DEVICE_ADMIN_ENABLED) {
            super.onReceive(context, intent)
            return
        }
        super.onReceive(context, intent)
    }
}

