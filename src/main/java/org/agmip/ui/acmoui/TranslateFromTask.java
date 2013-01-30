package org.agmip.ui.acmoui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.agmip.core.types.TranslatorInput;
import org.agmip.translators.acmo.AcmoDssatOutputFileInput;
import org.apache.pivot.util.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslateFromTask extends Task<HashMap> {

    private static final Logger LOG = LoggerFactory.getLogger(TranslateFromTask.class);
    private String file;
    private TranslatorInput translator;
    private String metaFileName = "ACMO_META.DAT";
    private String dssatSummaryFileName = "SUMMARY.OUT";

    public TranslateFromTask(String file) throws Exception {
        this.file = file;
        boolean metaDataFlg = false;
        boolean modelDataFlg = false;
        File dir = new File(file);
        
        // Pre-check if the meta data file is exist
        if (dir.isDirectory()) {
            File meta = new File(dir + File.separator + metaFileName);
            File summary = new File(dir + File.separator + dssatSummaryFileName);
            metaDataFlg = meta.exists();
            modelDataFlg = summary.exists();
            
//            if (!modelDataFlg) {
//                Process p = Runtime.getRuntime().exec("cmd /c cd " + dir + " && start C:\\dssat45\\dscsm045 b dssbatch.v45");
//                p.waitFor();
//                summary = new File(dir + File.separator + dssatSummaryFileName);
//                modelDataFlg = summary.exists();
//            }
        } else if (file.toLowerCase().endsWith(".zip")) {
            FileInputStream f = new FileInputStream(file);
            ZipInputStream z = new ZipInputStream(new BufferedInputStream(f));
            ZipEntry ze;
            
            while ((ze = z.getNextEntry()) != null) {
                if (ze.getName().toUpperCase().equals(metaFileName)) {
                    metaDataFlg = true;
                } else if (ze.getName().toUpperCase().equals(dssatSummaryFileName)) {
                    modelDataFlg = true;
                }
            }
            z.close();
            f.close();
        } else {
            LOG.error("Unsupported file: {}", file);
            throw new Exception("Unsupported file type");
        }

        // Error report if necessary
        if (metaDataFlg && modelDataFlg) {
            translator = new AcmoDssatOutputFileInput();
        } else {
            if (!metaDataFlg) {
                LOG.error("{} must be in the selected directory", metaFileName);
                throw new Exception("Meta data is missing");
            }
            if (!modelDataFlg) {
                LOG.error("{} must be in the selected directory", dssatSummaryFileName);
                throw new Exception("Summary.out must be included in the selected directory");
            }
        }
    }

    @Override
    public HashMap<String, Object> execute() {
        HashMap<String, Object> output = new HashMap();
        try {
            output = (HashMap<String, Object>) translator.readFile(file);
            LOG.debug("Translate From Results: {}", output.toString());
            return output;
        } catch (Exception ex) {
            output.put("errors", ex.getMessage());
            return output;
        }
    }
}
