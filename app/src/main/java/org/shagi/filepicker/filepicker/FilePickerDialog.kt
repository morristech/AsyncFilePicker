package org.shagi.filepicker.filepicker

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.layout_file_picker.*
import org.shagi.filepicker.R
import timber.log.Timber


class FilePickerDialog : BottomSheetDialogFragment() {

    private lateinit var resolver: IntentResolver

    var filePickedListener: OnFilePickedListener? = null
    private var isCameraStarting = false
    private val customActions = ArrayList<CustomActionItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        Timber.d("TEST onCreate $this  - $context")
        resolver = IntentResolver(activity)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.layout_file_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("File picker dialog $context")

        file_picker_camera.setOnClickListener {
            isCameraStarting = true
            resolver.launchCamera(this)
        }
        file_picker_gallery.setOnClickListener {
            isCameraStarting = false
            resolver.launchGallery(this)
        }
        file_picker_files.setOnClickListener { onFilesOpenClick() }

        customActions.forEach {
            (view as LinearLayout).addView(it.generateView(context, view))
        }
    }

    private fun onFilesOpenClick() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, IntentResolver.REQUESTER)
    }

    fun addCustomAction(customActionItem: CustomActionItem) {
        customActions.add(customActionItem)
    }

    override fun onDestroyView() {
        if (dialog != null && retainInstance) {
            dialog.setDismissMessage(null)
        }
        super.onDestroyView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("TEST onActivityResult $this  - $context - $filePickedListener - $customActions")
        if (requestCode == IntentResolver.REQUESTER && resultCode == Activity.RESULT_OK) {
            val isFromCamera = resolver.fromCamera(data)
            val fileType = getFileType(data)
            val uri = getUri(data)

            uri?.let {
                filePickedListener?.onFilePicked(uri, fileType, isFromCamera)
            }
        }

        dismissAllowingStateLoss()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == IntentResolver.REQUESTER) {
            var granted = true

            for (i in grantResults) {
                granted = granted && i == PackageManager.PERMISSION_GRANTED
            }

            if (granted) {
                if (isCameraStarting) {
                    resolver.launchCamera(this)
                } else {
                    resolver.launchGallery(this)
                }
            } else {
                dismissAllowingStateLoss()
            }
        }
    }

    //-----------------------------------------------------------------------------------------------

    private fun getFileType(data: Intent?): FileType {
        if (resolver.fromCamera(data)) return FileType.IMAGE

        val uri = data?.data
        val isImage = uri?.let {
            context.contentResolver.getType(it)
        }?.contains("image/") == true

        if (isImage) return FileType.IMAGE

        return FileType.FILE
    }

    private fun getUri(data: Intent?) = if (resolver.fromCamera(data)) {
        resolver.cameraUri()
    } else {
        data?.data
    }

    //-----------------------------------------------------------------------------------------------

    interface OnFilePickedListener {
        fun onFilePicked(uri: Uri, fileType: FileType, fromCamera: Boolean)
        fun onFilePickFailed()
    }

    companion object {
        const val TAG = "FilePickerDialog"

        @JvmStatic
        fun newInstance(): FilePickerDialog {
            val fragment = FilePickerDialog()

            return fragment
        }
    }
}