package io.almayce.dev.fancysticker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import io.almayce.dev.fancysticker.model.StorageReader
import io.almayce.dev.fancysticker.model.ZipManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import android.provider.MediaStore.Images
import android.view.View
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.google.android.gms.ads.AdRequest
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, BillingProcessor.IBillingHandler {

    private var stickerList = ArrayList<Bitmap>()
    private lateinit var bp: BillingProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.setDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        var adapter = CustomGridViewAdapter(this, stickerList)
        gvStickers.adapter = adapter
        gvStickers.setOnItemClickListener { adapterView, view, i, l -> send(i) }
        drawer_layout.openDrawer(GravityCompat.START)

        srlLoading.setOnRefreshListener {
            StorageReader.download("sun")
        }

        StorageReader.onStartLoading
                .compose(SchedulersTransformer())
                .subscribe({ it ->
                    srlLoading.isRefreshing = true
                    srlLoading.setOnRefreshListener {
                        StorageReader.download(it)
                    }
                })

        ZipManager.onExtracted
                .compose(SchedulersTransformer())
                .subscribe({ it ->
                    launch(UI) {
                        val path = "${Environment.getExternalStorageDirectory()}/Stickap/$it"
                        Log.d("Files", "Path: " + path)
                        val directory = File(path)
                        val files = directory.listFiles()
                        Log.d("Files", "Size: " + files.size)
                        stickerList.clear()
                        for (i in files.indices) {
                            Log.d("Files", "FileName:" + files[i].getName())
                            var p = "${path}/${files[i].getName()}"
                            var bmp = getBitmapFromPath(p)
                            stickerList.add(bmp)
                        }
                        srlLoading.isRefreshing = false
                        adapter.notifyDataSetChanged()
                    }
                })

        initAd()
        bp = BillingProcessor(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzGlVm3FzSBDcAjde6s0xA4TOjZRz76Ra2WPeXHm4UVtqarKjlrWFzqGwX4TN0/snd/r88iLt12fUhffcarFUVf2ZnyZSoJhme2MYbcDygQ2g9UT5ypxZXX1Cs/gR/neMvic0sSJvB0rJ4uB6FAMpv4YaQ0bDhsBpJXhnkQagZyDcGXec8XXMNnSPffZ39ZQ+VlrOAjmemJtyJyt/sOCc55uFWzJmqEy0fNnWiiS1G4qliKOiOar1kkrMRcHwzXK75wPkg1M2hKd5r3pVuffqkJQeWy6IIQHyOZnuhvCq1FDJ10j3fQsWDxFBBIEmqEkDaSzDv0QncqEaUVkKrct5swIDAQAB", this)
        verifyStoragePermissions()
    }

    fun send(index: Int) {
        val path = Images.Media.insertImage(contentResolver, stickerList.get(index), "temp", null)
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "image/*"
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path))
        startActivity(Intent.createChooser(emailIntent, "Choose messenger"))
    }

    fun getBitmapFromPath(filePath: String): Bitmap = BitmapFactory.decodeFile(filePath)

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START))
            drawer_layout.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        verifyStoragePermissions()

        val id = item.itemId
        when {
            id == R.id.nav_smiles -> StorageReader.download("smiles")
            id == R.id.nav_animal -> StorageReader.download("animal")
            id == R.id.nav_cartoon -> StorageReader.download("cartoon")
            id == R.id.nav_comic -> StorageReader.download("comic")
            id == R.id.nav_funny -> StorageReader.download("funny")
            id == R.id.nav_romantic -> StorageReader.download("romantic")
            id == R.id.nav_skeleton -> StorageReader.download("skeleton")
            id == R.id.nav_sun -> StorageReader.download("sun")

            id == R.id.nav_ads -> {
                if (BillingProcessor.isIabServiceAvailable(applicationContext))
                    bp.purchase(this, "removeads")
            }
        }

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    fun verifyStoragePermissions() {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this@MainActivity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    private fun initAd() {
        var sp = this.getSharedPreferences("billing", android.content.Context.MODE_PRIVATE)
        var b = sp.getBoolean("billing", false)
        adView.visibility = if (!b) View.VISIBLE else View.INVISIBLE
        adView.loadAd(AdRequest.Builder().build())
    }

    override fun onBillingInitialized() {
    }

    override fun onPurchaseHistoryRestored() {
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
    }

    override fun onProductPurchased(productId: String?, details: TransactionDetails?) {
        var sp = this.getSharedPreferences("billing", android.content.Context.MODE_PRIVATE)
        var ed = sp.edit()
        ed.putBoolean("billing", true)
        ed.apply()
    }
}
