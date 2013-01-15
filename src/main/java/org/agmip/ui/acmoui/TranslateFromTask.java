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

    public TranslateFromTask(String file) throws Exception {
        this.file = file;
        boolean metaDataFlg = false;
        File dir = new File(file);
        
        // Pre-check if the meta data file is exist
        if (dir.isDirectory()) {
            String[] files = dir.list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].toUpperCase().equals(metaFileName)) {
                    metaDataFlg = true;
                    break;
                }
            }
        } else if (file.toLowerCase().endsWith(".zip")) {
            FileInputStream f = new FileInputStream(file);
            ZipInputStream z = new ZipInputStream(new BufferedInputStream(f));
            ZipEntry ze;
            
            while ((ze = z.getNextEntry()) != null) {
                if (ze.getName().toUpperCase().equals(metaFileName)) {
                    metaDataFlg = true;
                    break;
                }
            }
            z.close();
            f.close();
        } else {
            LOG.error("Unsupported file: {}", file);
            throw new Exception("Unsupported file type");
        }

        // Error report if necessary
        if (metaDataFlg) {
            translator = new AcmoDssatOutputFileInput();
        } else {
            LOG.error("{} must be included in the zip package", metaFileName);
            throw new Exception("Meta data is missing");
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
