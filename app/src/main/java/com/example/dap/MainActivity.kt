package com.example.dap // ALLINEATO

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var userManager: UserManager

    // Viste UI
    private lateinit var adminControlsLayout: LinearLayout
    private lateinit var btnEnableAdmin: Button
    private lateinit var btnDisableAdmin: Button
    private lateinit var btnApplyPolicies: Button
    private lateinit var btnLockScreen: Button
    private lateinit var btnToggleCamera: Button
    private lateinit var btnWipeData: Button
    private lateinit var rvAppList: RecyclerView

    private var isCameraDisabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Carica il layout

        // Inizializza i manager di sistema
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java) // ALLINEATO
        userManager = getSystemService(Context.USER_SERVICE) as UserManager

        // Trova le viste (ID dal layout XML)
        adminControlsLayout = findViewById(R.id.adminControlsLayout)
        btnEnableAdmin = findViewById(R.id.btnEnableAdmin)
        btnDisableAdmin = findViewById(R.id.btnDisableAdmin)
        btnApplyPolicies = findViewById(R.id.btnApplyPolicies)
        btnLockScreen = findViewById(R.id.btnLockScreen)
        btnToggleCamera = findViewById(R.id.btnToggleCamera)
        btnWipeData = findViewById(R.id.btnWipeData)
        rvAppList = findViewById(R.id.rvAppList)

        // Imposta i listener per i click
        setupButtonListeners()

        // Configura il RecyclerView per la lista app
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        updateUI() // Aggiorna la UI ogni volta che l'app torna in primo piano
    }

    private fun setupButtonListeners() {
        btnEnableAdmin.setOnClickListener {
            enableDeviceAdmin()
        }

        btnDisableAdmin.setOnClickListener {
            // Rimuove i permessi di admin (solo se non è Device Owner)
            dpm.removeActiveAdmin(adminComponent)
            updateUI()
        }

        btnApplyPolicies.setOnClickListener {
            applyBaseDeviceOwnerPolicies()
        }

        btnLockScreen.setOnClickListener {
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow() // Applica policy 'force-lock'
            }
        }

        btnToggleCamera.setOnClickListener {
            if (dpm.isDeviceOwnerApp(packageName)) {
                try {
                    isCameraDisabled = !isCameraDisabled
                    dpm.setCameraDisabled(adminComponent, isCameraDisabled) // Applica policy 'disable-camera'
                    Toast.makeText(this, "Fotocamera ${if (isCameraDisabled) "Disabilitata" else "Abilitata"}", Toast.LENGTH_SHORT).show()
                    updateUI() // Aggiorna testo pulsante
                } catch (e: SecurityException) {
                    Log.e("AdminApp", "Errore impostazione fotocamera", e)
                }
            }
        }

        btnWipeData.setOnClickListener {
            if (dpm.isDeviceOwnerApp(packageName)) {
                Log.w("AdminApp", "TENTATIVO DI RESET DI FABBRICA!")
                Toast.makeText(this, "WipeData ATTENZIONE: Decommentare in codice per attivare.", Toast.LENGTH_LONG).show()
                // ATTENZIONE!! DECOMMENTA LA RIGA QUI SOTTO SOLO SE SAI COSA STAI FACENDO!
                 dpm.wipeData(0) // Applica policy 'wipe-data'
            }
        }
    }

    private fun setupRecyclerView() {
        rvAppList.layoutManager = LinearLayoutManager(this)

        if (dpm.isDeviceOwnerApp(packageName)) {
            // Solo il Device Owner può vedere la lista di tutte le app
            val pm = packageManager
            val allApps = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)

            // Filtra per mostrare solo app installate dall'utente o app di sistema aggiornate
            val userApps = allApps.filter {
                (it.flags and ApplicationInfo.FLAG_SYSTEM == 0) || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)
            }

            val adapter = AppAdapter(userApps, pm, dpm, adminComponent)
            rvAppList.adapter = adapter
        }
    }


    private fun enableDeviceAdmin() {
        // Avvia la schermata di sistema per richiedere i permessi di Admin
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.device_admin_description))
            startActivity(intent)
        }
    }

    private fun applyBaseDeviceOwnerPolicies() {
        if (!dpm.isDeviceOwnerApp(packageName)) {
            Toast.makeText(this, "Questa azione richiede i permessi di Device Owner", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            // 1. BLOCCO DISINSTALLAZIONE
            dpm.setUninstallBlocked(adminComponent, packageName, true)
            Log.d("AdminApp", "Blocco disinstallazione ATTIVATO.")

            // 2. BLOCCO FACTORY RESET (dalle Impostazioni)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            Log.d("AdminApp", "Blocco Factory Reset ATTIVATO.")

            // 3. (Opzionale) Blocca l'avvio in Modalità Provvisoria
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            Log.d("AdminApp", "Blocco Modalità Provvisoria ATTIVATO.")

            // 4. Imposta qualità password (policy 'limit-password')
            dpm.setPasswordQuality(adminComponent, DevicePolicyManager.PASSWORD_QUALITY_NUMERIC)
            Log.d("AdminApp", "Qualità password impostata a NUMERICA.")

            Toast.makeText(this, "Policy da Device Owner applicate!", Toast.LENGTH_LONG).show()
            updateUI() // Aggiorna la UI

        } catch (e: SecurityException) {
            Log.e("AdminApp", "Errore applicazione policy", e)
            Toast.makeText(this, "Errore: Permessi insufficienti.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateUI() {
        val isAdmin = dpm.isAdminActive(adminComponent)
        val isOwner = dpm.isDeviceOwnerApp(packageName)

        // Se siamo Device Owner, mostra tutti i controlli admin e la lista app
        if (isOwner) {
            adminControlsLayout.visibility = View.VISIBLE
            rvAppList.visibility = View.VISIBLE
            btnEnableAdmin.visibility = View.GONE
            btnDisableAdmin.visibility = View.GONE // Nascosto, un Owner non può essere disattivato

            val policiesActive = dpm.isUninstallBlocked(adminComponent, packageName)
            btnApplyPolicies.isEnabled = !policiesActive
            btnApplyPolicies.text = if (policiesActive) "Policy di Base APPLICATE" else "Applica Policy di Base (Owner)"

            btnLockScreen.isEnabled = true
            btnToggleCamera.isEnabled = true
            btnWipeData.isEnabled = true

            try {
                isCameraDisabled = dpm.getCameraDisabled(adminComponent)
                btnToggleCamera.text = if (isCameraDisabled) "Abilita Fotocamera" else "Disabilita Fotocamera"
            } catch (e: Exception) {
                Log.e("AdminApp", "Impossibile ottenere stato fotocamera")
            }

        } else if (isAdmin) {
            // Se siamo solo Admin (non Owner)
            adminControlsLayout.visibility = View.GONE
            rvAppList.visibility = View.GONE
            btnEnableAdmin.visibility = View.GONE
            btnDisableAdmin.visibility = View.VISIBLE

        } else {
            // Se non siamo né Admin né Owner
            adminControlsLayout.visibility = View.GONE
            rvAppList.visibility = View.GONE
            btnEnableAdmin.visibility = View.VISIBLE
            btnDisableAdmin.visibility = View.GONE
        }
    }
}

