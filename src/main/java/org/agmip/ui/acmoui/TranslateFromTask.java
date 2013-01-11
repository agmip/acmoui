package org.agmip.ui.acmoui;

import java.io.BufferedInputStream;
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
        if (file.toLowerCase().endsWith(".zip")) {
            FileInputStream f = new FileInputStream(file);
            ZipInputStream z = new ZipInputStream(new BufferedInputStream(f));
            ZipEntry ze;
            boolean metaDataFlg = false;
            while ((ze = z.getNextEntry()) != null) {
                if (ze.getName().toUpperCase().equals(metaFileName)) {
                    metaDataFlg = true;
                }
            }
            z.close();
            f.close();

            if (metaDataFlg) {
                translator = new AcmoDssatOutputFileInput();
            } else {
                LOG.error("{} must be included in the zip package", metaFileName);
                throw new Exception("Meta data is missing");
            }
        } else {
            LOG.error("Unsupported file: {}", file);
            throw new Exception("Unsupported file type");
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
