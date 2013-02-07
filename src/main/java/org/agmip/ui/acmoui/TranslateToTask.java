package org.agmip.ui.acmoui;

import java.io.File;
import java.util.HashMap;
import org.agmip.core.types.TranslatorOutput;
import org.agmip.translators.acmo.AcmoDssatCsvOutput;
import org.apache.pivot.util.concurrent.Task;
import org.apache.pivot.util.concurrent.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslateToTask extends Task<String> {

    private HashMap data;
//    private ArrayList<String> translateList;
    private String translateTpye;
    private String destDirectory;
    private static Logger LOG = LoggerFactory.getLogger(TranslateToTask.class);

    public TranslateToTask(String translateTpye, HashMap data, String destDirectory) {
        this.data = data;
        this.destDirectory = destDirectory;
        this.translateTpye = translateTpye;
//        this.translateList = translateList;
    }

    @Override
    public String execute() throws TaskExecutionException {
//        ExecutorService executor = Executors.newFixedThreadPool(64);
        TranslatorOutput translator;
        File output;

        try {

            // Run select mode
//            for (int i = 0; i < translateList.size(); i++) {
//                String tr = translateList.get(i);
            if (translateTpye.equals("DSSAT")) {
                translator = new AcmoDssatCsvOutput();
                LOG.debug("Translating with :" + translator.getClass().getName());
//                Runnable thread = new TranslateRunner(translator, data, destination);
//                executor.execute(thread);
            } else if (translateTpye.equals("APSIM")) {
                translator = null;
            } else {
                translator = null;
            }
            translator.writeFile(destDirectory, data);
//            }

            // Release executor
//            executor.shutdown();
//            while (!executor.isTerminated()) {
//            }
//            executor = null;
            if (translator == null) {
                return "";
            } else if (translateTpye.equals("DSSAT")) {
                output = ((AcmoDssatCsvOutput) translator).getOutputFile();
            } else if (translateTpye.equals("APSIM")) {
                output = null; //TODO
            } else {
                output = null;
            }

        } catch (Exception ex) {
            throw new TaskExecutionException(ex);
        }
        if (output != null) {
            return output.getAbsolutePath();
        } else {
            return "";
        }

    }
}
