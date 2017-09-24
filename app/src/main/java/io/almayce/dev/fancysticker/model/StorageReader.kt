package io.almayce.dev.fancysticker.model

import android.os.Environment
import android.util.Log
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import java.io.File

/**
 * Created by almayce on 20.09.17.
 */
object StorageReader {

    val onStartLoading = PublishSubject.create<String>()
    private lateinit var stoRef: StorageReference
    private var path = "${Environment.getExternalStorageDirectory()}/Stickap"

    fun download(category: String) {
        onStartLoading.onNext(category)
        stoRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://fancysticker-ab336.appspot.com/$category.zip")
        val localFile = File.createTempFile(category, "zip")
        Log.e("UNZIP_LOGS", path)
        stoRef.getFile(localFile)
                .addOnSuccessListener(object : OnSuccessListener<FileDownloadTask.TaskSnapshot> {
                    override fun onSuccess(taskSnapshot: FileDownloadTask.TaskSnapshot) {
                        launch(CommonPool) {ZipManager.unzip(category, localFile.absolutePath,  path)}
                    }
                })
    }
}