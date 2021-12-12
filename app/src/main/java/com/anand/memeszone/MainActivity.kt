package com.anand.memeszone


import android.Manifest
import android.annotation.TargetApi
import android.app.DownloadManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity(),MemeShare,MemeDownload{
    var isLoading = false
    var msg: String? = ""
    var lastMsg = ""
    private val memeArray = ArrayList<Meme>()
    private lateinit var mAdapter: MemeAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MemesZone)
        setContentView(R.layout.activity_main)
        val layoutManager = LinearLayoutManager(this)
        val rvMeme: RecyclerView = findViewById(R.id.rMeme)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        rvMeme.layoutManager = layoutManager
        loadMeme()
        mAdapter = MemeAdapter(this,this)
        rvMeme.adapter = mAdapter

        rvMeme.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                val visibleItemCount: Int = layoutManager.childCount
                val pastVisibleItem: Int = layoutManager.findFirstCompletelyVisibleItemPosition()
                val total = mAdapter.itemCount
                Log.d("Scroll","$visibleItemCount")
                Log.d("Scroll","$pastVisibleItem")
                if(!isLoading) {
                    if(visibleItemCount + pastVisibleItem >= total) {
                        loadMeme()
                    }
                }

                super.onScrolled(recyclerView, dx, dy)
            }
        })
    }
    private fun loadMeme() {
        isLoading = true
        progressBar.visibility = View.VISIBLE
        val url = "https://meme-api.herokuapp.com/gimme/20"
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null, {
            val memeJsonArray = it.getJSONArray("memes")
            for( i in 0 until memeJsonArray.length()) {
                val memeJsonObject = memeJsonArray.getJSONObject(i)
                val memes = Meme(
                    memeJsonObject.getString("url")
                )
                memeArray.add(memes)
                Log.d(TAG,memeArray.toString())
            }
            mAdapter.updateMemes(memeArray)
            isLoading = false
            progressBar.visibility = View.GONE
        },{
        })
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.custom_menu,menu)
        return true
    }
    override fun MemeShareClicked(MemeUrl: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT,"Hey, Check this cool meme i just saw in Meme Feed $MemeUrl")
        intent.type = "text/plain"
        startActivity(intent)
    }

    override fun MemeDownloadClicked(MemeUrl: String) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            askPermission(MemeUrl)
        } else {
            downloadImg(MemeUrl)
        }
    }

    private fun downloadImg(memeUrl: String) {
        val directory = File(Environment.DIRECTORY_PICTURES)

        if (!directory.exists()) {
            directory.mkdirs()
        }
        val downloadManager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadUri = Uri.parse(memeUrl)

        val request = DownloadManager.Request(downloadUri).apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(memeUrl.substring(memeUrl.lastIndexOf("/") + 1))
                .setDescription("")
                .setDestinationInExternalPublicDir(
                    directory.toString(),
                    memeUrl.substring(memeUrl.lastIndexOf("/") + 1)
                )
        }

        val downloadId = downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(downloadId)
        Thread(Runnable {
            var downloading = true
            while (downloading) {
                val cursor: Cursor = downloadManager.query(query)
                cursor.moveToFirst()
                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS).toInt()) == DownloadManager.STATUS_SUCCESSFUL) {
                    downloading = false
                }
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS).toInt())
                msg = statusMessage(memeUrl, directory, status)
                if (msg != lastMsg) {
                    this.runOnUiThread {
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                    lastMsg = msg ?: ""
                }
                cursor.close()
            }
        }).start()
    }

    private fun statusMessage(memeUrl: String, directory: File, status: Int): String? {
        var msg = ""
        msg = when (status) {
            DownloadManager.STATUS_FAILED -> "Download has been failed, please try again"
            DownloadManager.STATUS_PAUSED -> "Paused"
            DownloadManager.STATUS_PENDING -> "Pending"
            DownloadManager.STATUS_RUNNING -> "Downloading..."
            DownloadManager.STATUS_SUCCESSFUL -> "Image downloaded successfully in $directory" + File.separator + memeUrl.substring(
                memeUrl.lastIndexOf("/") + 1
            )
            else -> "There's nothing to download"
        }
        return msg
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun askPermission(MemeUrl: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Permission required")
                    .setMessage("Permission required to save photos from the Web.")
                    .setPositiveButton("Accept") { dialog, id ->
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
                        finish()
                        downloadImg(MemeUrl)
                    }
                    .setNegativeButton("Deny") { dialog, id -> dialog.cancel() }
                    .show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
            }
        } else {
            downloadImg(MemeUrl)
        }
    }
    companion object {
        private const val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1
    }
}