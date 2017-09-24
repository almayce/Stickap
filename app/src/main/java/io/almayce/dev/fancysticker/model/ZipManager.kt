package io.almayce.dev.fancysticker.model

import io.reactivex.subjects.PublishSubject
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.progress.ProgressMonitor


/**
 * Created by almayce on 20.09.17.
 */
object ZipManager {

    val onExtracted = PublishSubject.create<String>()

    fun unzip(category: String, source: String, destination: String) {
        try {
            val zipFile = ZipFile(source)
            zipFile.isRunInThread = true
            zipFile.extractAll(destination)
            val progressMonitor = zipFile.getProgressMonitor()
            while (progressMonitor.state == ProgressMonitor.STATE_BUSY) {
                // ProgressMonitor has a lot of useful information like, the current
                // operation being performed by Zip4j, current file being processed,
                // percentage done, etc. Once an operation is completed, ProgressMonitor
                // also contains the result of the operation. If any exception is thrown
                // during an operation, this is also stored in this object and can be retrieved
                // as shown below

                // To get the percentage done
                System.out.println("Percent Done: " + progressMonitor.percentDone)

                // To get the current file being processed
                System.out.println("File: " + progressMonitor.fileName)

                // To get current operation
                // Possible values are:
                // ProgressMonitor.OPERATION_NONE - no operation being performed
                // ProgressMonitor.OPERATION_ADD - files are being added to the zip file
                // ProgressMonitor.OPERATION_EXTRACT - files are being extracted from the zip file
                // ProgressMonitor.OPERATION_REMOVE - files are being removed from zip file
                // ProgressMonitor.OPERATION_CALC_CRC - CRC of the file is being calculated
                // ProgressMonitor.OPERATION_MERGE - Split zip files are being merged
//                when (progressMonitor.currentOperation) {
//                    ProgressMonitor.OPERATION_NONE -> println("no operation being performed")
//                    ProgressMonitor.OPERATION_ADD -> println("Add operation")
//                    ProgressMonitor.OPERATION_EXTRACT -> println("Extract operation")
//                    ProgressMonitor.OPERATION_REMOVE -> println("Remove operation")
//                    ProgressMonitor.OPERATION_CALC_CRC -> println("Calcualting CRC")
//                    ProgressMonitor.OPERATION_MERGE -> println("Merge operation")
//                    else -> println("invalid operation")
//                }
                // Once Zip4j is done with its task, it changes the ProgressMonitor
                // state from BUSY to READY, so the above loop breaks.
                // To get the result of the operation:
                // Possible values:
                // ProgressMonitor.RESULT_SUCCESS - Operation was successful
                // ProgressMonitor.RESULT_WORKING - Zip4j is still working and is not
                //									yet done with the current operation
                // ProgressMonitor.RESULT_ERROR - An error occurred during processing

                System.out.println("Result: " + progressMonitor.getResult());

                if (progressMonitor.getResult() == ProgressMonitor.RESULT_ERROR) {
                    // Any exception can be retrieved as below:
                    if (progressMonitor.getException() != null) {
                        progressMonitor.getException().printStackTrace();
                    } else {
                        System.err.println("An error occurred without any exception");
                    }
                }
            }
            onExtracted.onNext(category)

        } catch (e: ZipException) {
            e.printStackTrace()
        }
    }
}