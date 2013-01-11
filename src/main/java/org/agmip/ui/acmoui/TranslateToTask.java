package org.agmip.ui.acmoui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.agmip.core.types.TranslatorOutput;
import org.agmip.translators.acmo.AcmoDssatCsvOutput;
import org.apache.pivot.util.concurrent.Task;
import org.apache.pivot.util.concurrent.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslateToTask extends Task<String> {

    private HashMap data;
    private ArrayList<String> translateList;
    private String destDirectory;
    private static Logger LOG = LoggerFactory.getLogger(TranslateToTask.class);

    public TranslateToTask(ArrayList<String> translateList, HashMap data, String destDirectory) {
        this.data = data;
        this.destDirectory = destDirectory;
        this.translateList = translateList;
//        for (String trType : translateList) {
//            if (!trType.equals("JSON")) {
//                this.translateList.add(trType);
//            }
//        }
    }

    @Override
    public String execute() throws TaskExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(64);
        TranslatorOutput translator;
        try {
            for (String tr : translateList) {
                if (tr.equals("DSSAT")) {
                    translator = new AcmoDssatCsvOutput();
                    String destination = destDirectory + File.separator + tr;
                    LOG.debug("Translating with :" + translator.getClass().getName());
                    Runnable thread = new TranslateRunner(translator, data, destination);
                    executor.execute(thread);
                } else if (tr.equals("APSIM")){
                } else {
                }
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
            }
            executor = null;
        } catch (Exception ex) {
            throw new TaskExecutionException(ex);
        }
        return null;
    }
}
