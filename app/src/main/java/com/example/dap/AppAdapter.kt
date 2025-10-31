package com.example.dap // ALLINEATO

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val apps: List<ApplicationInfo>,
    private val pm: PackageManager,
    private val dpm: DevicePolicyManager,
    private val adminComponent: ComponentName
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val appName: TextView = view.findViewById(R.id.tvAppName)
        val blockButton: Button = view.findViewById(R.id.btnBlockApp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        val appName = app.loadLabel(pm).toString()
        val appIcon = app.loadIcon(pm)

        holder.appName.text = appName
        holder.appIcon.setImageDrawable(appIcon)

        // Controlla se l'app è già bloccata (nascosta)
        val isAppHidden = try {
            dpm.isApplicationHidden(adminComponent, app.packageName)
        } catch (e: Exception) {
            false
        }

        updateButtonState(holder.blockButton, isAppHidden)

        holder.blockButton.setOnClickListener {
            try {
                val currentlyHidden = dpm.isApplicationHidden(adminComponent, app.packageName)
                val newState = !currentlyHidden

                // Applica il blocco (nascondi l'app)
                dpm.setApplicationHidden(adminComponent, app.packageName, newState)

                Log.d("AppAdapter", "App ${app.packageName} impostata a hidden=$newState")

                // Aggiorna il pulsante
                updateButtonState(holder.blockButton, newState)
            } catch (e: SecurityException) {
                Log.e("AppAdapter", "Errore blocco app: ${app.packageName}", e)
            }
        }
    }

    private fun updateButtonState(button: Button, isHidden: Boolean) {
        if (isHidden) {
            button.text = "Sblocca"
            // Puoi anche cambiare colore qui se vuoi
            // button.setBackgroundColor(...)
        } else {
            button.text = "Blocca"
            // button.setBackgroundColor(...)
        }
    }

    override fun getItemCount() = apps.size
}

